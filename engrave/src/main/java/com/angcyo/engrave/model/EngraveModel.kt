package com.angcyo.engrave.model

import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.ExitCmd
import com.angcyo.bluetooth.fsc.laserpacker.parse.MiniReceiveParser
import com.angcyo.bluetooth.fsc.laserpacker.parse.QueryStateParser
import com.angcyo.bluetooth.fsc.laserpacker.writeEngraveLog
import com.angcyo.bluetooth.fsc.parse
import com.angcyo.canvas.data.CanvasProjectItemBean.Companion.MM_UNIT
import com.angcyo.canvas.utils.toDataModeStr
import com.angcyo.core.component.file.writeErrorLog
import com.angcyo.core.lifecycle.LifecycleViewModel
import com.angcyo.core.vmApp
import com.angcyo.engrave.EngraveFlowDataHelper
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.engrave.toLaserTypeString
import com.angcyo.engrave.transition.EngraveTransitionManager
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
    }

    val laserPeckerModel = vmApp<LaserPeckerModel>()

    /**雕刻状态通知*/
    val engraveStateData = vmDataOnce<EngraveTaskEntity>()

    //缓存
    var _engraveTaskId: String? = null

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
                        //机器空闲了, 可能一个数据雕刻结束了
                        val nowTime = nowTime()
                        UMEvent.ENGRAVE.umengEventValue {
                            val duration = nowTime - (_engraveTaskEntity?.startTime ?: 0)
                            put(UMEvent.KEY_FINISH_TIME, nowTime.toString())
                            put(UMEvent.KEY_DURATION, duration.toString())
                        }
                        _logEngraveDuration(queryState, _lastEngraveTimes)
                        _lastEngraveTimes = 1
                        //强制更新进度到100
                        updateEngraveProgress(queryState, 100)
                        if (isBatchEngraveSupport()) {
                            finishEngrave()
                        } else {
                            engraveNext()
                        }
                    } else if (queryState.isEngraving()) {
                        //雕刻中, 更新对应的雕刻进度
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
            val engraveDataEntity =
                EngraveFlowDataHelper.generateEngraveData(taskId, nextIndex)
            engraveDataEntity.startTime = nowTime()
            engraveDataEntity.lpSaveEntity()

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
                task.currentIndex = nextIndex
                task.indexStartTime = nowTime()
                task.indexPrintStartTime = task.indexStartTime
                engraveStateData.value = task
                task.lpSaveEntity()

                //
                _startEngraveCmd(
                    engraveDataEntity.index,
                    transferDataEntity,
                    engraveConfigEntity
                )
            }
        }
    }

    /**批量雕刻*/
    @CallPoint
    fun batchEngrave() {
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

        var type: Byte = 0
        var precision = 0
        var diameter = 0

        for (index in indexList) {
            EngraveFlowDataHelper.getTransferData(taskId, index)?.let {
                EngraveFlowDataHelper.getEngraveConfig(taskId, it.layerMode)
                    ?.let { engraveConfigEntity ->
                        precision = engraveConfigEntity.precision
                        type = engraveConfigEntity.type
                        diameter =
                            (MM_UNIT.convertPixelToValue(engraveConfigEntity.diameterPixel) * 100).roundToInt()

                        powerList.add(engraveConfigEntity.power.toByte())
                        depthList.add(engraveConfigEntity.depth.toByte())
                        timeList.add(engraveConfigEntity.time.toByte())

                        //保存外接设备名
                        engraveConfigEntity.exDevice = laserPeckerModel.getExDevice()
                        engraveConfigEntity.lpSaveEntity()
                    }
            }
        }

        buildString {
            append("开始批量雕刻任务:${taskId} [${task.bigIndex}] $indexList")
            append(" type:${type.toLaserTypeString()}")
            append(" 加速级别:${precision}")
            append(" 直径:${diameter}")
        }.writeEngraveLog()

        EngraveCmd.batchEngrave(
            task.bigIndex!!,
            indexList,
            powerList,
            depthList,
            timeList,
            type,
            precision,
            diameter
        ).enqueue { bean, error ->
            L.w("雕刻返回:${bean?.parse<MiniReceiveParser>()}")

            if (error == null) {
                //雕刻指令发送成功, 机器开始雕刻
                UMEvent.ENGRAVE.umengEventValue {
                    put(UMEvent.KEY_START_TIME, nowTime().toString())
                }
            } else {
                "雕刻失败:$error".writeErrorLog()
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

    /**完成雕刻*/
    @CallPoint
    fun finishEngrave() {
        _listenerEngraveState = false
        val engraveTaskEntity = _engraveTaskEntity ?: return
        engraveTaskEntity.currentIndex = -1
        engraveTaskEntity.finishTime = nowTime()
        engraveTaskEntity.state = ENGRAVE_STATE_FINISH
        engraveTaskEntity.lpSaveEntity()

        //雕刻次数+1
        HawkEngraveKeys.lastEngraveCount++

        //post
        engraveStateData.postValue(engraveTaskEntity)

        //clear
        _engraveTaskId = null
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
        finishEngrave()
        EngraveCmd.stopEngrave().enqueue()
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
        engraveConfigEntity: EngraveConfigEntity
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
            L.w("雕刻返回:${bean?.parse<MiniReceiveParser>()}")

            if (error == null) {
                //雕刻指令发送成功, 机器开始雕刻
                UMEvent.ENGRAVE.umengEventValue {
                    put(UMEvent.KEY_START_TIME, nowTime().toString())
                }
            } else {
                "雕刻失败:$error".writeErrorLog()
            }
        }
    }

    /**更新雕刻进度和次数
     * [progress] 当前索引的雕刻进度*/
    fun updateEngraveProgress(queryState: QueryStateParser, progress: Int) {
        EngraveFlowDataHelper.updateEngraveProgress(
            _engraveTaskEntity?.taskId,
            queryState.index,
            queryState.printTimes,
            clamp(progress, 0, 100)
        )
        _engraveTaskEntity?.let {
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

    /**是否支持批量文件雕刻, 或者当前处于批量雕刻*/
    fun isBatchEngraveSupport(): Boolean {
        val version = laserPeckerModel.productInfoData.value?.softwareVersion ?: return false
        val batchEngraveSupportFirmware = getAppString("lp_batch_engrave_firmware")
        if (VersionMatcher.matches(version, batchEngraveSupportFirmware, false)) {
            return true
        }
        return VersionMatcher.matches(version, HawkEngraveKeys.batchEngraveSupportFirmware, false)
    }
}