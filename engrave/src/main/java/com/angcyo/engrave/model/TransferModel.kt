package com.angcyo.engrave.model

import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.ViewModel
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.FileModeCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.QueryCmd
import com.angcyo.bluetooth.fsc.laserpacker.parse.FileTransferParser
import com.angcyo.bluetooth.fsc.laserpacker.parse.QueryEngraveFileParser
import com.angcyo.bluetooth.fsc.parse
import com.angcyo.canvas.CanvasDelegate
import com.angcyo.core.component.file.writeErrorLog
import com.angcyo.engrave.EngraveFlowDataHelper
import com.angcyo.engrave.data.*
import com.angcyo.engrave.transition.DataException
import com.angcyo.engrave.transition.EmptyException
import com.angcyo.engrave.transition.EngraveTransitionManager
import com.angcyo.engrave.transition.FailException
import com.angcyo.http.rx.doBack
import com.angcyo.http.rx.doMain
import com.angcyo.library.L
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.ex.clamp
import com.angcyo.library.ex.size
import com.angcyo.objectbox.deleteAllEntity
import com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity
import com.angcyo.viewmodel.vmDataNull

/**
 * 数据传输模式
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/29
 */
class TransferModel : ViewModel() {

    companion object {

        /**根据雕刻数据, 返回数据指令*/
        fun getTransferDataCmd(transferDataEntity: TransferDataEntity): DataCmd? {
            val bytes = transferDataEntity.bytes()
            if (bytes == null || bytes.isEmpty()) {
                return null
            }

            //数据类型封装
            val dataCmd: DataCmd = when (transferDataEntity.engraveDataType) {
                //0x10 图片数据
                DataCmd.ENGRAVE_TYPE_BITMAP -> DataCmd.bitmapData(
                    transferDataEntity.index,
                    transferDataEntity.x,
                    transferDataEntity.y,
                    transferDataEntity.width,
                    transferDataEntity.height,
                    transferDataEntity.dpi,
                    transferDataEntity.name,
                    bytes,
                )
                //0x20 GCode数据
                DataCmd.ENGRAVE_TYPE_GCODE -> DataCmd.gcodeData(
                    transferDataEntity.index,
                    transferDataEntity.x,
                    transferDataEntity.y,
                    transferDataEntity.width,
                    transferDataEntity.height,
                    transferDataEntity.name,
                    transferDataEntity.lines,
                    bytes,
                    transferDataEntity.dpi
                )
                //0x40 黑白画, 线段数据
                DataCmd.ENGRAVE_TYPE_BITMAP_PATH -> DataCmd.bitmapPathData(
                    transferDataEntity.index,
                    transferDataEntity.x,
                    transferDataEntity.y,
                    transferDataEntity.width,
                    transferDataEntity.height,
                    transferDataEntity.name,
                    transferDataEntity.lines,
                    bytes,
                    transferDataEntity.dpi,
                )
                //0x60 抖动数据, 二进制位
                //DataCmd.ENGRAVE_TYPE_BITMAP_DITHERING ->
                else -> DataCmd.bitmapDitheringData(
                    transferDataEntity.index,
                    transferDataEntity.x,
                    transferDataEntity.y,
                    transferDataEntity.width,
                    transferDataEntity.height,
                    transferDataEntity.name,
                    bytes,
                    transferDataEntity.dpi,
                )
            }
            return dataCmd
        }
    }

    /**雕刻数据转换管理*/
    val engraveTransitionManager = EngraveTransitionManager()

    /**传输状态数据*/
    val transferStateData = vmDataNull<TransferTaskStateData?>()

    //

    /**开始创建机器需要的传输数据*/
    @CallPoint
    @WorkerThread
    fun startCreateTransferData(
        taskId: String?,
        canvasDelegate: CanvasDelegate
    ) {
        _isCancelTransfer = false
        doBack {
            transferStateData.postValue(TransferTaskStateData(taskId, 0, null))
            val transferConfigEntity = EngraveFlowDataHelper.generateTransferConfig(taskId)
            val dataEntityList = engraveTransitionManager.transitionTransferData(
                canvasDelegate,
                transferConfigEntity
            )
            if (dataEntityList.isEmpty()) {
                transferStateData.postValue(TransferTaskStateData(taskId, 0, EmptyException()))
            } else {
                //开始传输数据
                _startTransferDataTask(taskId, dataEntityList)
            }
        }
    }

    /**重新传输
     * [all] 是否全部重新传输, 否则只传输未成功的数据
     * */
    @AnyThread
    @CallPoint
    fun retryTransfer(all: Boolean) {
        val taskId: String? = _transferTask?.taskId
        val transferDataEntityList = _transferTask?.entityList
        _isCancelTransfer = false
        if (!transferDataEntityList.isNullOrEmpty()) {
            val transferList = mutableListOf<TransferDataEntity>()
            if (all) {
                transferDataEntityList.deleteAllEntity()
                transferList.addAll(transferDataEntityList)
            } else {
                transferDataEntityList.filterTo(transferList) { !it.isTransfer }
            }
            //开始传输数据
            _startTransferDataTask(taskId, transferList)
        } else {
            transferStateData.postValue(
                TransferTaskStateData(taskId, 0, EmptyException())
            )
        }
    }

    var _isCancelTransfer = false

    /**停止传输*/
    @CallPoint
    fun stopTransfer() {
        transferStateData.postValue(null)
        _isCancelTransfer = true
        _transferTask = null

        //need?
        //CommandQueueHelper.clearCommand()
    }

    /**完成传输
     * [createEngraveConfigInfo]*/
    @CallPoint
    fun finishTransfer() {
        val taskId: String? = _transferTask?.taskId
        _isCancelTransfer = false
        _transferTask = null
        L.i("数据传输任务完成:${taskId}")
        doMain {
            transferStateData.value = TransferTaskStateData(taskId, 100, null, true)
            transferStateData.postValue(null)//清空
        }
    }

    //

    var _transferTask: TransferTask? = null

    /**开始传输数据*/
    fun _startTransferDataTask(taskId: String?, list: List<TransferDataEntity>) {
        //最大传输文件数
        val maxCount = list.size()
        _transferTask = TransferTask(taskId, maxCount, 0, list)

        _transferDataNext()
    }

    @WorkerThread
    fun _transferDataNext() {
        doBack(true) {
            _transferTask?.let { task ->
                if (task.index >= task.count) {
                    //传输完成
                    finishTransfer()
                } else {
                    val transferDataEntity = task.entityList.getOrNull(task.index)
                    if (transferDataEntity == null) {
                        finishTransfer()
                    } else {
                        L.i("开始传输数据:[${task.index}/${task.count}]")
                        val fileModeCmd = FileModeCmd(transferDataEntity.bytes()?.size ?: 0)
                        fileModeCmd.enqueue { bean, error ->
                            error?.let {
                                it.toString().writeErrorLog()
                                transferStateData.postValue(
                                    TransferTaskStateData(
                                        task.taskId,
                                        transferStateData.value?.progress ?: 0,
                                        FailException(error)
                                    )
                                )
                            }
                            bean?.parse<FileTransferParser>()?.let {
                                if (it.isIntoFileMode()) {
                                    //成功进入大数据模式
                                    val dataCmd = getTransferDataCmd(transferDataEntity)
                                    if (dataCmd == null) {
                                        task.index++
                                        _transferDataNext()
                                    } else {
                                        dataCmd.enqueue(progress = {
                                            //进度
                                            val progress = calcTransferProgress(
                                                it.sendPacketPercentage,
                                                task.index,
                                                task.count
                                            )
                                            doMain {
                                                //及时回调
                                                transferStateData.value =
                                                    TransferTaskStateData(task.taskId, progress)
                                            }
                                        }) { bean, error ->
                                            val result = bean?.parse<FileTransferParser>()
                                            L.w("传输结束:$result $error")
                                            result?.let {
                                                if (result.isFileTransferSuccess()) {
                                                    //文件传输完成
                                                    task.index++
                                                    transferDataEntity.isTransfer = true
                                                    _transferDataNext()
                                                } else {
                                                    "数据接收未完成".writeErrorLog()
                                                    transferStateData.postValue(
                                                        TransferTaskStateData(
                                                            task.taskId,
                                                            transferStateData.value?.progress ?: 0,
                                                            DataException()
                                                        )
                                                    )
                                                }
                                            }
                                            if (result == null) {
                                                "发送数据失败".writeErrorLog()
                                                transferStateData.postValue(
                                                    TransferTaskStateData(
                                                        task.taskId,
                                                        transferStateData.value?.progress ?: 0,
                                                        FailException()
                                                    )
                                                )
                                            }
                                        }
                                        //end data cmd
                                    }
                                    //end parse
                                } else {
                                    "未成功进入数据传输模式".writeErrorLog()
                                    transferStateData.postValue(
                                        TransferTaskStateData(
                                            task.taskId,
                                            transferStateData.value?.progress ?: 0,
                                            FailException()
                                        )
                                    )
                                }
                            }
                            //end file mode cmd
                        }
                    }
                }
            }
        }
    }

    /**检查文件索引是否存在*/
    fun checkIndex(index: Int, action: (Boolean) -> Unit) {
        //检查数据索引是否存在
        QueryCmd.fileList.enqueue { bean, error ->
            val have =
                bean?.parse<QueryEngraveFileParser>()?.nameList?.contains(index) == true
            action(have)
        }
    }

    /**计算进度*/
    fun calcTransferProgress(indexProgress: Int, index: Int, count: Int): Int {
        val part = 100f / count
        val result = (index * part + part * indexProgress / 100).toInt()
        return clamp(result, 0, 100)
    }

    //待发送的数据任务
    data class TransferTask(
        //任务id
        val taskId: String?,
        //总共需要发送的数据量
        val count: Int,
        //当前发送的索引
        var index: Int,
        //任务所有要传输的数据, 每个图层下的所有数据
        val entityList: List<TransferDataEntity>
    )

}