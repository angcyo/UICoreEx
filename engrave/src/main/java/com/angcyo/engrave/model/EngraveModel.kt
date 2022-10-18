package com.angcyo.engrave.model

import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.ExitCmd
import com.angcyo.bluetooth.fsc.laserpacker.parse.MiniReceiveParser
import com.angcyo.bluetooth.fsc.laserpacker.writeEngraveLog
import com.angcyo.bluetooth.fsc.parse
import com.angcyo.canvas.data.ItemDataBean.Companion.MM_UNIT
import com.angcyo.core.component.file.writeErrorLog
import com.angcyo.core.lifecycle.LifecycleViewModel
import com.angcyo.core.vmApp
import com.angcyo.engrave.EngraveFlowDataHelper
import com.angcyo.engrave.data.*
import com.angcyo.engrave.toLaserTypeString
import com.angcyo.http.rx.doMain
import com.angcyo.library.L
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.annotation.Private
import com.angcyo.library.component._delay
import com.angcyo.library.ex.clamp
import com.angcyo.library.ex.nowTime
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
    var _engraveTaskEntity: EngraveTaskEntity? = null

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
                        UMEvent.ENGRAVE.umengEventValue {
                            val nowTime = nowTime()
                            put(UMEvent.KEY_FINISH_TIME, nowTime.toString())
                            put(
                                UMEvent.KEY_DURATION,
                                (nowTime - (_engraveTaskEntity?.startTime ?: 0)).toString()
                            )
                        }
                        engraveNext()
                    } else if (queryState.isEngraving()) {
                        //雕刻中, 更新对应的雕刻进度
                        val progress = clamp(queryState.rate, 0, 100)
                        EngraveFlowDataHelper.updateEngraveProgress(
                            queryState.index,
                            queryState.printTimes,
                            progress
                        )
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
        task.let {
            it.startTime = nowTime()
            it.finishTime = -1
            it.currentIndex = -1
            it.state = ENGRAVE_STATE_START
            it.lpSaveEntity()

            _engraveTaskEntity = task

            //
            engraveNext()

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
        buildString {
            append("开始雕刻:[${transferDataEntity.taskId}]")
            append(" ${transferDataEntity.index}")
            append(" type:${engraveConfigEntity.type.toLaserTypeString()}")
            append(" power:${engraveConfigEntity.power}")
            append(" depth:${engraveConfigEntity.depth}")
            append(" time:${engraveConfigEntity.time}")
            append(" diameter:${diameter}")
            append(" precision:${engraveConfigEntity.precision}")
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

/*
    */
    /**当前选中的雕刻参数*//*
    val engraveOptionInfoData = vmData(
        EngraveOptionInfo(
            _string(R.string.material_custom),
            HawkEngraveKeys.lastPower.toByte(),
            HawkEngraveKeys.lastDepth.toByte(),
            1,
            diameterPixel = HawkEngraveKeys.lastDiameterPixel
        )
    )

    */
    /**当前正在雕刻的数据*//*
    val engraveReadyInfoData = vmDataNull<EngraveReadyInfo>()

    */
    /**用来通知item的雕刻进度*//*
    val engraveItemData = vmDataOnce<EngraveItemInfo>()

    */
    /**用来通知正在预览的item, 然后从[engravePreviewInfoData]中获取相应的数据*//*
    val engraveItemInfoData = vmDataOnce<EngraveItemInfo>()

    */
    /**设置需要雕刻的数据*//*
    @AnyThread
    fun setEngraveReadyDataInfo(info: EngraveReadyInfo) {
        if (isMain()) {
            engraveReadyInfoData.value = info
        } else {
            engraveReadyInfoData.postValue(info)
        }
    }

    */
    /**更新雕刻数据信息*//*
    @AnyThread
    fun updateEngraveReadyDataInfo(block: EngraveReadyInfo.() -> Unit) {
        engraveReadyInfoData.value?.let {
            it.block()
            setEngraveReadyDataInfo(it)
        }
    }

    */
    /**开始雕刻*//*
    fun startEngrave() {
        engraveReadyInfoData.value?.apply {
            printTimes = 0
            startEngraveTime = nowTime()

            val laserPeckerModel = vmApp<LaserPeckerModel>()

            var history: EngraveHistoryEntity? = historyEntity
            if (history == null) {
                lpBoxOf(EngraveHistoryEntity::class) {
                    history = findLast(
                        1,
                        EngraveHistoryEntity_.index.equal(engraveData?.index ?: -1)
                    ).lastOrNull() ?: EngraveHistoryEntity()
                }
            }
            history?.let { entity ->
                //入库
                engraveData?.updateToEntity(entity)
                engraveOptionInfoData.value?.updateToEntity(entity)

                entity.dataMode = dataMode
                entity.dataPath = dataPath
                entity.previewDataPath = previewDataPath
                entity.startEngraveTime = startEngraveTime
                entity.printTimes = printTimes

                entity.productVersion =
                    laserPeckerModel.productInfoData.value?.version ?: entity.productVersion

                if (laserPeckerModel.isZOpen()) {
                    //z轴模式
                    entity.zMode = QuerySettingParser.Z_MODEL
                }

                entity.saveEntity(LPBox.PACKAGE_NAME)
            }

            //hold
            historyEntity = history

            //progress
            updateEngraveProgress(0)
        }
    }

    */

    /**更新打印次数*//*
    fun updatePrintTimes(times: Int) {
        engraveReadyInfoData.value?.apply {
            printTimes = times
            setEngraveReadyDataInfo(this)

            historyEntity?.let { entity ->
                entity.printTimes = times
                entity.saveEntity(LPBox.PACKAGE_NAME)
            }
        }
    }

    */
    /**计算雕刻剩余时间, 毫秒
     * [rate] 打印进度百分比[0-100]*//*
    fun calcEngraveRemainingTime(rate: Int): Long {
        if (rate <= 0) {
            return -1
        } else if (rate >= 100) {
            return 0
        }

        val startEngraveTime = engraveReadyInfoData.value?.startEngraveTime ?: -1
        if (startEngraveTime <= 0) {
            return -1
        }

        val time = nowTime() - startEngraveTime
        val speed = rate * 1f / time

        val sum = 100 - rate
        if (sum <= 0) {
            return -1
        }
        return (sum / speed).roundToLong()
    }

    */
    /**更新雕刻进度
     * [progress] 0~100*//*
    @AnyThread
    fun updateEngraveProgress(progress: Int) {
        engraveReadyInfoData.value?.let {
            val uuid = if (progress < 0) null else it.itemUuid
            engraveItemData.postValue(EngraveItemInfo(uuid, progress))
        }
    }

    */
    /**通知正在预览的item*//*
    @AnyThread
    fun updateEngravePreviewUuid(uuid: String?) {
        engraveItemInfoData.postValue(
            EngraveItemInfo(uuid, engraveItemInfoData.value?.progress ?: -1)
        )
    }

    */
    /**恢复的状态*//*
    fun isRestore() = engraveReadyInfoData.value == null*/
}