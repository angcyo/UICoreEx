package com.angcyo.engrave2.model

import com.angcyo.bluetooth.fsc.FscBleApiModel
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.command.CommandException
import com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.ExitCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.toEngraveTypeStr
import com.angcyo.bluetooth.fsc.laserpacker.parse.MiniReceiveParser
import com.angcyo.bluetooth.fsc.laserpacker.parse.NoDeviceException
import com.angcyo.bluetooth.fsc.laserpacker.syncQueryDeviceState
import com.angcyo.bluetooth.fsc.laserpacker.writeEngraveLog
import com.angcyo.bluetooth.fsc.parse
import com.angcyo.core.component.file.writeErrorLog
import com.angcyo.core.lifecycle.LifecycleViewModel
import com.angcyo.core.vmApp
import com.angcyo.engrave2.*
import com.angcyo.http.rx.doBack
import com.angcyo.http.rx.doMain
import com.angcyo.laserpacker.device.*
import com.angcyo.laserpacker.device.exception.EmptyException
import com.angcyo.library.L
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.annotation.Private
import com.angcyo.library.component.VersionMatcher
import com.angcyo.library.ex.clamp
import com.angcyo.library.ex.nowTime
import com.angcyo.library.ex.toMsTime
import com.angcyo.library.unit.IValueUnit.Companion.MM_UNIT
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.EngraveDataEntity
import com.angcyo.objectbox.laser.pecker.entity.EngraveTaskEntity
import com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity
import com.angcyo.objectbox.laser.pecker.lpSaveEntity
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
                                EngraveFlowDataHelper.finishIndexEngrave(
                                    _engraveTaskId,
                                    _lastEngraveIndex
                                )
                            }
                            if (_lastEngraveCmdError == null) {
                                val printTimes =
                                    if (queryState.printTimes <= 0) null else queryState.printTimes
                                _checkEngraveNextOnIdle(
                                    queryState.index,
                                    printTimes,
                                    100
                                )
                            }
                        }
                    } else if (queryState.isEngraving()) {
                        //雕刻中, 更新对应的雕刻进度
                        if (_lastEngraveIndex != queryState.index) {
                            _startEngraveIndex(queryState.index)
                            if (_lastEngraveIndex > 0) {
                                EngraveFlowDataHelper.finishIndexEngrave(
                                    _engraveTaskId,
                                    _lastEngraveIndex
                                )
                            }
                        }

                        //多文件雕刻的小索引
                        _lastEngraveIndex = queryState.index

                        if (_lastEngraveTimes != queryState.printTimes) {
                            _logEngraveDuration(
                                queryState.index,
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
                            queryState.printTimes,
                            queryState.rate
                        )
                    } else if (queryState.isEngravePause()) {
                        //
                    } else if (queryState.error != 0) {
                        //有异常, 暂停雕刻
                        "雕刻中出现异常码[${queryState.error}],暂停雕刻[${_engraveTaskId}].".writeEngraveLog()
                        pauseEngrave()
                    } else {
                        //
                        L.w("未处理的雕刻模式:${queryState.mode} ${queryState.workState}")
                    }
                }
            }
        }
    }

    /**打印一下*/
    fun _logEngraveDuration(index: Int, printTimes: Int, reason: String?) {
        val nowTime = nowTime()
        val duration = nowTime - (_engraveTaskEntity?.indexPrintStartTime ?: 0)
        buildString {
            append("雕刻完成[${_engraveTaskId}]")
            append(":${index} ")
            append("第${printTimes}次 ")
            append("耗时:${duration.toMsTime()} ")
            append(reason ?: "")
        }.writeEngraveLog(L.INFO)
    }

    /**开始雕刻*/
    @CallPoint
    fun startEngrave(taskId: String?): EngraveTaskEntity {
        val task = EngraveFlowDataHelper.generateEngraveTask(taskId)
        if (task.dataIndexList.isNullOrEmpty()) {
            //无数据需要雕刻
            "雕刻任务无数据[${taskId}]".writeEngraveLog()
            errorEngrave(EmptyException())
        } else {
            _startEngraveTask(task)
        }
        return task
    }

    /**开始雕刻下一个索引*/
    fun startEngraveNext(taskId: String?) {
        //clear
        _engraveTaskId = taskId

        _engraveTaskEntity?.apply {
            currentIndex = -1
            state = ENGRAVE_STATE_START
            lpSaveEntity()
        }

        buildString {
            append("开始雕刻下一个:[${taskId}]")
        }.writeEngraveLog()
        //loop
        deviceStateModel.startLoopCheckPauseState()
        engraveNext()
    }

    /**完成当前索引的雕刻任务*/
    fun finishCurrentIndexEngrave() {
        isSendEngraveCmd = false
        val task = _engraveTaskEntity ?: return
        val index = task.currentIndex
        buildString {
            append("直接完成雕刻:${_engraveTaskId} ")
            append("索引:${index} ")
        }.writeEngraveLog(L.INFO)

        //更新雕刻进度
        _checkEngraveNextOnIdle(index, null, 100)
    }

    /**所有的索引, 是否都雕刻完成了*/
    fun isAllEngraveFinish(taskId: String?): Boolean {
        val nextIndex = EngraveFlowDataHelper.getNextEngraveIndex(taskId)
        return nextIndex == null
    }

    /**雕刻完成后, 触发下一个雕刻*/
    fun _checkEngraveNextOnIdle(index: Int, printTimes: Int?, progress: Int) {
        deviceStateModel.pauseLoopCheckState(true)

        val nowTime = nowTime()
        UMEvent.ENGRAVE.umengEventValue {
            val duration = nowTime - (_engraveTaskEntity?.startTime ?: 0)
            put(UMEvent.KEY_FINISH_TIME, nowTime.toString())
            put(UMEvent.KEY_DURATION, duration.toString())
        }
        _logEngraveDuration(index, _lastEngraveTimes, "机器空闲状态")
        _lastEngraveTimes = 1
        _lastEngraveIndex = -1
        //强制更新进度到100
        updateEngraveProgress(index, printTimes, progress)

        //完成指定索引的雕刻
        EngraveFlowDataHelper.finishIndexEngrave(_engraveTaskId, index)

        if (isBatchEngraveSupport()) {
            finishEngrave("批量雕刻指令完成")
        } else {
            if (HawkEngraveKeys.enableSingleItemTransfer &&
                !isAllEngraveFinish(_engraveTaskId)
            ) {
                //激活了单文件雕刻, 并且未完成雕刻
                finishIndexEngrave()
            } else {
                engraveNext()
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
    fun _startEngraveTask(task: EngraveTaskEntity) {
        task.apply {
            startTime = nowTime()
            finishTime = -1
            currentIndex = -1
            state = ENGRAVE_STATE_START

            //雕刻参数获取方式
            enableItemEngraveParams = HawkEngraveKeys.enableItemEngraveParams
            lpSaveEntity()

            _engraveTaskId = task.taskId

            //构建所有图层的雕刻参数, 确保有数据
            EngraveFlowDataHelper.generateEngraveConfig(task.taskId)

            //通知开始雕刻
            engraveStateData.postValue(this)

            //loop
            deviceStateModel.startLoopCheckPauseState()

            //
            if (isBatchEngraveSupport()) {
                batchEngrave()
            } else {
                buildString {
                    append("开始雕刻任务:[${taskId}][${task.dataIndexList}] $task")
                }.writeEngraveLog()
                engraveNext()
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
            deviceStateModel.startLoopCheckPauseState()
        }
    }

    /**雕刻下一个*/
    @CallPoint
    fun engraveNext() {
        val task = _engraveTaskEntity ?: return
        val taskId = task.taskId

        if (task.currentIndex > 0) {
            //之前的雕刻索引
            EngraveFlowDataHelper.finishIndexEngrave(taskId, task.currentIndex)
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
                    engraveNext()
                    return
                }
            }

            //雕刻配置数据
            val engraveConfigEntity = if (task.enableItemEngraveParams) {
                EngraveFlowDataHelper.getEngraveConfig(engraveDataEntity.index)
                    ?: EngraveFlowDataHelper.generateEngraveConfig(
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
            task.bigIndex = EngraveHelper.generateEngraveIndex()
            task.lpSaveEntity()
        }

        val indexList = task.dataIndexList?.mapTo(mutableListOf()) {
            it.toIntOrNull() ?: -1
        } ?: emptyList()

        val powerList = mutableListOf<Byte>()
        val depthList = mutableListOf<Byte>()
        val timeList = mutableListOf<Byte>()
        val typeList = mutableListOf<Byte>()

        var precision = 0
        var diameter = 0

        for (index in indexList) {
            EngraveFlowDataHelper.getTransferData(taskId, index)?.let {
                //雕刻参数
                val engraveConfigEntity = if (task.enableItemEngraveParams)
                    EngraveFlowDataHelper.getEngraveConfig(it.index)
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
                }

                //任务雕刻的数据入库
                _generateEngraveData(taskId, index)
            }
        }

        buildString {
            append("开始批量雕刻任务:${taskId} 大索引:${task.bigIndex} 小索引:$indexList")
            append(" power:${powerList}")
            append(" depth:${depthList}")
            append(" time:${timeList}")
            append(" type:${typeList}")
            append(" 加速级别:${precision}")
            append(" 直径:${diameter}")
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
            diameter
        ).enqueue { bean, error ->
            "批量雕刻指令返回:${bean?.parse<MiniReceiveParser>()}".writeEngraveLog(L.WARN)
            _lastEngraveCmdError = error
            if (error == null) {
                //雕刻指令发送成功, 机器开始雕刻
                isSendEngraveCmd = true
                UMEvent.ENGRAVE.umengEventValue {
                    put(UMEvent.KEY_START_TIME, nowTime().toString())
                }
                deviceStateModel.pauseLoopCheckState(false)
            } else if (error is CommandException) {
                //指令异常
                "雕刻失败:[${indexList}] $error".writeErrorLog()
                errorEngrave(error)
            } else {
                //雕刻失败, 重试
                val taskEntity = _engraveTaskEntity
                "雕刻失败:[${indexList}] $error, 即将重试[${retryCount}/${HawkEngraveKeys.engraveRetryCount}]...$taskEntity".writeErrorLog()

                if (taskEntity?.state == ENGRAVE_STATE_START &&
                    fscBleApiModel.haveDeviceConnected()
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

    /**生成任务需要的雕刻数据*/
    fun _generateEngraveData(taskId: String?, index: Int): EngraveDataEntity {
        val engraveDataEntity = EngraveFlowDataHelper.generateEngraveData(taskId, index)
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
        engraveStateData.value = task
        task.lpSaveEntity()
    }

    /**完成雕刻*/
    @CallPoint
    fun finishEngrave(reason: String?) {
        deviceStateModel.pauseLoopCheckState(false)

        isSendEngraveCmd = false
        "完成雕刻[${_engraveTaskId}]:${reason ?: ""}".writeEngraveLog()

        _lastEngraveTimes = 1
        _lastEngraveIndex = -1

        EngraveNotifyHelper.hideEngraveNotify()//隐藏通知

        //
        val engraveTaskEntity = _engraveTaskEntity ?: return
        //clear
        _engraveTaskId = null
        engraveTaskEntity.currentIndex = -1
        engraveTaskEntity.finishTime = nowTime()
        engraveTaskEntity.state = ENGRAVE_STATE_FINISH
        engraveTaskEntity.lpSaveEntity()

        //雕刻次数+1
        HawkEngraveKeys.lastEngraveCount++
        if (HawkEngraveKeys.lastEngraveCount > 10_0000) {
            //超过10W个之后, 清零
            HawkEngraveKeys.lastEngraveCount = 0
        }

        //post
        engraveStateData.postValue(engraveTaskEntity)
    }

    /**完成了一个索引的雕刻, 需要传输下一个索引, 并且雕刻下一个*/
    fun finishIndexEngrave() {
        isSendEngraveCmd = false
        val engraveTaskEntity = _engraveTaskEntity ?: return
        engraveTaskEntity.state = ENGRAVE_STATE_INDEX_FINISH
        engraveTaskEntity.lpSaveEntity()

        //post
        engraveStateData.postValue(engraveTaskEntity)
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
        deviceStateModel.pauseLoopCheckState(true)
        ExitCmd(timeout = HawkEngraveKeys.receiveTimeoutMax).enqueue { bean, error ->
            countDownLatch?.countDown()
            syncQueryDeviceState()
            if (error == null || error is NoDeviceException /*无设备连接*/) {
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
     * [com.angcyo.engrave.model.EngraveModel._lastEngraveCmdError]*/
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
     * [transferDataEntity] 需要雕刻的数据实体
     * [engraveConfigEntity] 雕刻的参数实体
     * */
    fun _startEngraveCmd(
        index: Int,
        transferDataEntity: TransferDataEntity?,
        engraveConfigEntity: EngraveConfigEntity,
        retryCount: Int = 0
    ) {
        val diameter =
            (MM_UNIT.convertPixelToValue(engraveConfigEntity.diameterPixel) * 100).roundToInt()

        val engraveLayer = LayerHelper.getEngraveLayerInfo(engraveConfigEntity.layerId)
        buildString {
            append("开始雕刻指令:[${transferDataEntity?.taskId}][$index]")

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
        EngraveCmd(
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
            engraveConfigEntity.precision
        ).enqueue { bean, error ->
            "雕刻指令返回:${bean?.parse<MiniReceiveParser>()}".writeEngraveLog(L.WARN)
            _lastEngraveCmdError = error
            if (error == null) {
                isSendEngraveCmd = true
                _lastEngraveIndex = index //赋值, 如果机器雕刻过快, 可以不会进入progress状态
                //雕刻指令发送成功, 机器开始雕刻
                UMEvent.ENGRAVE.umengEventValue {
                    put(UMEvent.KEY_START_TIME, nowTime().toString())
                }
                deviceStateModel.pauseLoopCheckState(false)
            } else if (error is CommandException) {
                //指令异常
                "雕刻失败:[${index}] $error".writeErrorLog()
                errorEngrave(error)
            } else {
                //如果索引雕刻异常, 则不能跳过索引雕刻
                "雕刻失败:[${index}] $error, 即将重试[${retryCount}/${HawkEngraveKeys.engraveRetryCount}]...$taskEntity".writeErrorLog()

                if (taskEntity?.state == ENGRAVE_STATE_START &&
                    fscBleApiModel.haveDeviceConnected()
                ) {
                    if (retryCount < HawkEngraveKeys.engraveRetryCount) {
                        doBack {
                            _startEngraveCmd(
                                index,
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
        //使用的模块
        entity.moduleState =
            deviceStateModel.deviceStateData.value?.moduleState ?: entity.moduleState
        //固件版本
        entity.softwareVersion =
            laserPeckerModel.productInfoData.value?.softwareVersion ?: entity.softwareVersion

        //
        entity.deviceAddress = LaserPeckerHelper.lastDeviceAddress() ?: entity.deviceAddress
        entity.productName = laserPeckerModel.productInfoData.value?.name ?: entity.productName
    }

    /**更新雕刻进度和次数
     * [progress] 当前索引的雕刻进度*/
    fun updateEngraveProgress(index: Int, printTimes: Int?, progress: Int) {
        val currentProgress = clamp(progress, 0, 100)
        EngraveFlowDataHelper.updateEngraveProgress(
            _engraveTaskEntity?.taskId,
            index,
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

            "雕刻进度[${it.taskId}]:${index} ${currentProgress}%/${it.progress}% ${printTimes ?: ""}".writeEngraveLog()
        }
    }

    /**是否支持批量文件雕刻, 或者当前处于批量雕刻
     *
     * 打开滑台，打开滑台多文件雕刻开关之后， 走多文件雕刻指令。
     * */
    fun isBatchEngraveSupport(): Boolean {
        /*val setting = laserPeckerModel.deviceSettingData.value ?: return false
        if (setting.sRep == 1) {
            return true
        }*/
        if (HawkEngraveKeys.enableSingleItemTransfer) {
            //单文件传输模式下, 不支持批量文件雕刻
            return false
        }

        if (laserPeckerModel.isL4() || laserPeckerModel.isCSeries()) {
            return true
        }

        //debug
        val version = laserPeckerModel.productInfoData.value?.softwareVersion ?: return false
        return VersionMatcher.matches(version, DeviceHelper.batchEngraveSupportFirmware, false)
    }
}