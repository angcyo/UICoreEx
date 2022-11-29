package com.angcyo.engrave.model

import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.ViewModel
import com.angcyo.bluetooth.fsc.CommandQueueHelper.FLAG_CLEAR_BEFORE
import com.angcyo.bluetooth.fsc.CommandQueueHelper.FLAG_NORMAL
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.FileModeCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.QueryCmd
import com.angcyo.bluetooth.fsc.laserpacker.parse.FileTransferParser
import com.angcyo.bluetooth.fsc.laserpacker.parse.QueryEngraveFileParser
import com.angcyo.bluetooth.fsc.laserpacker.writeEngraveLog
import com.angcyo.bluetooth.fsc.parse
import com.angcyo.canvas.CanvasDelegate
import com.angcyo.core.component.file.writeErrorLog
import com.angcyo.engrave.EngraveFlowDataHelper
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.engrave.data.TransferState
import com.angcyo.engrave.toEngraveDataTypeStr
import com.angcyo.engrave.transition.EmptyException
import com.angcyo.engrave.transition.EngraveTransitionManager
import com.angcyo.engrave.transition.OutOfSizeException
import com.angcyo.engrave.transition.TransferException
import com.angcyo.http.rx.doBack
import com.angcyo.http.rx.doMain
import com.angcyo.library.L
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.ex.clamp
import com.angcyo.library.ex.toSizeString
import com.angcyo.objectbox.laser.pecker.LPBox
import com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity
import com.angcyo.objectbox.laser.pecker.lpSaveEntity
import com.angcyo.objectbox.saveAllEntity
import com.angcyo.viewmodel.vmDataOnce
import kotlin.math.max

/**
 * 数据传输模式
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/29
 */
class TransferModel : ViewModel() {

    companion object {

        /**计算进度*/
        fun calcTransferProgress(taskId: String?, indexProgress: Int): Int {
            val list = EngraveFlowDataHelper.getTransferDataList(taskId)
            val count = list.count()//总共要传输的数量
            val transferCount = list.sumOf { if (it.isTransfer) 1L else 0L }//已经传输的数量
            val part = 100f / count //每个文件传输只能占用的进度
            val result = (transferCount * part + part * indexProgress / 100).toInt()
            return clamp(result, 0, 100)
        }

        /**根据雕刻数据, 返回数据指令*/
        fun getTransferDataCmd(transferDataEntity: TransferDataEntity): DataCmd? {
            val bytes = transferDataEntity.bytes()
            if (bytes == null || bytes.isEmpty()) {
                return null
            }

            val index = transferDataEntity.index
            val x = max(0, transferDataEntity.x)//必须>=0
            val y = max(0, transferDataEntity.y)//必须>=0
            val width = transferDataEntity.width
            val height = transferDataEntity.height
            val dpi = transferDataEntity.dpi
            val name = transferDataEntity.name

            val lines = transferDataEntity.lines

            //数据类型封装
            val dataCmd: DataCmd = when (transferDataEntity.engraveDataType) {
                //0x10 图片数据
                DataCmd.ENGRAVE_TYPE_BITMAP -> DataCmd.bitmapData(
                    index,
                    x,
                    y,
                    width,
                    height,
                    dpi,
                    name,
                    bytes
                )
                //0x20 GCode数据
                DataCmd.ENGRAVE_TYPE_GCODE -> DataCmd.gcodeData(
                    index,
                    x,
                    y,
                    width,
                    height,
                    name,
                    lines,
                    bytes,
                    dpi
                )
                //0x40 黑白画, 线段数据
                DataCmd.ENGRAVE_TYPE_BITMAP_PATH -> DataCmd.bitmapPathData(
                    index,
                    x,
                    y,
                    width,
                    height,
                    name,
                    lines,
                    bytes,
                    dpi,
                )
                //0x60 抖动数据, 二进制位
                //DataCmd.ENGRAVE_TYPE_BITMAP_DITHERING ->
                else -> DataCmd.bitmapDitheringData(
                    index,
                    x,
                    y,
                    width,
                    height,
                    name,
                    bytes,
                    dpi,
                )
            }
            return dataCmd
        }
    }

    /**雕刻数据转换管理*/
    val engraveTransitionManager = EngraveTransitionManager()

    /**当前任务的状态通知*/
    val transferStateOnceData = vmDataOnce<TransferState>()

    /**缓存*/
    var _transferState: TransferState? = null

    //

    /**开始创建机器需要的传输数据*/
    @CallPoint
    @AnyThread
    fun startCreateTransferData(taskId: String?, canvasDelegate: CanvasDelegate) {
        stopTransfer()
        doBack {
            //传输状态, 开始创建数据
            val transferState = TransferState(taskId, progress = -1)
            EngraveFlowDataHelper.startCreateTransferData(taskId)
            transferStateOnceData.postValue(transferState)
            val transferConfigEntity = EngraveFlowDataHelper.generateTransferConfig(taskId)
            val dataEntityList = engraveTransitionManager.transitionTransferData(
                canvasDelegate,
                transferConfigEntity
            )//数据已入库, 可以直接在数据库中查询
            EngraveFlowDataHelper.finishCreateTransferData(taskId)
            startTransferData(transferState.taskId)
        }
    }

    /**开始传输机器需要的数据*/
    @CallPoint
    @AnyThread
    fun startTransferData(taskId: String?) {
        stopTransfer()
        val transferState = TransferState(taskId, TransferState.TRANSFER_STATE_NORMAL, 0)
        _transferState = transferState
        doBack {
            val list = EngraveFlowDataHelper.getTransferDataList(taskId)
            EngraveFlowDataHelper.startTransferData(taskId, list)//检测数据

            if (list.isEmpty()) {
                //需要传输的数据为空
                transferState.state = TransferState.TRANSFER_STATE_FINISH
                transferState.error = EmptyException()
                transferStateOnceData.postValue(transferState)
            } else {
                //开始传输
                transferStateOnceData.postValue(transferState)
                _transferNext(transferState)
            }
        }
    }

    /**重新传输
     * [all] 是否全部重新传输, 否则只传输未成功的数据
     * */
    @AnyThread
    @CallPoint
    fun retryTransfer(all: Boolean) {
        val transferState = _transferState ?: return
        val taskId = transferState.taskId
        val transferDataEntityList = EngraveFlowDataHelper.getTransferDataList(taskId)
        if (transferDataEntityList.isNotEmpty()) {
            if (all) {
                //需要重新传输所有数据
                transferDataEntityList.forEach { it.isTransfer = false }
                transferDataEntityList.saveAllEntity(LPBox.PACKAGE_NAME)
            }
            //开始传输数据
            startTransferData(taskId)
        } else {
            transferState.state = TransferState.TRANSFER_STATE_FINISH
            transferState.error = EmptyException()
            transferStateOnceData.postValue(transferState)
        }
    }

    /**停止当前的传输*/
    @CallPoint
    fun stopTransfer() {
        _transferState?.state = TransferState.TRANSFER_STATE_CANCEL
        transferStateOnceData.postValue(_transferState)
        //need?
        //CommandQueueHelper.clearCommand()
    }

    //

    @WorkerThread
    fun _transferNext(transferState: TransferState) {
        val taskId: String? = transferState.taskId
        val transferDataEntity = EngraveFlowDataHelper.getNeedTransferData(taskId)
        if (transferDataEntity == null) {
            //全部传输完成
            transferState.state = TransferState.TRANSFER_STATE_FINISH
            transferState.progress = 100
            transferStateOnceData.postValue(transferState)
            EngraveFlowDataHelper.finishTransferData(taskId)
            L.i("数据传输任务完成:${taskId}")
        } else {
            //需要传输数据
            if (transferState.state == TransferState.TRANSFER_STATE_NORMAL) {
                //状态正常
                transferData(transferState, transferDataEntity)
            }
        }
    }

    /**传输数据
     * [action] 成功或者失败的回调*/
    fun transferData(
        transferState: TransferState,
        transferDataEntity: TransferDataEntity,
        action: (Throwable?) -> Unit = {}
    ) {
        val taskId = transferDataEntity.taskId
        L.i("开始传输数据:[$taskId][${transferDataEntity.index}]")
        val size = transferDataEntity.bytes()?.size ?: 0
        if (size <= 0) {
            "传输数据为空:${transferDataEntity.index}".writeErrorLog()
            transferState.state = TransferState.TRANSFER_STATE_FINISH
            transferState.error = EmptyException()
            transferStateOnceData.postValue(transferState)

            action(transferState.error)
            return
        } else if (size > HawkEngraveKeys.maxTransferDataSize) {
            "传输数据过大:${transferDataEntity.index}${size.toSizeString()}".writeErrorLog()
            transferState.state = TransferState.TRANSFER_STATE_FINISH
            transferState.error = OutOfSizeException()
            transferStateOnceData.postValue(transferState)

            action(transferState.error)
            return
        }
        val fileModeCmd = FileModeCmd(size)
        fileModeCmd.enqueue(FLAG_NORMAL or FLAG_CLEAR_BEFORE) { bean, error ->
            error?.let {
                it.toString().writeErrorLog()
                transferState.state = TransferState.TRANSFER_STATE_FINISH
                transferState.error = TransferException(error)
                transferStateOnceData.postValue(transferState)

                action(transferState.error)
            }
            if (transferState.state == TransferState.TRANSFER_STATE_NORMAL) {
                //传输指令
                bean?.parse<FileTransferParser>()?.let {
                    if (it.isIntoFileMode()) {
                        //成功进入大数据模式
                        val dataCmd = getTransferDataCmd(transferDataEntity)
                        if (dataCmd == null) {
                            //无效的数据, !!!
                            transferDataEntity.isTransfer = true
                            transferDataEntity.lpSaveEntity()
                            _transferNext(transferState)

                            action(transferState.error)
                        } else {
                            buildString {
                                append("开始传输:[${taskId}]")
                                append(" ${transferDataEntity.engraveDataType.toEngraveDataTypeStr()}")
                                append(" $transferDataEntity")
                            }.writeEngraveLog()

                            dataCmd.enqueue(progress = {
                                //进度
                                val progress = calcTransferProgress(taskId, it.sendPacketPercentage)
                                EngraveFlowDataHelper.updateTransferDataProgress(
                                    taskId,
                                    progress,
                                    it.sendSpeed
                                )
                                doMain {
                                    //及时回调
                                    transferState.progress = progress
                                    transferStateOnceData.value = transferState
                                }
                            }) { bean, error ->
                                val result = bean?.parse<FileTransferParser>()
                                L.w("传输结束:$result $error Success:${result?.isFileTransferSuccess()}")
                                result?.let {
                                    if (result.isFileTransferSuccess()) {
                                        //文件传输完成
                                        transferDataEntity.isTransfer = true
                                        transferDataEntity.lpSaveEntity()
                                        _transferNext(transferState)

                                        action(null)
                                    } else if (transferState.state == TransferState.TRANSFER_STATE_NORMAL) {
                                        "数据接收未完成".writeErrorLog()
                                        transferState.state =
                                            TransferState.TRANSFER_STATE_FINISH
                                        transferState.error = TransferException()
                                        transferStateOnceData.postValue(transferState)

                                        action(transferState.error)
                                    }
                                }
                                if (result == null && transferState.state == TransferState.TRANSFER_STATE_NORMAL) {
                                    "发送数据失败".writeErrorLog()
                                    transferState.state =
                                        TransferState.TRANSFER_STATE_FINISH
                                    transferState.error = TransferException()
                                    transferStateOnceData.postValue(transferState)

                                    action(transferState.error)
                                }
                            }
                            //end data cmd
                        }
                        //end parse
                    } else if (transferState.state == TransferState.TRANSFER_STATE_NORMAL) {
                        "未成功进入数据传输模式".writeErrorLog()
                        transferState.state = TransferState.TRANSFER_STATE_FINISH
                        transferState.error = TransferException()
                        transferStateOnceData.postValue(transferState)

                        action(transferState.error)
                    }
                }
                //end传输指令
            }
            //end file mode cmd
        }
    }

    /**检查文件索引是否存在*/
    fun checkIndex(index: Int, action: (Boolean) -> Unit) {
        //检查数据索引是否存在
        QueryCmd.fileList.enqueue { bean, error ->
            val have =
                bean?.parse<QueryEngraveFileParser>()?.indexList?.contains(index) == true
            action(have)
        }
    }
}