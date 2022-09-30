package com.angcyo.engrave.model

import androidx.annotation.AnyThread
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.ExitCmd
import com.angcyo.bluetooth.fsc.laserpacker.parse.MiniReceiveParser
import com.angcyo.bluetooth.fsc.parse
import com.angcyo.canvas.data.ItemDataBean.Companion.mmUnit
import com.angcyo.core.component.file.writeErrorLog
import com.angcyo.core.lifecycle.LifecycleViewModel
import com.angcyo.core.vmApp
import com.angcyo.engrave.data.*
import com.angcyo.http.rx.doMain
import com.angcyo.library.L
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.component._delay
import com.angcyo.library.ex.clamp
import com.angcyo.library.ex.nowTime
import com.angcyo.library.ex.size
import com.angcyo.viewmodel.IViewModel
import com.angcyo.viewmodel.vmDataNull
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

    /**雕刻状态数据*/
    val engraveStateData = vmDataNull<EngraveState>()

    //缓存
    var _engraveTask: EngraveTask? = null

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
                                (nowTime - _engraveTask!!.startTime).toString()
                            )
                        }
                        engraveNext()
                    } else if (queryState.isEngraving()) {
                        //雕刻中, 更新对应的雕刻进度
                        val progress = clamp(queryState.rate, 0, 100)
                        _updateEngraveProgress(queryState.printTimes, progress)
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
    fun startEngrave(engraveConfigInfo: EngraveConfigInfo) {
        val task = EngraveTask(engraveConfigInfo)
        task.count = engraveConfigInfo.engraveDataParamList.sumOf { it.dataList.size() }
        _engraveTask = task

        //
        againEngrave()
    }

    /**再雕一次*/
    @CallPoint
    fun againEngrave() {
        _engraveTask?.let {
            it.startTime = nowTime()
            it.current = -1

            engraveNext()

            //loop
            loopCheckDeviceState()
        }
    }

    /**雕刻下一个*/
    @CallPoint
    fun engraveNext() {
        _engraveTask?.let { task ->
            _listenerEngraveState = true
            task.current++
            if (task.current >= task.count) {
                //雕刻完成
                finishEngrave()
            } else {
                val paramAndData = task.engraveConfigInfo.getEngraveParamAndData(task.current)
                if (paramAndData == null) {
                    engraveNext()
                } else {
                    val engraveState = EngraveState(paramAndData, ENGRAVE_STATE_START)
                    doMain {
                        engraveStateData.value = engraveState
                        _updateEngraveProgress(1, 0)
                        _startEngraveCmd(engraveState.engraveDataParam)
                    }
                }
            }
        }
    }

    /**完成雕刻*/
    @CallPoint
    fun finishEngrave() {
        _listenerEngraveState = false
        _engraveTask?.finishTime = nowTime()
        val engraveState = engraveStateData.value ?: return
        engraveState.state = ENGRAVE_STATE_FINISH
        engraveState.progress = 100

        //雕刻次数+1
        HawkEngraveKeys.lastEngraveCount++
        //post
        engraveStateData.postValue(engraveState)
    }

    /**暂停雕刻*/
    fun pauseEngrave() {
        val engraveState = engraveStateData.value ?: return
        engraveState.state = ENGRAVE_STATE_PAUSE
        engraveStateData.postValue(engraveState)
        EngraveCmd.pauseEngrave().enqueue()
    }

    /**继续雕刻*/
    fun continueEngrave() {
        val engraveState = engraveStateData.value ?: return
        engraveState.state = ENGRAVE_STATE_START
        engraveStateData.postValue(engraveState)
        EngraveCmd.continueEngrave().enqueue()
    }

    /**停止雕刻*/
    fun stopEngrave() {
        EngraveCmd.stopEngrave().enqueue()
        ExitCmd().enqueue()
    }

    //---

    /**更新雕刻进度
     * [printTimes] 当前元素的第几次雕刻
     * [progress] 当前的雕刻进度[0~100]
     * */
    @AnyThread
    fun _updateEngraveProgress(printTimes: Int, progress: Int) {
        val engraveState = engraveStateData.value ?: return
        _engraveTask?.let { task ->
            val totalProgress = task.engraveConfigInfo.getTotalProgress()
            val index = engraveState.engraveDataParam.dataList.first().index
            var currentProgress = task.engraveConfigInfo.getBeforeTotalProgress(index)
            currentProgress += (printTimes - 1) * 100 + progress

            //雕刻进度
            val engraveProgress = (currentProgress / totalProgress) * 100
            L.i("当前雕刻总进度:${engraveProgress}")

            engraveState.printTimes = printTimes
            engraveState.progress = engraveProgress

            //记录当前雕刻的次数
            task.engraveConfigInfo.engraveDataParamList.forEach {
                it.dataList.forEach {
                    if (it.index == index) {
                        it.printTimes = printTimes
                    }
                }
            }

            //post
            engraveStateData.postValue(engraveState)
        }
    }

    /**开始雕刻, 发送雕刻指令*/
    fun _startEngraveCmd(engraveDataParam: EngraveDataParam) {
        val dataInfo = engraveDataParam.dataList.first()
        EngraveCmd(
            dataInfo.index,
            engraveDataParam.power.toByte(),
            engraveDataParam.depth.toByte(),
            0x01,
            dataInfo.x,
            dataInfo.y,
            max(1, engraveDataParam.time).toByte(),
            engraveDataParam.type,
            0x09,
            (mmUnit.convertPixelToValue(engraveDataParam.diameterPixel) * 100).roundToInt(),
            engraveDataParam.precision
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

    //雕刻状态
    data class EngraveState(
        /**当前雕刻的参数
         * 当前的[dataList]至多有1条数据
         * */
        var engraveDataParam: EngraveDataParam,
        /**
         * 雕刻的状态
         * [ENGRAVE_STATE_START]
         * [ENGRAVE_STATE_PAUSE]
         * [ENGRAVE_STATE_FINISH]
         * */
        var state: Int = 0,

        /**第几次雕刻*/
        var printTimes: Int = 1,

        /**当前的雕刻, 在整个任务中的进度*/
        var progress: Int = 0,
    )

    //待雕刻的任务
    data class EngraveTask(

        /**雕刻的数据和参数*/
        val engraveConfigInfo: EngraveConfigInfo,

        /**任务开始的时间, 毫秒*/
        var startTime: Long = nowTime(),

        /**任务完成的时间, 毫秒*/
        var finishTime: Long = nowTime(),

        /**当前雕刻的序列*/
        var current: Int = 0,

        /**总共雕刻的数据数量*/
        var count: Int = 0,
    )

}