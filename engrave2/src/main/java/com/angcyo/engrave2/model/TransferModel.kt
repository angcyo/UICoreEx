package com.angcyo.engrave2.model

import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.ViewModel
import com.angcyo.bluetooth.fsc.CommandQueueHelper
import com.angcyo.bluetooth.fsc.CommandQueueHelper.FLAG_CLEAR_BEFORE
import com.angcyo.bluetooth.fsc.CommandQueueHelper.FLAG_NORMAL
import com.angcyo.bluetooth.fsc.ReceiveCancelException
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker._deviceConfigBean
import com.angcyo.bluetooth.fsc.laserpacker.bean._useCutCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.ExitCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.FileModeCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.QueryCmd
import com.angcyo.bluetooth.fsc.laserpacker.parse.FileTransferParser
import com.angcyo.bluetooth.fsc.laserpacker.parse.QueryEngraveFileParser
import com.angcyo.bluetooth.fsc.laserpacker.parse.QueryIndexExistParser
import com.angcyo.bluetooth.fsc.laserpacker.syncQueryDeviceState
import com.angcyo.bluetooth.fsc.laserpacker.writeBleLog
import com.angcyo.bluetooth.fsc.laserpacker.writeEngraveLog
import com.angcyo.bluetooth.fsc.parse
import com.angcyo.core.component.file.writeErrorLog
import com.angcyo.core.component.file.writePerfLog
import com.angcyo.core.component.model.DataShareModel
import com.angcyo.core.vmApp
import com.angcyo.engrave2.EngraveFlowDataHelper
import com.angcyo.engrave2.data.TransferState
import com.angcyo.engrave2.transition.EngraveTransitionHelper
import com.angcyo.http.rx.doBack
import com.angcyo.http.rx.doMain
import com.angcyo.laserpacker.device.DeviceHelper
import com.angcyo.laserpacker.device.exception.EmptyException
import com.angcyo.laserpacker.device.exception.OutOfSizeException
import com.angcyo.laserpacker.device.exception.TransferException
import com.angcyo.laserpacker.toEngraveDataTypeStr
import com.angcyo.library.L
import com.angcyo.library.LTime
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.component.MainExecutor
import com.angcyo.library.component.byteWriter
import com.angcyo.library.ex.clamp
import com.angcyo.library.ex.connect
import com.angcyo.library.ex.nowTime
import com.angcyo.library.ex.toDC
import com.angcyo.library.ex.toMsTime
import com.angcyo.library.ex.toSizeString
import com.angcyo.library.ex.toStr
import com.angcyo.library.ex.trimAndPad
import com.angcyo.library.libCacheFile
import com.angcyo.objectbox.laser.pecker.LPBox
import com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity
import com.angcyo.objectbox.laser.pecker.lpSaveEntity
import com.angcyo.objectbox.saveAllEntity
import com.angcyo.viewmodel.vmDataOnce
import com.angcyo.widget.span.span
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue
import kotlin.math.max
import kotlin.math.min

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
                L.w("未指定传输数据!")
                return null
            }
            if (transferDataEntity.engraveDataType < 0) {
                L.w("未指定传输数据类型!")
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
                    transferDataEntity.layerId ?: LaserPeckerHelper.LAYER_PICTURE,
                    dpi,
                    name,
                    bytes,
                    transferDataEntity.dataDir
                )
                //0x20 GCode数据 0x21 GCode切割数据
                DataCmd.ENGRAVE_TYPE_GCODE -> DataCmd.gcodeData(
                    index,
                    x,
                    y,
                    width,
                    height,
                    name,
                    lines,
                    bytes,
                    dpi,
                    transferDataEntity.layerId == LaserPeckerHelper.LAYER_CUT && _useCutCmd /*C系列才有专属切割数据*/
                )
                //0x30 路径数据 0x31 路径切割数据
                DataCmd.ENGRAVE_TYPE_PATH -> DataCmd.pathData(
                    index,
                    x,
                    y,
                    width,
                    height,
                    name,
                    lines,
                    bytes,
                    dpi,
                    transferDataEntity.layerId == LaserPeckerHelper.LAYER_CUT && _useCutCmd
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
                    transferDataEntity.layerId ?: LaserPeckerHelper.LAYER_FILL,
                    dpi,
                    transferDataEntity.dataDir
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
                    transferDataEntity.layerId ?: LaserPeckerHelper.LAYER_PICTURE,
                    dpi,
                    transferDataEntity.dataDir
                )
            }
            return dataCmd
        }

        /**检查文件索引是否存在*/
        fun checkIndex(index: Int, mount: Int, action: (Boolean) -> Unit) {
            //检查数据索引是否存在
            if (vmApp<LaserPeckerModel>().isL5()) {
                QueryCmd.indexExist(index, mount).enqueue { bean, error ->
                    val have = bean?.parse<QueryIndexExistParser>()?.isIndexExist() == true
                    action(have)
                }
            } else {
                QueryCmd.fileList.enqueue { bean, error ->
                    val have =
                        bean?.parse<QueryEngraveFileParser>()?.indexList?.contains(index) == true
                    action(have)
                }
            }
        }

        /**只有索引的文件数据头*/
        fun indexDataHead(index: Int): ByteArray {
            return byteWriter {
                write(0, 5)
                //索引，占用4个字节
                write(index, 4)
                padLength(64) //需要64个字节
            }
        }

        /**文件头, 包含索引和文件名*/
        fun nameDataHead(index: Int, name: String = "$index"): ByteArray {
            return byteWriter {
                write(0, 5)
                //索引，占用4个字节
                write(index, 4)
                //塞满34个
                padLength(DataCmd.DEFAULT_NAME_BYTE_START)
                //第21个字节开始 共36个字节的文件名
                val nameBytes = name.toByteArray().trimAndPad(DataCmd.DEFAULT_NAME_BYTE_COUNT)
                write(nameBytes)
                write(0x00) //写入文件结束字节
                padLength(64) //需要64个字节
            }
        }

        /**简单的数据传输
         * [head] 64个字节的数据头
         * [data] 数据本身
         * */
        fun singleTransferData(head: ByteArray, data: ByteArray, action: (Throwable?) -> Unit) {
            LTime.tick()
            val size = data.size
            val fileModeCmd = FileModeCmd(size)
            fileModeCmd.enqueue(FLAG_NORMAL or FLAG_CLEAR_BEFORE) { bean, error ->
                if (error == null && bean?.parse<FileTransferParser>()?.isIntoFileMode() == true) {
                    //成功进入文件传输模式
                    val dataCmd = DataCmd(head, data, "传输数据")
                    dataCmd.enqueue { bean, error ->
                        val result = bean?.parse<FileTransferParser>()
                        buildString {
                            append("传输结束[${LTime.time()}]:$result $error ")
                            append("Success:${result?.isFileTransferSuccess().toDC()}")
                        }.writeBleLog(L.WARN)

                        if (error == null && result?.isFileTransferSuccess() == true) {
                            //成功传输数据
                            "成功传输数据[${size.toSizeString()}]".writeBleLog()
                            action(null)
                        } else {
                            "未成功传输数据[${size.toSizeString()}]".writeErrorLog()
                            action(error)
                        }
                    }
                } else {
                    "未成功进入数据传输模式:${error.toStr()}".writeErrorLog()
                    error?.printStackTrace()
                    action(error ?: IllegalStateException("未成功进入数据传输模式"))
                }
            }
        }
    }

    /**雕刻数据转换管理*/
    //val engraveTransitionManager = EngraveTransitionManager()

    /**当前任务的状态通知*/
    val transferStateOnceData = vmDataOnce<TransferState>()

    /**缓存*/
    var _transferState: TransferState? = null

    val deviceStateModel = vmApp<DeviceStateModel>()

    //

    /*
        */
    /**开始创建机器需要的传输数据*//*
    @CallPoint
    @AnyThread
    fun startCreateTransferData(taskId: String?, canvasDelegate: CanvasDelegate) {
        //清空之前之前的所有传输数据
        "开始创建传输数据[$taskId]".writeToLog()
        EngraveFlowDataHelper.removeTransferDataState(taskId)
        stopTransfer()
        doBack {
            //传输状态, 开始创建数据
            "即将创建传输数据[$taskId]".writeToLog()
            val transferState = TransferState(taskId, progress = -1)
            try {
                EngraveFlowDataHelper.onStartCreateTransferData(taskId)
                transferStateOnceData.postValue(transferState)
                val transferConfigEntity = EngraveFlowDataHelper.generateTransferConfig(taskId)
                val entityList = engraveTransitionManager.transitionTransferData(
                    canvasDelegate,
                    transferConfigEntity
                )//数据已入库, 可以直接在数据库中查询
                "已创建传输数据[$taskId]:$entityList".writeToLog()
                EngraveFlowDataHelper.onFinishCreateTransferData(taskId)
                startTransferData(transferState.taskId)
            } catch (e: Exception) {
                "$e".writeErrorLog()
                errorTransfer(transferState, TransferException())
            }
        }
        "请等待数据创建完成[$taskId]".writeToLog()
    }*/

    /**开始传输机器需要的数据*/
    @CallPoint
    @AnyThread
    fun startTransferData(taskId: String?) {
        stopTransfer()
        val transferState = TransferState(taskId, TransferState.TRANSFER_STATE_NORMAL, 0)
        _transferState = transferState
        val list = EngraveFlowDataHelper.getTransferDataList(taskId)
        EngraveFlowDataHelper.startTransferData(taskId, list)//检测数据
        if (list.isEmpty()) {
            //需要传输的数据为空, 是直接完成传输?还是空异常报错?
            "传输数据为空:[${taskId}]".writeEngraveLog()
            transferState.state = TransferState.TRANSFER_STATE_FINISH
            transferState.error = EmptyException()
            transferStateOnceData.postValue(transferState)
            return
        }
        doBack {
            //开始传输
            "准备传输任务:[${taskId}][${list.firstOrNull()?.name}][${list.connect { it.index.toStr() }}]".writeEngraveLog()

            EngraveFlowDataHelper.clearTransferDataState(taskId)  //清空所有数据已经传输完成的状态, 从设备中读取判断

            transferStateOnceData.postValue(transferState)
            _transferNext(transferState)
        }
    }

    /**开始传输同一个任务的下一个文件*/
    fun startTransferNextData(taskId: String?) {
        val transferState = _transferState ?: return
        "准备传输下一个文件:[${taskId}]".writeEngraveLog()

        transferState.state = TransferState.TRANSFER_STATE_NORMAL
        transferStateOnceData.postValue(transferState)
        _transferNext(transferState)
    }

    /**所有数据是否全部传输完成*/
    fun isAllTransferFinish(taskId: String?): Boolean {
        val transferDataEntity = EngraveFlowDataHelper.getNeedTransferData(taskId)
        return transferDataEntity == null
    }

    /**重新传输
     * [all] 是否全部重新传输, 否则只传输未成功的数据
     * */
    @AnyThread
    @CallPoint
    fun retryTransfer(all: Boolean) {
        val transferState = _transferState
        if (transferState == null) {
            val state = TransferState(null, progress = -1)
            errorTransfer(state, EmptyException())
            return
        }
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
        _transferTask?._receiveTask?.isCancel = true
        _transferTask = null
        //need?
        //CommandQueueHelper.clearCommand()
    }

    /**传输异常*/
    fun errorTransfer(transferState: TransferState, error: Throwable?) {
        transferState.state = TransferState.TRANSFER_STATE_FINISH
        transferState.error = error
        transferStateOnceData.postValue(transferState)
        UMEvent.APP_ERROR.umengEventValue {
            put(UMEvent.KEY_TRANSFER_ERROR, error?.message ?: "传输异常")
        }
    }

    /**传输异常*/
    fun errorTransfer(error: Throwable?) {
        _transferState?.let {
            errorTransfer(it, error)
        }
    }

    //

    /**继续查找并传输下一个需要传输的数据*/
    @WorkerThread
    fun _transferNext(transferState: TransferState) {
        val taskId: String? = transferState.taskId
        val transferDataEntity = EngraveFlowDataHelper.getNeedTransferData(taskId)
        transferState.transferDataEntity = transferDataEntity
        if (transferState.state == TransferState.TRANSFER_STATE_CANCEL) {
            //
            "传输被取消[${taskId}]:$transferState".writeEngraveLog()
        } else if (transferDataEntity == null) {
            //全部传输完成
            _transferFinish(transferState)
        } else {
            //需要传输数据, 从设备中读取索引
            if (HawkEngraveKeys.enableTransferIndexCheck) {
                checkIndex(transferDataEntity.index, transferDataEntity.mount) {
                    if (it) {
                        //下位机已经有对应的索引文件, 则直接传输下一个
                        transferDataEntity.isTransfer = true
                        transferDataEntity.lpSaveEntity()
                        "索引已存在[${transferDataEntity.index}], 跳过传输!".writeEngraveLog()

                        if (HawkEngraveKeys.enableSingleItemTransfer) {
                            //激活了单文件传输, 则传输完一个文件, 雕刻一个文件
                            _transferFinish(transferState)//传输完成
                        } else {
                            _transferNext(transferState)//下一个
                        }
                    } else {
                        if (transferState.state == TransferState.TRANSFER_STATE_NORMAL) {
                            //状态正常
                            wrapTransferData(transferState, transferDataEntity)
                        }
                    }
                }
            } else {
                //不检查直接传输数据
                wrapTransferData(transferState, transferDataEntity)
            }
        }
    }

    /**传输阶段完成*/
    fun _transferFinish(transferState: TransferState) {
        val taskId: String? = transferState.taskId
        transferState.state = TransferState.TRANSFER_STATE_FINISH
        transferState.error = null
        if (HawkEngraveKeys.enableSingleItemTransfer) {
            //单文件单传, 传输进度需要计算
            transferState.progress = calcTransferProgress(taskId, 100)
        } else {
            transferState.progress = 100
        }
        transferStateOnceData.postValue(transferState)
        EngraveFlowDataHelper.finishTransferData(taskId)
        "数据传输任务完成:${taskId},流转!".writeEngraveLog()

        //进入空闲模式
        ExitCmd().enqueue { bean, error ->
            syncQueryDeviceState()
        }
    }

    private val _transferFinishRunnable = Runnable {
        _transferState?.let { transferState ->
            "传输超时结束:[${transferState.taskId}]:${transferState.transferDataEntity?.index}".writeErrorLog()
            _transferTask?._receiveTask?.isCancel = true
            _transferTask = null
            removeTransferFinishCheck()

            _transferFinish(transferState)//传输完成
        }
    }

    private fun transferFinishCheck() {
        MainExecutor.delay(_transferFinishRunnable, DataCmd.MAX_RECEIVE_TIMEOUT)
    }

    private fun removeTransferFinishCheck() {
        MainExecutor.remove(_transferFinishRunnable)
    }

    /**传输的任务, 用来停止发送*/
    private var _transferTask: CommandQueueHelper.CommandInfo? = null

    /**
     * 发送数据之前, 需要先退出当前的模式
     * [transferData]*/
    private fun wrapTransferData(
        transferState: TransferState,
        transferDataEntity: TransferDataEntity
    ) {
        transferData(transferState, transferDataEntity)

        /*ExitCmd().enqueue { bean, error ->
            if (error == null) {
                transferData(transferState, transferDataEntity)
            } else {
                errorTransfer(transferState, error)
            }
        }*/
    }

    /**传输数据
     * [action] 成功或者失败的回调*/
    fun transferData(
        transferState: TransferState,
        transferDataEntity: TransferDataEntity,
        action: (Throwable?) -> Unit = {}
    ) {
        val taskId = transferDataEntity.taskId
        val size = transferDataEntity.bytes()?.size ?: 0
        val sizeString = size.toSizeString()

        "开始传输数据:[单传${HawkEngraveKeys.enableSingleItemTransfer.toDC()}]:[$taskId][${transferDataEntity.index}] $sizeString".writeEngraveLog()
        if (size <= 0) {
            "传输数据为空:[${taskId}]${transferDataEntity.index}".writeErrorLog()
            val emptyException = EmptyException()
            if (HawkEngraveKeys.enableSingleItemTransfer && HawkEngraveKeys.ignoreEngraveError) {
                //激活了文件单传, 并且忽略错误, 则传输完成
                transferDataEntity.isTransfer = true
                transferDataEntity.lpSaveEntity()

                _transferNext(transferState)
                action(emptyException)
            } else {
                errorTransfer(transferState, emptyException)
                action(transferState.error)
            }
            return
        } else if (size > (_deviceConfigBean?.maxTransferDataSize
                ?: HawkEngraveKeys.maxTransferDataSize)
        ) {
            "传输数据过大:${transferDataEntity.index}$sizeString".writeErrorLog()
            errorTransfer(transferState, OutOfSizeException())
            action(transferState.error)
            return
        }
        LTime.tick()
        transferDataEntity.deviceAddress = LaserPeckerHelper.lastDeviceAddress()
        transferDataEntity.lpSaveEntity()

        val fileModeCmd = FileModeCmd(size)
        _transferTask = fileModeCmd.enqueue(FLAG_NORMAL or FLAG_CLEAR_BEFORE) { bean, error ->
            "进入大数据模式[${(error == null).toDC()}]:$sizeString,耗时:${LTime.time()}".writePerfLog()
            error?.let {
                "进入文件传输模式异常:${it} [${transferDataEntity.index}] $sizeString".writeErrorLog()
                errorTransfer(transferState, TransferException(error))
                action(transferState.error)
            }
            if (error == null && transferState.state == TransferState.TRANSFER_STATE_NORMAL) {
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
                                append("开始传输:[$taskId][${transferDataEntity.index}] $sizeString")
                                append(" ${transferDataEntity.engraveDataType.toEngraveDataTypeStr()}")
                                append(" $transferDataEntity")
                            }.writeEngraveLog()

                            val startTransferTime = nowTime()//开始传输的时间
                            val speedList = mutableListOf<Float>() //用来计算平均速率
                            _transferTask = dataCmd.enqueue(progress = {
                                //进度
                                val progress = calcTransferProgress(taskId, it.sendPacketPercentage)
                                val sendSpeed = it.sendSpeed
                                if (progress > 10) {
                                    speedList.add(sendSpeed)
                                }
                                EngraveFlowDataHelper.updateTransferDataProgress(
                                    taskId,
                                    progress,
                                    sendSpeed,
                                    speedList.average().toFloat()
                                ) {
                                    dataName = transferDataEntity.name
                                    dataIndex = transferDataEntity.index
                                }
                                doMain {
                                    //及时回调
                                    transferState.progress = progress
                                    transferStateOnceData.value = transferState
                                }
                                if (progress >= 100) {
                                    //进度达到100%时, 进行传输超时检查, 防止机器假死
                                    transferFinishCheck()
                                }
                            }) { bean, error ->
                                _transferTask = null
                                removeTransferFinishCheck()

                                if (error is ReceiveCancelException) {
                                    //cancel
                                } else {
                                    val result = bean?.parse<FileTransferParser>()
                                    buildString {
                                        append("传输结束:$result $error ")
                                        append("Success:${result?.isFileTransferSuccess().toDC()}")
                                    }.writeBleLog(L.WARN)

                                    result?.let {
                                        if (result.isFileTransferSuccess()) {
                                            //文件传输完成
                                            val nowTime = nowTime()
                                            "传输完成[$taskId][${transferDataEntity.index}],耗时:${(nowTime - startTransferTime).toMsTime()}"
                                                .writeEngraveLog().writePerfLog()

                                            transferDataEntity.isTransfer = true
                                            transferDataEntity.lpSaveEntity()

                                            if (HawkEngraveKeys.enableSingleItemTransfer) {
                                                //激活了单文件传输, 则传输完一个文件, 雕刻一个文件
                                                _transferFinish(transferState)//传输完成
                                            } else {
                                                _transferNext(transferState)
                                            }

                                            action(null)
                                        } else if (transferState.state == TransferState.TRANSFER_STATE_NORMAL) {
                                            "数据接收未完成:[${transferDataEntity.index}]".writeErrorLog()
                                            errorTransfer(transferState, TransferException())
                                            action(transferState.error)
                                        }
                                    }
                                    if (result == null && transferState.state == TransferState.TRANSFER_STATE_NORMAL) {
                                        "发送数据失败,无效的返回值:[${transferDataEntity.index}] error:${error.toStr()}".writeErrorLog()
                                        errorTransfer(transferState, TransferException())
                                        action(transferState.error)
                                    }
                                }
                            }
                            //end data cmd
                        }
                        //end parse
                    } else if (transferState.state == TransferState.TRANSFER_STATE_NORMAL) {
                        "未成功进入数据传输模式:[${transferDataEntity.index}]".writeErrorLog()
                        errorTransfer(transferState, TransferException())
                        action(transferState.error)
                    }
                }
                //end传输指令
            }
            //end file mode cmd
        }
    }

    /**传输数据测试
     * [size] 需要传输的数据大小, 单位:字节byte
     * */
    fun transferDataTest(size: Long, progressAction: (CharSequence?) -> Unit) {
        LTime.tick()
        val sizeString = size.toSizeString()
        val sizeInt = size.toInt()
        span {
            append("请稍等,正在进入大数据模式:")
            append("[$sizeString]") {
                foregroundColor = EngraveTransitionHelper.accentColor
            }
        }.apply { vmApp<DataShareModel>().shareTextOnceData.postValue(this) }
        val fileModeCmd = FileModeCmd(sizeInt - LaserPeckerHelper.PACKET_FILE_HEAD_SIZE)
        fileModeCmd.enqueue(FLAG_NORMAL or FLAG_CLEAR_BEFORE) { bean, error ->
            span {
                append("进入大数据[$sizeString]模式[${(error == null).toDC()}],")
                append("耗时:${LTime.time()}") {
                    foregroundColor = EngraveTransitionHelper.accentColor
                }
                bean?.parse<FileTransferParser>()?.let {
                    if (it.isIntoFileMode()) {
                        //成功进入大数据模式, 开始发送数据
                        LTime.tick()
                        //DataCmd(ByteArray(0), ByteArray(sizeInt))

                        val cacheFile = libCacheFile("test_data.txt")
                        DeviceHelper.tempEngraveLogPathList.add(cacheFile.absolutePath)

                        val dataCmd = DataCmd.testDataCmd(sizeInt, cacheFile)
                        var minSpeed: Float? = null
                        var maxSpeed: Float? = null
                        val speedList = mutableListOf<Float>()
                        val startTime = nowTime()
                        dataCmd.enqueue(progress = {
                            //进度[0~100]
                            val progress = it.sendPacketPercentage
                            //byte/s
                            val sendSpeed = it.sendSpeed
                            maxSpeed =
                                if (maxSpeed == null) sendSpeed else max(maxSpeed!!, sendSpeed)
                            if (progress > 2) {
                                minSpeed =
                                    if (minSpeed == null) sendSpeed else min(minSpeed!!, sendSpeed)
                                speedList.add(sendSpeed)
                            }
                            progressAction(
                                span {
                                    append("发送[${sizeString}]进度:")
                                    append("${progress}%") {
                                        foregroundColor = EngraveTransitionHelper.accentColor
                                    }
                                    append(" 速度:")
                                    append("${sendSpeed.toLong().toSizeString()}/s") {
                                        foregroundColor = EngraveTransitionHelper.accentColor
                                    }
                                    append(" 最慢:")
                                    append("${(minSpeed ?: 0).toLong().toSizeString()}/s") {
                                        foregroundColor = EngraveTransitionHelper.accentColor
                                    }
                                    append(" 最快:")
                                    append("${(maxSpeed ?: 0).toLong().toSizeString()}/s") {
                                        foregroundColor = EngraveTransitionHelper.accentColor
                                    }
                                    append(" 平均:")
                                    append("${(speedList.average()).toLong().toSizeString()}/s") {
                                        foregroundColor = EngraveTransitionHelper.accentColor
                                    }
                                    append(" 耗时:${LTime.time(startTime)}") {
                                        foregroundColor = EngraveTransitionHelper.accentColor
                                    }
                                }
                            )
                        }) { bean, error ->
                            val result = bean?.parse<FileTransferParser>()
                            span {
                                append("传输[$sizeString]")
                                append(" 平均速率:")
                                append("${(speedList.average()).toLong().toSizeString()}/s") {
                                    foregroundColor = EngraveTransitionHelper.accentColor
                                }
                                append(" 结束[${result?.isFileTransferSuccess().toDC()}]")
                                append(" 耗时:${LTime.time()}") {
                                    foregroundColor = EngraveTransitionHelper.accentColor
                                }
                            }.apply { vmApp<DataShareModel>().shareTextOnceData.postValue(this) }
                        }
                    } else {
                        appendln()
                        append("未成功进入数据传输模式") {
                            foregroundColor = EngraveTransitionHelper.accentColor
                        }
                    }
                }
            }.apply { vmApp<DataShareModel>().shareTextOnceData.postValue(this) }
        }
    }

    /**更新传输消息通知*/
    fun updateTransferMessage(message: String?) {
        _transferState?.let {
            EngraveFlowDataHelper.updateTransferMessage(it.taskId, message)
            transferStateOnceData.postValue(it)
        }
    }
}