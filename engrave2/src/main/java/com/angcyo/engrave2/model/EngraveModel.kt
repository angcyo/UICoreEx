package com.angcyo.engrave2.model

import com.angcyo.bluetooth.fsc.FscBleApiModel
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker._deviceConfigBean
import com.angcyo.bluetooth.fsc.laserpacker.bean.FileIndexBean
import com.angcyo.bluetooth.fsc.laserpacker.command.CommandException
import com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.ExitCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.QueryCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.toEngraveTypeStr
import com.angcyo.bluetooth.fsc.laserpacker.parse.MiniReceiveParser
import com.angcyo.bluetooth.fsc.laserpacker.parse.NoDeviceException
import com.angcyo.bluetooth.fsc.laserpacker.parse.toErrorStateString
import com.angcyo.bluetooth.fsc.laserpacker.syncQueryDeviceState
import com.angcyo.bluetooth.fsc.laserpacker.writeEngraveLog
import com.angcyo.bluetooth.fsc.parse
import com.angcyo.core.component.file.writeErrorLog
import com.angcyo.core.lifecycle.LifecycleViewModel
import com.angcyo.core.vmApp
import com.angcyo.engrave2.*
import com.angcyo.engrave2.transition.EngraveTransitionHelper
import com.angcyo.http.rx.doBack
import com.angcyo.http.rx.doMain
import com.angcyo.http.tcp.TcpClosedException
import com.angcyo.laserpacker.device.*
import com.angcyo.laserpacker.device.exception.EmptyException
import com.angcyo.library.L
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.annotation.Private
import com.angcyo.library.component.VersionMatcher
import com.angcyo.library.ex.clamp
import com.angcyo.library.ex.nowTime
import com.angcyo.library.ex.toDC
import com.angcyo.library.ex.toMsTime
import com.angcyo.library.ex.toStr
import com.angcyo.library.unit.IValueUnit.Companion.MM_UNIT
import com.angcyo.objectbox.laser.pecker.LPBox
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.EngraveDataEntity
import com.angcyo.objectbox.laser.pecker.entity.EngraveDataEntity_
import com.angcyo.objectbox.laser.pecker.entity.EngraveTaskEntity
import com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity
import com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity_
import com.angcyo.objectbox.laser.pecker.lpSaveEntity
import com.angcyo.objectbox.removeAll
import com.angcyo.viewmodel.IViewModel
import com.angcyo.viewmodel.vmDataOnce
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue
import java.util.concurrent.CountDownLatch
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 雕刻数据存储/管理
 *
 * 需要雕刻的数据放在 [TransferDataEntity]
 * 需要雕刻的参数放在 [EngraveConfigEntity]
 * 通过[taskId]关联
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/21
 */
class EngraveModel : LifecycleViewModel(), IViewModel {

    companion object {
        /**雕刻状态: 开始雕刻*/
        const val ENGRAVE_STATE_START = 1

        /**雕刻状态: 已暂停*/
        const val ENGRAVE_STATE_PAUSE = 2

        /**雕刻状态: 当前索引雕刻已完成*/
        const val ENGRAVE_STATE_INDEX_FINISH = 3

        /**雕刻状态: 已完成*/
        const val ENGRAVE_STATE_FINISH = 4

        /**雕刻状态: 异常*/
        const val ENGRAVE_STATE_ERROR = 5

        /**最后一次雕刻的次数*/
        var _lastEngraveTimes: Int = 1

        /**多文件雕刻指令, 上一次雕刻的索引*/
        var _lastEngraveIndex: Int = -1

        var _lastEngraveFileName: String? = null
    }

    val laserPeckerModel = vmApp<LaserPeckerModel>()

    val deviceStateModel = vmApp<DeviceStateModel>()

    val fscBleApiModel = vmApp<FscBleApiModel>()

    /**雕刻状态通知*/
    val engraveStateData = vmDataOnce<EngraveTaskEntity>()

    /**是否发送过雕刻指令*/
    private var isSendEngraveCmd = false

    //缓存
    var _engraveTaskId: String? = null

    //最后一次雕刻指令发送出去, 是否有异常
    var _lastEngraveCmdError: Throwable? = null

    /**必须要实时获取, 否则在其他地方修改了数据, 在这里还是缓存, 不会是最新的*/
    val _engraveTaskEntity: EngraveTaskEntity?
        get() = EngraveFlowDataHelper.getEngraveTask(_engraveTaskId)

    init {
        //监听雕刻状态
        deviceStateModel.deviceStateData.observe(this) { queryState ->
            queryState?.let {
                if (_engraveTaskId != null && isSendEngraveCmd) {
                    //有任务在执行
                    if (queryState.isModeIdle()) {
                        isSendEngraveCmd = false
                        if (_engraveTaskEntity?.state != ENGRAVE_STATE_FINISH) {
                            //机器空闲了, 可能一个数据雕刻结束了
                            if (_lastEngraveIndex > 0) {
                                EngraveFlowDataHelper.finishDataEngrave(
                                    _engraveTaskId,
                                    _lastEngraveIndex,
                                    _lastEngraveFileName,
                                    EngraveDataEntity.FINISH_REASON_IDLE
                                )
                            }
                            if (_lastEngraveCmdError == null) {
                                val printTimes =
                                    if (queryState.printTimes <= 0) null else queryState.printTimes
                                _checkEngraveNextOnIdle(
                                    queryState.index,
                                    _lastEngraveFileName,
                                    printTimes,
                                    100,
                                    EngraveDataEntity.FINISH_REASON_IDLE
                                )
                            }
                        }
                    } else if (queryState.isEngraving()) {
                        //雕刻中, 更新对应的雕刻进度
                        //  _startEngraveFileName(_lastEngraveFileName)
                        if (queryState.isFileNameEngrave()) {
                            //文件名雕刻
                        } else {
                            if (_lastEngraveIndex != queryState.index) {
                                _startEngraveIndex(queryState.index)
                                if (_lastEngraveIndex != -1) {
                                    //单元素雕刻, 一定要等待机器进入空闲才算完成雕刻
                                    EngraveFlowDataHelper.finishDataEngrave(
                                        _engraveTaskId,
                                        _lastEngraveIndex,
                                        _lastEngraveFileName,
                                        EngraveDataEntity.FINISH_REASON_INDEX
                                    )
                                }
                            }

                            //多文件雕刻的小索引
                            _lastEngraveIndex = queryState.index
                        }

                        if (_lastEngraveTimes != queryState.printTimes) {
                            _logEngraveDuration(
                                queryState.index,
                                _lastEngraveFileName,
                                _lastEngraveTimes,
                                "雕刻次数不一致:${_lastEngraveTimes}/${queryState.printTimes}"
                            )
                            _lastEngraveTimes = queryState.printTimes
                            _engraveTaskEntity?.apply {
                                indexPrintStartTime = nowTime()
                                lpSaveEntity()
                            }
                        }
                        updateEngraveProgress(
                            queryState.index,
                            _lastEngraveFileName,
                            queryState.printTimes,
                            queryState.rate
                        )
                    } else if (queryState.isEngravePause()) {
                        //
                    } else if (queryState.error != 0) {
                        //有异常, 暂停雕刻
                        val pause = !HawkEngraveKeys.ignoreEngraveError
                        buildString {
                            append("雕刻中出现异常码[${queryState.error}:${queryState.error.toErrorStateString()}],")
                            append("暂停雕刻[${pause.toDC()}]:${_engraveTaskId}")
                        }.writeEngraveLog().writeErrorLog()
                        if (pause) {
                            isSendEngraveCmd = false
                            pauseEngrave()
                        }
                    } else {
                        //
                        L.w("未处理的雕刻模式:${queryState.mode} ${queryState.workState}")
                    }
                }
            }
        }
    }

    /**打印一下*/
    fun _logEngraveDuration(index: Int, fileName: String?, printTimes: Int, reason: String?) {
        val nowTime = nowTime()
        val duration = nowTime - (_engraveTaskEntity?.indexPrintStartTime ?: 0)
        buildString {
            append("雕刻完成[${_engraveTaskId}]")
            append(":[${index}][$fileName]")
            append("第${printTimes}次 ")
            append("耗时:${duration.toMsTime()} ")
            append(reason ?: "")
        }.writeEngraveLog(L.INFO)
    }

    /**开始雕刻*/
    @CallPoint
    fun startEngrave(
        taskId: String?,
        fileIndexBean: FileIndexBean? = null
    ): EngraveTaskEntity {
        val task = EngraveFlowDataHelper.generateEngraveTask(taskId, fileIndexBean)
        if (task.dataList.isNullOrEmpty()) {
            //无数据需要雕刻
            "雕刻任务无数据[${taskId}]".writeEngraveLog()
            errorEngrave(EmptyException())
        } else {
            "即将开始雕刻[$taskId]:${laserPeckerModel.deviceSettingData.value}".writeEngraveLog()
            _startEngraveTask(task)
        }
        return task
    }

    /**开始文件名雕刻任务*/
    @CallPoint
    fun startEngrave(
        taskId: String,
        layerId: String,
        fileIndexBean: FileIndexBean
    ): EngraveTaskEntity {
        //清除之前的雕刻数据
        EngraveDataEntity::class.removeAll(LPBox.PACKAGE_NAME) {
            apply(EngraveDataEntity_.taskId.equal(taskId))
        }
        //创建传输数据
        TransferDataEntity::class.removeAll(LPBox.PACKAGE_NAME) {
            apply(TransferDataEntity_.taskId.equal(taskId))
        }
        TransferDataEntity().apply {
            this.taskId = taskId
            this.index = fileIndexBean.index
            this.fileName = fileIndexBean.name
            this.mount = fileIndexBean.mount
            this.layerId = layerId
            this.isTransfer = true
            lpSaveEntity()
        }
        //创建雕刻任务
        return startEngrave(taskId, fileIndexBean)
    }

    /**开始雕刻下一个索引*/
    fun startEngraveNext(taskId: String?) {
        //clear
        _engraveTaskId = taskId

        _engraveTaskEntity?.apply {
            currentIndex = -1
            currentFileName = null
            state = ENGRAVE_STATE_START
            lpSaveEntity()
        }

        buildString {
            append("开始雕刻下一个:[${taskId}]:${_engraveTaskEntity}")
        }.writeEngraveLog()
        engraveNextData()
    }

    /**完成当前索引的雕刻任务*/
    fun finishCurrentIndexEngrave() {
        isSendEngraveCmd = false
        val task = _engraveTaskEntity ?: return
        val index = task.currentIndex
        val fileName = task.currentFileName
        buildString {
            append("直接完成雕刻:${_engraveTaskId} ")
            append("索引:[${index}][${fileName}]")
        }.writeEngraveLog(L.INFO)

        //更新雕刻进度
        _checkEngraveNextOnIdle(index, fileName, null, 100, EngraveDataEntity.FINISH_REASON_SKIP)
    }

    /**所有的索引, 是否都雕刻完成了*/
    fun isAllEngraveFinish(taskId: String?): Boolean {
        return if (_engraveTaskEntity?.isFileNameEngrave == true) {
            EngraveFlowDataHelper.getNextEngraveFileName(taskId) == null
        } else {
            EngraveFlowDataHelper.getNextEngraveIndex(taskId) == null
        }
    }

    /**雕刻完成后, 触发下一个雕刻*/
    fun _checkEngraveNextOnIdle(
        index: Int,
        fileName: String?,
        printTimes: Int?,
        progress: Int,
        reason: Int
    ) {
        deviceStateModel.pauseLoopCheckState(true, "雕刻完成,暂停检查状态")
        val nowTime = nowTime()
        UMEvent.ENGRAVE.umengEventValue {
            val duration = nowTime - (_engraveTaskEntity?.startTime ?: 0)
            put(UMEvent.KEY_FINISH_TIME, nowTime.toString())
            put(UMEvent.KEY_DURATION, duration.toString())
        }
        _logEngraveDuration(index, fileName, _lastEngraveTimes, "机器空闲状态")
        _lastEngraveTimes = 1
        _lastEngraveIndex = -1
        _lastEngraveFileName = null
        //强制更新进度到100
        updateEngraveProgress(index, fileName, printTimes, progress)

        //完成指定索引的雕刻
        EngraveFlowDataHelper.finishDataEngrave(_engraveTaskId, index, fileName, reason)

        if (isBatchEngraveSupport()) {
            finishEngrave("批量雕刻指令完成")
        } else {
            if (HawkEngraveKeys.enableSingleItemTransfer &&
                !isAllEngraveFinish(_engraveTaskId)
            ) {
                //激活了单文件雕刻, 并且未完成雕刻
                finishDataEngrave()
            } else {
                engraveNextData()
            }
        }
    }

    /**再雕一次*/
    @CallPoint
    fun againEngrave(taskId: String?) {
        val task = EngraveFlowDataHelper.getEngraveTask(taskId)
        task?.let {
            //clear
            EngraveFlowDataHelper.againEngrave(it.taskId)
            _startEngraveTask(it)
        }
    }

    @Private
    fun _startEngraveTask(
        task: EngraveTaskEntity,
        itemEngraveParams: Boolean = HawkEngraveKeys.enableItemEngraveParams
    ) {
        task.apply {
            startTime = nowTime()
            finishTime = -1
            currentIndex = -1
            state = ENGRAVE_STATE_START

            //雕刻参数获取方式
            enableItemEngraveParams = itemEngraveParams
            lpSaveEntity()

            _engraveTaskId = task.taskId

            //构建所有图层的雕刻参数, 确保有数据
            EngraveFlowDataHelper.generateEngraveConfig(task.taskId)

            //通知开始雕刻
            engraveStateData.postValue(this)

            //
            if (isBatchEngraveSupport(this)) {
                batchEngrave()
            } else {
                buildString {
                    append("开始雕刻任务:[${taskId}][${task.dataList}] $task")
                }.writeEngraveLog()
                engraveNextData()
            }
        }
    }

    /**恢复雕刻任务*/
    @CallPoint
    fun restoreEngrave(taskId: String?) {
        val task = EngraveFlowDataHelper.getEngraveTask(taskId)
        task?.apply {
            _engraveTaskId = task.taskId
            //loop
            deviceStateModel.startLoopCheckState(reason = "恢复雕刻任务")
        }
    }

    /**雕刻下一个文件索引或文件
     * [engraveNextFileName]
     * [engraveNextDataIndex]*/
    @CallPoint
    fun engraveNextData() {
        val task = _engraveTaskEntity ?: return
        if (task.isFileNameEngrave) {
            //文件名雕刻
            engraveNextFileName()
        } else {
            //索引雕刻
            engraveNextDataIndex()
        }
    }

    /**雕刻下一个文件索引*/
    @CallPoint
    private fun engraveNextDataIndex() {
        val task = _engraveTaskEntity ?: return
        val taskId = task.taskId

        if (task.currentIndex > 0) {
            //之前的雕刻索引
            EngraveFlowDataHelper.finishDataEngrave(taskId, task.currentIndex, null, null)
        }

        //查找下一个未完成雕刻的索引
        val nextIndex = EngraveFlowDataHelper.getNextEngraveIndex(taskId)

        if (nextIndex == null) {
            //雕刻完成
            finishEngrave("无下一个数据")
        } else {
            //开始雕刻
            val engraveDataEntity = _generateEngraveData(taskId, nextIndex)

            val transferDataEntity = EngraveFlowDataHelper.getTransferData(taskId, nextIndex)

            if (transferDataEntity == null) {
                if (engraveDataEntity.isFromDeviceHistory) {

                } else {
                    //
                    //需要雕刻的数据不存在,则直接完成
                    engraveDataEntity.progress = 100
                    engraveDataEntity.lpSaveEntity()
                    engraveNextDataIndex()
                    return
                }
            }

            //雕刻配置数据
            val engraveConfigEntity = if (task.enableItemEngraveParams) {
                EngraveFlowDataHelper.getEngraveIndexConfig(
                    engraveDataEntity.taskId,
                    "${engraveDataEntity.index}"
                ) ?: EngraveFlowDataHelper.generateEngraveConfig(
                    taskId,
                    transferDataEntity?.layerId
                )
            } else {
                EngraveFlowDataHelper.generateEngraveConfig(taskId, transferDataEntity?.layerId)
            }
            doMain {
                _startEngraveIndex(nextIndex)

                //
                _startEngraveCmd(
                    engraveDataEntity.index,
                    null,
                    transferDataEntity,
                    engraveConfigEntity
                )
            }
        }
    }

    /**批量雕刻
     * [retryCount] 失败后的重试次数*/
    @CallPoint
    fun batchEngrave(retryCount: Int = 0) {
        val task = _engraveTaskEntity ?: return
        val taskId = task.taskId

        if (task.bigIndex == null) {
            var index = EngraveHelper.generateEngraveIndex()
            task.bigIndex = index
            val mount = if (task.mount == QueryCmd.TYPE_USB) 0 else 1
            //修改低8位
            index = index and 0x00ffffff
            task.bigIndex = index or (mount shl 24)
            task.lpSaveEntity()
        }

        val indexList = task.dataList?.mapTo(mutableListOf()) {
            it.toIntOrNull() ?: -1
        } ?: emptyList()

        val powerList = mutableListOf<Byte>()
        val depthList = mutableListOf<Byte>()
        val timeList = mutableListOf<Byte>()
        val typeList = mutableListOf<Byte>()

        var precision = 0
        var diameter = 0

        //风速等级
        var pumpFill = 0
        var pumpPicture = 0
        var pumpLine = 0
        var pumpCut = 0

        //出光功率
        var laserFrequencyFill = HawkEngraveKeys.defaultLaserFrequency
        var laserFrequencyPicture = HawkEngraveKeys.defaultLaserFrequency
        var laserFrequencyLine = HawkEngraveKeys.defaultLaserFrequency
        var laserFrequencyCut = HawkEngraveKeys.defaultLaserFrequency

        //大速度
        var bigSpeedFill: Int? = 0
        var bigSpeedPicture: Int? = 0
        var bigSpeedLine: Int? = 0
        var bigSpeedCut: Int? = 0

        for (index in indexList) {
            EngraveFlowDataHelper.getTransferData(taskId, index)?.let {
                //雕刻参数
                val engraveConfigEntity = if (task.enableItemEngraveParams)
                    EngraveFlowDataHelper.getEngraveIndexConfig(it.taskId, "${it.index}")
                else
                    EngraveFlowDataHelper.getEngraveConfig(taskId, it.layerId)

                //---
                engraveConfigEntity?.let {
                    precision = engraveConfigEntity.precision
                    diameter =
                        (MM_UNIT.convertPixelToValue(engraveConfigEntity.diameterPixel) * 100).roundToInt()

                    powerList.add(engraveConfigEntity.power.toByte())
                    depthList.add(engraveConfigEntity.depth.toByte())
                    timeList.add(engraveConfigEntity.time.toByte())
                    typeList.add(engraveConfigEntity.type)

                    //保存外接设备名
                    initDefaultEngraveConfigInfo(engraveConfigEntity)
                    engraveConfigEntity.lpSaveEntity()

                    when (engraveConfigEntity.layerId) {
                        LaserPeckerHelper.LAYER_FILL -> {
                            bigSpeedFill = engraveConfigEntity.bigSpeed
                            pumpFill = engraveConfigEntity.pump
                            laserFrequencyFill =
                                if (engraveConfigEntity.useLaserFrequency) engraveConfigEntity.laserFrequency
                                    ?: HawkEngraveKeys.defaultLaserFrequency else HawkEngraveKeys.defaultLaserFrequency
                        }

                        LaserPeckerHelper.LAYER_PICTURE -> {
                            bigSpeedPicture = engraveConfigEntity.bigSpeed
                            pumpPicture = engraveConfigEntity.pump
                            laserFrequencyPicture =
                                if (engraveConfigEntity.useLaserFrequency) engraveConfigEntity.laserFrequency
                                    ?: HawkEngraveKeys.defaultLaserFrequency else HawkEngraveKeys.defaultLaserFrequency
                        }

                        LaserPeckerHelper.LAYER_LINE -> {
                            bigSpeedLine = engraveConfigEntity.bigSpeed
                            pumpLine = engraveConfigEntity.pump
                            laserFrequencyLine =
                                if (engraveConfigEntity.useLaserFrequency) engraveConfigEntity.laserFrequency
                                    ?: HawkEngraveKeys.defaultLaserFrequency else HawkEngraveKeys.defaultLaserFrequency
                        }

                        LaserPeckerHelper.LAYER_CUT -> {
                            bigSpeedCut = engraveConfigEntity.bigSpeed
                            pumpCut = engraveConfigEntity.pump
                            laserFrequencyCut =
                                if (engraveConfigEntity.useLaserFrequency) engraveConfigEntity.laserFrequency
                                    ?: HawkEngraveKeys.defaultLaserFrequency else HawkEngraveKeys.defaultLaserFrequency
                        }
                    }
                }

                //任务雕刻的数据入库
                _generateEngraveData(taskId, index)
            }
        }

        buildString {
            append("开始批量雕刻任务:[单参${_engraveTaskEntity?.enableItemEngraveParams.toDC()}]:[${taskId}] 大索引:${task.bigIndex} 小索引:$indexList")
            append(" power:${powerList}")
            append(" depth:${depthList}")
            append(" time:${timeList}")
            append(" type:${typeList}")
            append(" 加速级别:${precision}")
            append(" 直径:${diameter}")
            append(" pump:${pumpFill},${pumpPicture},${pumpLine},${pumpCut}")
            append(" laserFrequency:${laserFrequencyFill},${laserFrequencyPicture},${laserFrequencyLine},${laserFrequencyCut}")
        }.writeEngraveLog()

        //task
        task.state = ENGRAVE_STATE_START
        task.currentIndex = indexList.firstOrNull() ?: -1
        task.indexStartTime = nowTime()
        task.indexPrintStartTime = task.indexStartTime
        engraveStateData.value = task
        task.lpSaveEntity()

        isSendEngraveCmd = false

        EngraveCmd.batchEngrave(
            task.bigIndex!!,
            indexList,
            powerList,
            depthList,
            timeList,
            typeList,
            precision,
            diameter,
            pumpFill,
            pumpPicture,
            pumpLine,
            pumpCut,
            laserFrequencyFill,
            laserFrequencyPicture,
            laserFrequencyLine,
            laserFrequencyCut,
            bigSpeedFill,
            bigSpeedPicture,
            bigSpeedLine,
            bigSpeedCut,
        ).enqueue { bean, error ->
            "批量雕刻指令返回:${bean?.parse<MiniReceiveParser>()}".writeEngraveLog(L.WARN)
            _lastEngraveCmdError = error
            if (error == null) {
                //雕刻指令发送成功, 机器开始雕刻
                isSendEngraveCmd = true
                _lastEngraveIndex = indexList.firstOrNull() ?: _lastEngraveIndex
                UMEvent.ENGRAVE.umengEventValue {
                    put(UMEvent.KEY_START_TIME, nowTime().toString())
                }
                deviceStateModel.startLoopCheckState(reason = "批量雕刻指令")
            } else if (error is CommandException) {
                //指令异常
                "雕刻失败:[${indexList}] ${error.toStr()}".writeErrorLog()
                errorEngrave(error)
            } else {
                //雕刻失败, 重试
                val taskEntity = _engraveTaskEntity
                "雕刻失败:[${indexList}] ${error.toStr()}, 即将重试[${retryCount}/${HawkEngraveKeys.engraveRetryCount}]...$taskEntity".writeErrorLog()

                if (taskEntity?.state == ENGRAVE_STATE_START &&
                    vmApp<DeviceStateModel>().isDeviceConnect()
                ) {
                    if (retryCount < HawkEngraveKeys.engraveRetryCount) {
                        doBack {
                            batchEngrave(retryCount + 1)
                        }
                    } else {
                        //重试过了还是失败, 则~
                        errorEngrave(error)
                    }
                }
            }
        }
    }

    /**雕刻下一个文件名*/
    @CallPoint
    private fun engraveNextFileName() {
        val task = _engraveTaskEntity ?: return
        val taskId = task.taskId

        if (task.currentFileName != null) {
            //之前的雕刻索引
            EngraveFlowDataHelper.finishFileNameEngrave(taskId, task.currentFileName!!, null)
        }

        //查找下一个未完成雕刻的文件名
        val nextFileName = EngraveFlowDataHelper.getNextEngraveFileName(taskId)

        if (nextFileName == null) {
            //雕刻完成
            finishEngrave("无下一个数据")
        } else {
            //开始雕刻
            val engraveDataEntity = _generateEngraveData(taskId, nextFileName)

            val transferDataEntity = EngraveFlowDataHelper.getTransferData(taskId, nextFileName)

            if (transferDataEntity == null) {
                if (engraveDataEntity.isFromDeviceHistory) {

                } else {
                    //
                    //需要雕刻的数据不存在,则直接完成
                    engraveDataEntity.progress = 100
                    engraveDataEntity.lpSaveEntity()
                    engraveNextFileName()
                    return
                }
            }

            //雕刻配置数据
            val engraveConfigEntity = if (task.enableItemEngraveParams) {
                EngraveFlowDataHelper.getEngraveIndexConfig(
                    engraveDataEntity.taskId,
                    "${engraveDataEntity.index}"
                ) ?: EngraveFlowDataHelper.generateEngraveConfig(
                    taskId,
                    transferDataEntity?.layerId
                )
            } else {
                EngraveFlowDataHelper.generateEngraveConfig(taskId, transferDataEntity?.layerId)
            }
            doMain {
                _startEngraveFileName(nextFileName)

                //
                _startEngraveCmd(
                    engraveDataEntity.index,
                    engraveDataEntity.fileName,
                    transferDataEntity,
                    engraveConfigEntity
                )
            }
        }
    }

    /**生成任务需要的雕刻数据*/
    fun _generateEngraveData(taskId: String?, index: Int): EngraveDataEntity {
        val engraveDataEntity = EngraveFlowDataHelper.generateEngraveData(taskId, index)
        engraveDataEntity.startTime = nowTime()
        engraveDataEntity.lpSaveEntity()
        return engraveDataEntity
    }

    fun _generateEngraveData(taskId: String?, fileName: String): EngraveDataEntity {
        val engraveDataEntity = EngraveFlowDataHelper.generateEngraveData(taskId, fileName)
        engraveDataEntity.startTime = nowTime()
        engraveDataEntity.lpSaveEntity()
        return engraveDataEntity
    }

    /**开始雕刻指定的索引*/
    fun _startEngraveIndex(index: Int) {
        val task = _engraveTaskEntity ?: return
        if (index != -1) task.currentIndex = index
        task.state = ENGRAVE_STATE_START //雕刻已开始
        task.indexStartTime = nowTime()
        task.indexPrintStartTime = task.indexStartTime
        task.lpSaveEntity()
        engraveStateData.value = task
    }

    /**开始雕刻指定的索引*/
    fun _startEngraveFileName(fileName: String?) {
        val task = _engraveTaskEntity ?: return
        if (fileName != null) task.currentFileName = fileName
        task.state = ENGRAVE_STATE_START //雕刻已开始
        task.indexStartTime = nowTime()
        task.indexPrintStartTime = task.indexStartTime
        task.lpSaveEntity()
        engraveStateData.value = task
    }

    /**完成雕刻*/
    @CallPoint
    fun finishEngrave(reason: String?) {
        val taskId = _engraveTaskId
        deviceStateModel.pauseLoopCheckState(false, "雕刻完成")
        "完成雕刻[$taskId]:${reason ?: ""}".writeEngraveLog()

        val engraveTaskEntity = clearEngrave() ?: return

        //雕刻次数+1
        HawkEngraveKeys.lastEngraveCount++
        if (HawkEngraveKeys.lastEngraveCount > 10_0000) {
            //超过10W个之后, 清零
            HawkEngraveKeys.lastEngraveCount = 0
        }

        //post
        engraveStateData.postValue(engraveTaskEntity)

        //鸟瞰图
        EngraveTransitionHelper.saveTaskAerialView(engraveTaskEntity.taskId)
    }

    /**清理雕刻的缓存*/
    fun clearEngrave(): EngraveTaskEntity? {
        isSendEngraveCmd = false

        _lastEngraveTimes = 1
        _lastEngraveIndex = -1
        _lastEngraveFileName = null

        EngraveNotifyHelper.hideEngraveNotify()//隐藏通知

        //
        val engraveTaskEntity = _engraveTaskEntity ?: return null
        //clear
        _engraveTaskId = null
        engraveTaskEntity.currentIndex = -1
        engraveTaskEntity.currentFileName = null
        engraveTaskEntity.finishTime = nowTime()
        engraveTaskEntity.state = ENGRAVE_STATE_FINISH
        engraveTaskEntity.lpSaveEntity()

        return engraveTaskEntity
    }

    /**完成了一个索引的雕刻, 需要传输下一个索引, 并且雕刻下一个*/
    fun finishDataEngrave() {
        isSendEngraveCmd = false
        val engraveTaskEntity = _engraveTaskEntity ?: return
        engraveTaskEntity.state = ENGRAVE_STATE_INDEX_FINISH
        engraveTaskEntity.lpSaveEntity()

        //post
        engraveStateData.postValue(engraveTaskEntity)

        //鸟瞰图
        EngraveTransitionHelper.saveTaskAerialView(engraveTaskEntity.taskId)
    }

    /**暂停雕刻*/
    fun pauseEngrave() {
        val engraveState = _engraveTaskEntity ?: return
        "暂停雕刻[${engraveState.taskId}]".writeEngraveLog()
        engraveState.state = ENGRAVE_STATE_PAUSE
        engraveState.lpSaveEntity()
        engraveStateData.postValue(engraveState)
        EngraveCmd.pauseEngrave().enqueue()
    }

    /**继续雕刻*/
    fun continueEngrave() {
        val engraveState = _engraveTaskEntity ?: return
        "继续雕刻[${engraveState.taskId}]".writeEngraveLog()
        engraveState.state = ENGRAVE_STATE_START
        engraveState.lpSaveEntity()
        engraveStateData.postValue(engraveState)
        EngraveCmd.continueEngrave().enqueue()
    }

    /**停止雕刻*/
    fun stopEngrave(reason: String?, countDownLatch: CountDownLatch? = null) {
        "停止雕刻[${_engraveTaskId}]:$reason".writeEngraveLog()
        deviceStateModel.waitForExit = true
        deviceStateModel.pauseLoopCheckState(true, "停止雕刻")
        ExitCmd(timeout = HawkEngraveKeys.receiveTimeoutMax).enqueue { bean, error ->
            countDownLatch?.countDown()
            syncQueryDeviceState()
            if (error == null
                || error is NoDeviceException /*无设备连接*/
                || error is TcpClosedException /*tcp断开*/
            ) {
                //退出成功
                finishEngrave(reason)
            }
        }

        /*
        listenerIdleMode { idle, error ->
            countDownLatch?.countDown()
            deviceStateModel.pauseLoopCheckState(false)
            if (idle) {
                finishEngrave()
            }
        }*/

        /*EngraveCmd.stopEngrave().enqueue { bean, error ->
            if (error == null) {
                //停止成功
                ExitCmd().enqueue { bean, error ->
                    deviceStateModel.pauseLoopCheckState(false)
                    countDownLatch?.countDown()
                    if (error == null) {
                        //退出成功
                        finishEngrave()
                    }
                }
            } else {
                deviceStateModel.pauseLoopCheckState(false)
                countDownLatch?.countDown()
            }
        }*/
    }

    /**雕刻失败, 错误信息在
     * [com.angcyo.engrave2.model.EngraveModel._lastEngraveCmdError]*/
    fun errorEngrave(error: Exception?) {
        isSendEngraveCmd = false
        _lastEngraveCmdError = error
        val task = _engraveTaskEntity ?: return
        "雕刻异常[${task.taskId}]:${error}".writeEngraveLog()
        task.state = ENGRAVE_STATE_ERROR
        task.lpSaveEntity()
        engraveStateData.postValue(task)
    }

    //---

    /**开始雕刻, 发送雕刻指令
     * [index] 需要雕刻的索引
     * [fileName] 需要雕刻的文件名, 优先判断
     * [transferDataEntity] 需要雕刻的数据实体
     * [engraveConfigEntity] 雕刻的参数实体
     * */
    fun _startEngraveCmd(
        index: Int,
        fileName: String?,
        transferDataEntity: TransferDataEntity?,
        engraveConfigEntity: EngraveConfigEntity,
        retryCount: Int = 0
    ) {
        val diameter =
            (MM_UNIT.convertPixelToValue(engraveConfigEntity.diameterPixel) * 100).roundToInt()

        val engraveLayer = LayerHelper.getEngraveLayerInfo(engraveConfigEntity.layerId)
        buildString {
            append("开始雕刻指令:[单参${_engraveTaskEntity?.enableItemEngraveParams.toDC()}]:")
            append("[${transferDataEntity?.taskId}][$index][$fileName]")

            //雕刻数据类型
            transferDataEntity?.engraveDataType?.let {
                append(" ${it.toEngraveTypeStr()}")
            }

            if (engraveLayer?.label.isNullOrBlank()) {
                append(" layerId:${engraveConfigEntity.layerId}")
            } else {
                append(" layer:${engraveLayer?.label}")
            }

            append(" type:${engraveConfigEntity.type.toLaserTypeString()}")
            transferDataEntity?.let {
                append(" $it")
            }
            append("\n->$engraveConfigEntity")
        }.writeEngraveLog()

        initDefaultEngraveConfigInfo(engraveConfigEntity)
        engraveConfigEntity.lpSaveEntity()

        val taskEntity = _engraveTaskEntity
        taskEntity?.let {
            it.state = ENGRAVE_STATE_START
            it.lpSaveEntity()
        }
        isSendEngraveCmd = false

        val laserFrequency =
            if (engraveConfigEntity.useLaserFrequency) engraveConfigEntity.laserFrequency
                ?: HawkEngraveKeys.defaultLaserFrequency else HawkEngraveKeys.defaultLaserFrequency

        val mount =
            if (transferDataEntity?.mount == QueryCmd.TYPE_USB) QueryCmd.TYPE_USB else QueryCmd.TYPE_SD
        val engraveCmd = if (fileName == null) EngraveCmd(
            index,
            engraveConfigEntity.power.toByte(),
            engraveConfigEntity.depth.toByte(),
            0x01,
            transferDataEntity?.x ?: 0,
            transferDataEntity?.y ?: 0,
            max(1, engraveConfigEntity.time).toByte(),
            engraveConfigEntity.type,
            0x09,
            diameter,
            engraveConfigEntity.precision,
            pumpFill = engraveConfigEntity.pump,
            pumpPicture = engraveConfigEntity.pump,
            pumpLine = engraveConfigEntity.pump,
            pumpCut = engraveConfigEntity.pump,
            laserFrequencyLine = laserFrequency,
            laserFrequencyFill = laserFrequency,
            laserFrequencyPicture = laserFrequency,
            laserFrequencyCut = laserFrequency,
            mount = mount.toByte()
        ) else EngraveCmd.filenameEngrave(
            fileName, mount.toByte(),
            engraveConfigEntity.power.toByte(),
            engraveConfigEntity.depth.toByte(),
            max(1, engraveConfigEntity.time).toByte(),
            engraveConfigEntity.type,
            diameter,
            engraveConfigEntity.precision,
            laserFrequency = laserFrequency
        )

        engraveCmd.enqueue { bean, error ->
            "雕刻指令返回:${bean?.parse<MiniReceiveParser>()}".writeEngraveLog(L.WARN)
            _lastEngraveCmdError = error
            if (error == null) {
                isSendEngraveCmd = true
                //赋值, 如果机器雕刻过快, 可以不会进入progress状态
                _lastEngraveIndex = index
                _lastEngraveFileName = fileName
                //雕刻指令发送成功, 机器开始雕刻
                UMEvent.ENGRAVE.umengEventValue {
                    put(UMEvent.KEY_START_TIME, nowTime().toString())
                }
                deviceStateModel.startLoopCheckState(reason = "雕刻指令")
            } else if (error is CommandException) {
                //指令异常
                "雕刻失败:[${index}] ${error.toStr()}".writeErrorLog()
                errorEngrave(error)
            } else {
                //如果索引雕刻异常, 则不能跳过索引雕刻
                buildString {
                    append("雕刻失败:[${index}][$fileName] ")
                    append("$error, 即将重试[${retryCount}/${HawkEngraveKeys.engraveRetryCount}]...$taskEntity")
                }.writeErrorLog()

                if (taskEntity?.state == ENGRAVE_STATE_START &&
                    vmApp<DeviceStateModel>().isDeviceConnect()
                ) {
                    if (retryCount < HawkEngraveKeys.engraveRetryCount) {
                        doBack {
                            _startEngraveCmd(
                                index,
                                fileName,
                                transferDataEntity,
                                engraveConfigEntity,
                                retryCount + 1
                            )
                        }
                    } else {
                        //重试过了还是失败, 则~
                        errorEngrave(error)
                    }
                }
            }
        }
    }

    /**初始化默认的雕刻信息*/
    private fun initDefaultEngraveConfigInfo(entity: EngraveConfigEntity) {
        //保存外接设备名
        entity.exDevice = laserPeckerModel.getExDevice()
        //使用的雕刻模块
        entity.moduleState =
            deviceStateModel.deviceStateData.value?.moduleState ?: entity.moduleState
        //固件的软件版本
        entity.softwareVersion =
            laserPeckerModel.productInfoData.value?.softwareVersion ?: entity.softwareVersion
        //固件的硬件版本
        entity.hardwareVersion =
            laserPeckerModel.productInfoData.value?.hardwareVersion ?: entity.hardwareVersion

        //
        entity.deviceAddress = LaserPeckerHelper.lastDeviceAddress() ?: entity.deviceAddress
        entity.productName = laserPeckerModel.productInfoData.value?.name ?: entity.productName
    }

    /**更新雕刻进度和次数
     * [progress] 当前索引的雕刻进度*/
    fun updateEngraveProgress(index: Int, fileName: String?, printTimes: Int?, progress: Int) {
        val currentProgress = clamp(progress, 0, 100)
        EngraveFlowDataHelper.updateEngraveProgress(
            _engraveTaskEntity?.taskId,
            index,
            fileName,
            printTimes,
            currentProgress
        )
        _engraveTaskEntity?.let {
            val engraveProgress = EngraveFlowDataHelper.calcEngraveProgress(it.taskId)

            it.currentProgress = currentProgress
            it.progress = engraveProgress
            it.lpSaveEntity()
            engraveStateData.postValue(it)

            EngraveNotifyHelper.showEngraveNotify(engraveProgress)//显示通知

            "雕刻进度[${it.taskId}]:[${index}][$fileName] ${currentProgress}%/${it.progress}%/${printTimes ?: ""}".writeEngraveLog()
        }
    }

    /**是否支持批量文件雕刻, 或者当前处于批量雕刻
     *
     * 打开滑台，打开滑台多文件雕刻开关之后， 走多文件雕刻指令。
     * */
    fun isBatchEngraveSupport(task: EngraveTaskEntity? = _engraveTaskEntity): Boolean {
        if (task != null) {
            if (task.isFileNameEngrave) {
                return false
            }
        }
        /*val setting = laserPeckerModel.deviceSettingData.value ?: return false
        if (setting.sRep == 1) {
            return true
        }*/
        if (HawkEngraveKeys.enableSingleItemTransfer) {
            //单文件传输模式下, 不支持批量文件雕刻
            return false
        }

        //config
        _deviceConfigBean?.useBatchEngraveCmd?.let {
            return it
        }

        //LP4 LP5 C系列, 都支持批量雕刻
        if (laserPeckerModel.isL4() ||
            laserPeckerModel.isL5() ||
            laserPeckerModel.isCSeries()
        ) {
            return true
        }

        //debug
        val version = laserPeckerModel.productInfoData.value?.softwareVersion ?: return false
        return VersionMatcher.matches(version, DeviceHelper.batchEngraveSupportFirmware, false)
    }

    /**获取当前任务下, 指定索引文件的雕刻进度*/
    fun getEngraveIndexProgress(index: Int?): Int? {
        index ?: return null
        val engraveTask = _engraveTaskEntity ?: return null
        return EngraveFlowDataHelper.getEngraveDataEntity(engraveTask.taskId, index)?.progress
    }
}