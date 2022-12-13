package com.angcyo.engrave.model

import com.angcyo.bluetooth.fsc.FscBleApiModel
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.ExitCmd
import com.angcyo.bluetooth.fsc.laserpacker.parse.MiniReceiveParser
import com.angcyo.bluetooth.fsc.laserpacker.parse.QueryStateParser
import com.angcyo.bluetooth.fsc.laserpacker.syncQueryDeviceState
import com.angcyo.bluetooth.fsc.laserpacker.writeEngraveLog
import com.angcyo.bluetooth.fsc.parse
import com.angcyo.canvas.data.CanvasProjectItemBean.Companion.MM_UNIT
import com.angcyo.canvas.utils.toDataModeStr
import com.angcyo.core.component.file.writeErrorLog
import com.angcyo.core.lifecycle.LifecycleViewModel
import com.angcyo.core.vmApp
import com.angcyo.engrave.EngraveFlowDataHelper
import com.angcyo.engrave.EngraveNotifyHelper
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.engrave.toLaserTypeString
import com.angcyo.engrave.transition.EngraveTransitionManager
import com.angcyo.http.rx.doBack
import com.angcyo.http.rx.doMain
import com.angcyo.library.L
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.annotation.Private
import com.angcyo.library.component.VersionMatcher
import com.angcyo.library.component._delay
import com.angcyo.library.ex.clamp
import com.angcyo.library.ex.nowTime
import com.angcyo.library.ex.toMsTime
import com.angcyo.library.getAppString
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.EngraveDataEntity
import com.angcyo.objectbox.laser.pecker.entity.EngraveTaskEntity
import com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity
import com.angcyo.objectbox.laser.pecker.lpSaveEntity
import com.angcyo.viewmodel.IViewModel
import com.angcyo.viewmodel.vmDataOnce
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue
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

        /**雕刻状态: 已完成*/
        const val ENGRAVE_STATE_FINISH = 3

        /**最后一次雕刻的次数*/
        var _lastEngraveTimes: Int = 1

        /**多文件雕刻指令, 上一次雕刻的索引*/
        var _lastEngraveIndex: Int = -1
    }

    val laserPeckerModel = vmApp<LaserPeckerModel>()

    val fscBleApiModel = vmApp<FscBleApiModel>()

    /**雕刻状态通知*/
    val engraveStateData = vmDataOnce<EngraveTaskEntity>()

    //缓存
    var _engraveTaskId: String? = null

    //最后一次雕刻指令发送出去, 是否有异常
    var _lastEngraveCmdError: Throwable? = null

    /**必须要实时获取, 否则在其他地方修改了数据, 在这里还是缓存, 不会是最新的*/
    val _engraveTaskEntity: EngraveTaskEntity?
        get() = EngraveFlowDataHelper.getEngraveTask(_engraveTaskId)

    //是否要监听设备的雕刻状态
    var _listenerEngraveState: Boolean = false

    init {
        //监听雕刻状态
        laserPeckerModel.deviceStateData.observe(this) { queryState ->
            queryState?.let {
                if (_listenerEngraveState && _engraveTaskId != null) {
                    //有任务在执行
                    if (queryState.isModeIdle()) {
                        if (_engraveTaskEntity?.state != ENGRAVE_STATE_FINISH) {
                            //机器空闲了, 可能一个数据雕刻结束了
                            if (_lastEngraveCmdError == null) {
                                val nowTime = nowTime()
                                UMEvent.ENGRAVE.umengEventValue {
                                    val duration = nowTime - (_engraveTaskEntity?.startTime ?: 0)
                                    put(UMEvent.KEY_FINISH_TIME, nowTime.toString())
                                    put(UMEvent.KEY_DURATION, duration.toString())
                                }
                                _logEngraveDuration(queryState, _lastEngraveTimes)
                                _lastEngraveTimes = 1
                                _lastEngraveIndex = -1
                                //强制更新进度到100
                                updateEngraveProgress(queryState, 100)

                                if (isBatchEngraveSupport()) {
                                    finishEngrave()
                                } else {
                                    engraveNext()
                                }
                            }
                        }
                    } else if (queryState.isEngraving()) {
                        //雕刻中, 更新对应的雕刻进度
                        if (_lastEngraveIndex != queryState.index) {
                            _startEngraveIndex(queryState.index)
                            if (_lastEngraveIndex > 0) {
                                EngraveFlowDataHelper.finishEngrave(
                                    _engraveTaskId,
                                    _lastEngraveIndex
                                )
                            }
                        }

                        //多文件雕刻的小索引
                        _lastEngraveIndex = queryState.index

                        if (_lastEngraveTimes != queryState.printTimes) {
                            _logEngraveDuration(queryState, _lastEngraveTimes)
                            _lastEngraveTimes = queryState.printTimes
                            _engraveTaskEntity?.apply {
                                indexPrintStartTime = nowTime()
                                lpSaveEntity()
                            }
                        }
                        updateEngraveProgress(queryState, queryState.rate)
                    } else if (queryState.isEngravePause()) {
                        //
                    } else {
                        //
                        L.w("未处理的雕刻模式:${queryState.mode} ${queryState.workState}")
                    }
                }
            }
        }
    }

    /**打印一下*/
    fun _logEngraveDuration(queryState: QueryStateParser, printTimes: Int) {
        val nowTime = nowTime()
        val duration = nowTime - (_engraveTaskEntity?.indexPrintStartTime ?: 0)
        buildString {
            append("雕刻完成:${_engraveTaskId} ")
            append("索引:${queryState.index} ")
            append("第${printTimes}次 ")
            append("耗时:${duration.toMsTime()}")
        }.writeEngraveLog(L.INFO)
    }

    /**开始雕刻*/
    @CallPoint
    fun startEngrave(taskId: String?): EngraveTaskEntity {
        val task = EngraveFlowDataHelper.generateEngraveTask(taskId)
        if (task.dataIndexList.isNullOrEmpty()) {
            //无数据需要雕刻
        } else {
            _startEngraveTask(task)
        }
        return task
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
            lpSaveEntity()

            _engraveTaskId = task.taskId

            //构建所有图层的雕刻参数, 确保有数据
            EngraveFlowDataHelper.generateEngraveConfig(task.taskId)

            //
            if (isBatchEngraveSupport()) {
                batchEngrave()
            } else {
                buildString {
                    append("开始雕刻任务:[${taskId}][${task.dataIndexList}] $task")
                }.writeEngraveLog()
                engraveNext()
            }

            //loop
            loopCheckDeviceState()
        }
    }

    /**恢复雕刻任务*/
    @CallPoint
    fun restoreEngrave(taskId: String?) {
        val task = EngraveFlowDataHelper.getEngraveTask(taskId)
        task?.apply {
            _engraveTaskId = task.taskId
            _listenerEngraveState = true
            //loop
            loopCheckDeviceState()
        }
    }

    /**雕刻下一个*/
    @CallPoint
    fun engraveNext() {
        val task = _engraveTaskEntity ?: return
        _listenerEngraveState = true
        val taskId = task.taskId

        if (task.currentIndex > 0) {
            //之前的雕刻索引
            EngraveFlowDataHelper.finishEngrave(taskId, task.currentIndex)
        }

        //查找下一个未完成雕刻的索引
        val nextIndex = EngraveFlowDataHelper.getNextEngraveIndex(taskId)

        if (nextIndex == null) {
            //雕刻完成
            finishEngrave()
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
            val engraveConfigEntity = EngraveFlowDataHelper.generateEngraveConfig(
                taskId,
                transferDataEntity?.layerMode ?: 0
            )
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
        _listenerEngraveState = true
        val taskId = task.taskId

        if (task.bigIndex == null) {
            task.bigIndex = EngraveTransitionManager.generateEngraveIndex()
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
                EngraveFlowDataHelper.getEngraveConfig(taskId, it.layerMode)
                    ?.let { engraveConfigEntity ->
                        precision = engraveConfigEntity.precision
                        diameter =
                            (MM_UNIT.convertPixelToValue(engraveConfigEntity.diameterPixel) * 100).roundToInt()

                        powerList.add(engraveConfigEntity.power.toByte())
                        depthList.add(engraveConfigEntity.depth.toByte())
                        timeList.add(engraveConfigEntity.time.toByte())
                        typeList.add(engraveConfigEntity.type)

                        //保存外接设备名
                        engraveConfigEntity.exDevice = laserPeckerModel.getExDevice()
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
                UMEvent.ENGRAVE.umengEventValue {
                    put(UMEvent.KEY_START_TIME, nowTime().toString())
                }
            } else {
                //雕刻失败, 重试
                val taskEntity = _engraveTaskEntity
                "雕刻失败:[${indexList}] $error, 即将重试...$taskEntity".writeErrorLog()

                if (taskEntity?.state == ENGRAVE_STATE_START &&
                    fscBleApiModel.haveDeviceConnected()
                ) {
                    if (retryCount < HawkEngraveKeys.engraveRetryCount) {
                        doBack {
                            batchEngrave(retryCount + 1)
                        }
                    }
                }
            }
        }

        doMain {
            task.currentIndex = indexList.firstOrNull() ?: -1
            task.indexStartTime = nowTime()
            task.indexPrintStartTime = task.indexStartTime
            engraveStateData.value = task
            task.lpSaveEntity()
        }
    }

    /**生成任务需要的雕刻数据*/
    fun _generateEngraveData(taskId: String?, index: Int): EngraveDataEntity {
        val engraveDataEntity =
            EngraveFlowDataHelper.generateEngraveData(taskId, index)
        engraveDataEntity.startTime = nowTime()
        engraveDataEntity.lpSaveEntity()
        return engraveDataEntity
    }

    /**开始雕刻指定的索引*/
    fun _startEngraveIndex(index: Int) {
        val task = _engraveTaskEntity ?: return
        task.currentIndex = index
        task.indexStartTime = nowTime()
        task.indexPrintStartTime = task.indexStartTime
        engraveStateData.value = task
        task.lpSaveEntity()
    }

    /**完成雕刻*/
    @CallPoint
    fun finishEngrave() {
        _lastEngraveTimes = 1
        _lastEngraveIndex = -1
        _listenerEngraveState = false

        EngraveNotifyHelper.hideEngraveNotify()//隐藏通知

        //
        val engraveTaskEntity = _engraveTaskEntity ?: return
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

        //clear
        _engraveTaskId = null

        //更新设备状态
        _delay(HawkEngraveKeys.minQueryDelayTime) {
            syncQueryDeviceState { bean, error ->
                //no op
            }
        }
    }

    /**暂停雕刻*/
    fun pauseEngrave() {
        val engraveState = _engraveTaskEntity ?: return
        engraveState.state = ENGRAVE_STATE_PAUSE
        engraveState.lpSaveEntity()
        engraveStateData.postValue(engraveState)
        EngraveCmd.pauseEngrave().enqueue()
    }

    /**继续雕刻*/
    fun continueEngrave() {
        val engraveState = _engraveTaskEntity ?: return
        engraveState.state = ENGRAVE_STATE_START
        engraveState.lpSaveEntity()
        engraveStateData.postValue(engraveState)
        EngraveCmd.continueEngrave().enqueue()
    }

    /**停止雕刻*/
    fun stopEngrave() {
        EngraveCmd.stopEngrave().enqueue()
        finishEngrave()
        ExitCmd().enqueue()
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

        val engraveLayer = EngraveTransitionManager.getEngraveLayer(engraveConfigEntity.layerMode)
        buildString {
            append("开始雕刻指令:[${transferDataEntity?.taskId}][$index]")
            if (engraveLayer?.label.isNullOrBlank()) {
                append(" mode:${engraveConfigEntity.layerMode.toDataModeStr()}")
            } else {
                append(" layer:${engraveLayer?.label}")
            }

            append(" type:${engraveConfigEntity.type.toLaserTypeString()}")
            transferDataEntity?.let {
                append(" $it")
            }
            append("\n->$engraveConfigEntity")
        }.writeEngraveLog()

        //保存外接设备名
        engraveConfigEntity.exDevice = laserPeckerModel.getExDevice()
        engraveConfigEntity.lpSaveEntity()

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
                //雕刻指令发送成功, 机器开始雕刻
                UMEvent.ENGRAVE.umengEventValue {
                    put(UMEvent.KEY_START_TIME, nowTime().toString())
                }
            } else {
                //如果索引雕刻异常, 则不能跳过索引雕刻
                val taskEntity = _engraveTaskEntity
                "雕刻失败:[${index}] $error, 即将重试...$taskEntity".writeErrorLog()

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
                    }
                }
            }
        }
    }

    /**更新雕刻进度和次数
     * [progress] 当前索引的雕刻进度*/
    fun updateEngraveProgress(queryState: QueryStateParser, progress: Int) {
        _engraveTaskEntity?.apply {
            val engraveProgress = EngraveFlowDataHelper.calcEngraveProgress(taskId)
            EngraveNotifyHelper.showEngraveNotify(engraveProgress)//显示通知
        }

        val currentProgress = clamp(progress, 0, 100)
        EngraveFlowDataHelper.updateEngraveProgress(
            _engraveTaskEntity?.taskId,
            queryState.index,
            queryState.printTimes,
            currentProgress
        )
        _engraveTaskEntity?.let {
            it.currentProgress = currentProgress
            it.progress = EngraveFlowDataHelper.calcEngraveProgress(it.taskId)
            it.lpSaveEntity()
            engraveStateData.postValue(it)
        }
    }

    /**持续检查工作作态*/
    fun loopCheckDeviceState() {
        _delay(HawkEngraveKeys.minQueryDelayTime) {
            //延迟1秒后, 继续查询状态
            laserPeckerModel.queryDeviceState() { bean, error ->
                if (error != null || _listenerEngraveState) {
                    //出现了错误, 继续查询
                    loopCheckDeviceState()
                }
            }
        }
    }

    /**是否支持批量文件雕刻, 或者当前处于批量雕刻
     *
     * 打开滑台，打开滑台多文件雕刻开关之后， 走多文件雕刻指令。
     * */
    fun isBatchEngraveSupport(): Boolean {
        val setting = laserPeckerModel.deviceSettingData.value ?: return false
        if (setting.sRep == 1) {
            return true
        }

        //debug
        val version = laserPeckerModel.productInfoData.value?.softwareVersion ?: return false
        val batchEngraveSupportFirmware = getAppString("lp_batch_engrave_firmware")
        if (VersionMatcher.matches(version, batchEngraveSupportFirmware, false)) {
            return true
        }
        return VersionMatcher.matches(version, HawkEngraveKeys.batchEngraveSupportFirmware, false)
    }
}