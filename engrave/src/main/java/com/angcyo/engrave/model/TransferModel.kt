package com.angcyo.engrave.model

import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.ViewModel
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.FileModeCmd
import com.angcyo.bluetooth.fsc.laserpacker.parse.FileTransferParser
import com.angcyo.bluetooth.fsc.parse
import com.angcyo.canvas.CanvasDelegate
import com.angcyo.core.component.file.writeErrorLog
import com.angcyo.engrave.data.TransferDataConfigInfo
import com.angcyo.engrave.data.TransferDataInfo
import com.angcyo.engrave.data.TransferTaskData
import com.angcyo.engrave.data.TransferTaskStateData
import com.angcyo.engrave.transition.DataException
import com.angcyo.engrave.transition.EmptyException
import com.angcyo.engrave.transition.EngraveTransitionManager
import com.angcyo.engrave.transition.FailException
import com.angcyo.http.rx.doBack
import com.angcyo.library.L
import com.angcyo.library.ex.clamp
import com.angcyo.library.ex.size
import com.angcyo.viewmodel.vmDataNull

/**
 * 数据传输模式
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/29
 */
class TransferModel : ViewModel() {

    companion object {

        /**根据雕刻数据, 返回数据指令*/
        fun getTransferDataCmd(transferDataInfo: TransferDataInfo): DataCmd? {
            val bytes = transferDataInfo.data
            if (bytes == null || bytes.isEmpty()) {
                return null
            }

            //数据类型封装
            val dataCmd: DataCmd = when (transferDataInfo.engraveDataType) {
                //0x10 图片数据
                DataCmd.ENGRAVE_TYPE_BITMAP -> DataCmd.bitmapData(
                    transferDataInfo.index,
                    transferDataInfo.x,
                    transferDataInfo.y,
                    transferDataInfo.width,
                    transferDataInfo.height,
                    transferDataInfo.px,
                    transferDataInfo.name,
                    bytes,
                )
                //0x20 GCode数据
                DataCmd.ENGRAVE_TYPE_GCODE -> DataCmd.gcodeData(
                    transferDataInfo.index,
                    transferDataInfo.x,
                    transferDataInfo.y,
                    transferDataInfo.width,
                    transferDataInfo.height,
                    transferDataInfo.name,
                    transferDataInfo.lines,
                    bytes
                )
                //0x40 黑白画, 线段数据
                DataCmd.ENGRAVE_TYPE_BITMAP_PATH -> DataCmd.bitmapPathData(
                    transferDataInfo.index,
                    transferDataInfo.x,
                    transferDataInfo.y,
                    transferDataInfo.width,
                    transferDataInfo.height,
                    transferDataInfo.px,
                    transferDataInfo.name,
                    transferDataInfo.lines,
                    bytes,
                )
                //0x60 抖动数据, 二进制位
                //DataCmd.ENGRAVE_TYPE_BITMAP_DITHERING ->
                else -> DataCmd.bitmapDitheringData(
                    transferDataInfo.index,
                    transferDataInfo.x,
                    transferDataInfo.y,
                    transferDataInfo.width,
                    transferDataInfo.height,
                    transferDataInfo.px,
                    transferDataInfo.name,
                    bytes
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

    var taskDataCacheList: List<TransferTaskData>? = null

    /**开始创建机器需要的数据*/
    @WorkerThread
    fun startCreateData(
        transferDataConfigInfo: TransferDataConfigInfo,
        canvasDelegate: CanvasDelegate
    ) {
        _isCancelTransfer = false
        doBack {
            transferStateData.postValue(TransferTaskStateData(0, null))
            val taskDataList = engraveTransitionManager.transitionTransferData(
                canvasDelegate,
                transferDataConfigInfo
            )
            taskDataCacheList = taskDataList
            if (taskDataList.isEmpty()) {
                transferStateData.postValue(TransferTaskStateData(0, EmptyException()))
            } else {
                //开始传输数据
                _startTransferDataTask(taskDataList)
            }
        }
    }

    /**重新传输*/
    @AnyThread
    fun retryTransfer() {
        _isCancelTransfer = false
        if (!taskDataCacheList.isNullOrEmpty()) {
            _startTransferDataTask(taskDataCacheList!!)
        } else {
            transferStateData.postValue(TransferTaskStateData(0, EmptyException()))
        }
    }

    var _isCancelTransfer = false

    /**停止传输*/
    fun stopTransfer() {
        transferStateData.postValue(null)
        _isCancelTransfer = true
        _transferTask = null

        //need?
        //CommandQueueHelper.clearCommand()
    }

    /**完成传输*/
    fun finishTransfer() {
        _isCancelTransfer = false
        _transferTask = null
        transferStateData.postValue(TransferTaskStateData(100, null, true))
    }

    //

    var _transferTask: TransferTask? = null

    /**开始传输数据*/
    fun _startTransferDataTask(list: List<TransferTaskData>) {
        //最大传输文件数
        val maxCount = list.sumOf { it.transferDataList.size() }
        _transferTask = TransferTask(maxCount, 0, list)

        _transferDataNext()
    }

    fun _transferDataNext() {
        _transferTask?.let { task ->
            if (task.index >= task.count) {
                //传输完成
                finishTransfer()
            } else {
                val allData = mutableListOf<TransferDataInfo>()
                task.list.forEach { allData.addAll(it.transferDataList) }
                val transferDataInfo = allData.getOrNull(task.index)
                if (transferDataInfo == null) {
                    finishTransfer()
                } else {
                    L.i("开始传输数据:[${task.index}/${task.count}]")
                    val fileModeCmd = FileModeCmd(transferDataInfo.data?.size ?: 0)
                    fileModeCmd.enqueue { bean, error ->
                        bean?.parse<FileTransferParser>()?.let {
                            if (it.isIntoFileMode()) {
                                //成功进入大数据模式
                                val dataCmd = getTransferDataCmd(transferDataInfo)
                                if (dataCmd == null) {
                                    task.index++
                                    _transferDataNext()
                                } else {
                                    dataCmd.enqueue(null, {
                                        //进度
                                        val progress = calcTransferProgress(
                                            it.sendPacketPercentage,
                                            task.index,
                                            task.count
                                        )
                                        transferStateData.postValue(TransferTaskStateData(progress))
                                    }) { bean, error ->
                                        val result = bean?.parse<FileTransferParser>()
                                        L.w("传输结束:$result $error")
                                        result?.let {
                                            if (result.isFileTransferSuccess()) {
                                                //文件传输完成
                                                task.index++
                                                _transferDataNext()
                                            } else {
                                                "数据接收未完成".writeErrorLog()
                                                transferStateData.postValue(
                                                    TransferTaskStateData(
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

    /**计算进度*/
    fun calcTransferProgress(indexProgress: Int, index: Int, count: Int): Int {
        val part = 100f / count
        val result = (index * part + part * indexProgress / 100).toInt()
        return clamp(result, 0, 100)
    }

    data class TransferTask(
        val count: Int,
        var index: Int,
        val list: List<TransferTaskData>
    )

}