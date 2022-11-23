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
import com.angcyo.library.component._delay
import com.angcyo.library.ex.clamp
import com.angcyo.library.ex.nowTime
import com.angcyo.library.ex.toMsTime
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.EngraveTaskEntity
import com.angcyo.objectbox.laser.pecker.entity.PreviewConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity
import com.angcyo.objectbox.laser.pecker.lpSaveEntity
import com.angcyo.viewmodel.IViewModel
import com.angcyo.viewmodel.vmDataOnce
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 雕刻数据存储
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
                if (_listenerEngraveState) {
                    //有任务在执行
                    if (queryState.isModeIdle()) {
                        //机器空闲了, 可能一个数据雕刻结束了
                        val nowTime = nowTime()
                        UMEvent.ENGRAVE.umengEventValue {
                            val duration = nowTime - (_engraveTaskEntity?.startTime ?: 0)
                            put(UMEvent.KEY_FINISH_TIME, nowTime.toString())
                            put(UMEvent.KEY_DURATION, duration.toString())
                        }
                        val duration = nowTime - (_engraveTaskEntity?.indexStartTime ?: 0)
                        buildString {
                            append("雕刻完成:${_engraveTaskId} ")
                            append("index:${queryState.index} ")
                            append("第${queryState.printTimes}次 ")
                            append("耗时:${duration.toMsTime()}")
                        }.writeEngraveLog(L.INFO)
                        //强制更新进度到100
                        updateEngraveProgress(queryState, 100)
                        engraveNext()
                    } else if (queryState.isEngraving()) {
                        //雕刻中, 更新对应的雕刻进度
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

    /**开始雕刻*/
    @CallPoint
    fun startEngrave(taskId: String?) {
        val task = EngraveFlowDataHelper.generateEngraveTask(taskId)
        _startEngraveTask(task)
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

            //
            engraveNext()

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
        _engraveTaskEntity?.let { task ->
            _listenerEngraveState = true
            val taskId = task.taskId

            if (task.currentIndex > 0) {
                //之前的雕刻索引
                EngraveFlowDataHelper.finishEngrave(task.currentIndex)
            }

            val nextIndex = EngraveFlowDataHelper.getNextEngraveIndex(taskId)

            if (nextIndex == null) {
                //雕刻完成
                finishEngrave()
            } else {
                //开始雕刻
                val engraveDataEntity =
                    EngraveFlowDataHelper.generateEngraveData(taskId, nextIndex)
                engraveDataEntity.printTimes = 1
                engraveDataEntity.progress = 0
                engraveDataEntity.startTime = nowTime()
                engraveDataEntity.finishTime = -1
                engraveDataEntity.lpSaveEntity()

                val transferDataEntity = EngraveFlowDataHelper.getTransferData(taskId, nextIndex)
                if (transferDataEntity == null) {
                    //需要雕刻的数据不存在,则直接完成
                    engraveDataEntity.progress = 100
                    engraveDataEntity.lpSaveEntity()
                    engraveNext()
                } else {
                    //雕刻配置数据
                    val previewConfigEntity = EngraveFlowDataHelper.generatePreviewConfig(taskId)
                    val engraveConfigEntity = EngraveFlowDataHelper.generateEngraveConfig(
                        taskId,
                        transferDataEntity.layerMode
                    )
                    doMain {
                        task.currentIndex = nextIndex
                        task.indexStartTime = nowTime()
                        engraveStateData.value = task
                        task.lpSaveEntity()

                        _startEngraveCmd(
                            previewConfigEntity,
                            transferDataEntity,
                            engraveConfigEntity
                        )
                    }
                }
            }
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
     * [transferDataEntity] 需要雕刻的数据实体
     * [engraveConfigEntity] 雕刻的参数实体
     * */
    fun _startEngraveCmd(
        previewConfigEntity: PreviewConfigEntity,
        transferDataEntity: TransferDataEntity,
        engraveConfigEntity: EngraveConfigEntity
    ) {
        val diameter =
            (MM_UNIT.convertPixelToValue(previewConfigEntity.diameterPixel) * 100).roundToInt()

        val engraveLayer = EngraveTransitionManager.getEngraveLayer(engraveConfigEntity.layerMode)
        buildString {
            append("开始雕刻:[${transferDataEntity.taskId}]")
            if (engraveLayer?.label.isNullOrBlank()) {
                append(" mode:${engraveConfigEntity.layerMode.toDataModeStr()}")
            } else {
                append(" layer:${engraveLayer?.label}")
            }

            append(" type:${engraveConfigEntity.type.toLaserTypeString()}")
            append(" $transferDataEntity")
            append("\n->$engraveConfigEntity")
        }.writeEngraveLog()

        EngraveCmd(
            transferDataEntity.index,
            engraveConfigEntity.power.toByte(),
            engraveConfigEntity.depth.toByte(),
            0x01,
            transferDataEntity.x,
            transferDataEntity.y,
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
        _delay(1_000) {
            //延迟1秒后, 继续查询状态
            laserPeckerModel.queryDeviceState() { bean, error ->
                if (error != null || _listenerEngraveState) {
                    //出现了错误, 继续查询
                    loopCheckDeviceState()
                }
            }
        }
    }
}