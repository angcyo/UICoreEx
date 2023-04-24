package com.angcyo.bluetooth.fsc.laserpacker

import androidx.annotation.AnyThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.angcyo.bluetooth.fsc.CommandQueueHelper
import com.angcyo.bluetooth.fsc.IReceiveBeanAction
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.command.ExitCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.QueryCmd
import com.angcyo.bluetooth.fsc.laserpacker.parse.QueryLogParser
import com.angcyo.bluetooth.fsc.laserpacker.parse.QueryStateParser
import com.angcyo.bluetooth.fsc.laserpacker.parse.toErrorStateString
import com.angcyo.bluetooth.fsc.parse
import com.angcyo.core.component.file.writeErrorLog
import com.angcyo.core.vmApp
import com.angcyo.http.rx.doMain
import com.angcyo.library.L
import com.angcyo.library.component._delay
import com.angcyo.library.ex.add
import com.angcyo.library.ex.have
import com.angcyo.library.ex.remove
import com.angcyo.library.toastQQ
import com.angcyo.viewmodel.MutableHoldLiveData
import com.angcyo.viewmodel.updateValue
import com.angcyo.viewmodel.vmData
import com.angcyo.viewmodel.vmHoldDataNull

/**
 * 设备状态轮询管理
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/04/13
 */
class DeviceStateModel : ViewModel() {

    companion object {
        /**轮询模式*/
        const val QUERY_MODE_LOOP = 0x01

        /**暂停模式*/
        const val QUERY_MODE_PAUSE = QUERY_MODE_LOOP shl 1
    }

    /**查询模式, 是否是轮询*/
    private var queryMode: Int = 0x0

    val laserPeckerModel = vmApp<LaserPeckerModel>()

    /**设备状态,蓝牙断开后,清空设备状态
     * [com.angcyo.laserpacker.device.model.FscDeviceModel.initDevice]
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.sendInitCommand]
     * */
    val deviceStateData: MutableHoldLiveData<QueryStateParser?> = vmHoldDataNull()

    /**设备状态切换记录*/
    val deviceStateStackData = vmData(mutableListOf<QueryStateParser>())

    /**当前设备的模式, 用来记录设备上一次的工作模式
     * [com.angcyo.bluetooth.fsc.laserpacker.parse.QueryStateParser.WORK_MODE_IDLE]
     * [com.angcyo.bluetooth.fsc.laserpacker.parse.QueryStateParser.WORK_MODE_ENGRAVE]
     * [com.angcyo.bluetooth.fsc.laserpacker.parse.QueryStateParser.WORK_MODE_ENGRAVE_PREVIEW]*/
    val deviceModelData: MutableLiveData<Int> = vmData(QueryStateParser.WORK_MODE_IDLE)

    /**是否需要轮询*/
    private val needLoop: Boolean
        get() = queryMode == QUERY_MODE_LOOP

    /**有轮询模式*/
    val isLoop: Boolean
        get() = queryMode.have(QUERY_MODE_LOOP)

    /**有暂停模式*/
    val isPause: Boolean
        get() = queryMode.have(QUERY_MODE_PAUSE)

    private var isLooping = false

    /**开始循环并暂停*/
    fun startLoopCheckPauseState() {
        queryMode = queryMode.add(QUERY_MODE_LOOP).add(QUERY_MODE_PAUSE)
        loopCheckDeviceState()
    }

    /**开始循环查询设备状态*/
    fun startLoopCheckState(start: Boolean = true, removePause: Boolean = true) {
        queryMode = if (!start) {
            0x0
        } else {
            if (removePause) {
                pauseLoopCheckState(false)
            }
            queryMode.add(QUERY_MODE_LOOP)
        }
        loopCheckDeviceState()
    }

    /**暂停轮询状态查询*/
    fun pauseLoopCheckState(pause: Boolean = true) {
        queryMode = if (pause) {
            queryMode.add(QUERY_MODE_PAUSE)
        } else {
            queryMode.remove(QUERY_MODE_PAUSE)
        }
    }

    /**持续检查工作作态*/
    private fun loopCheckDeviceState() {
        if (isLooping) return
        if (!isLoop) return
        isLooping = true
        _delay(HawkEngraveKeys.minQueryDelayTime) {
            //延迟1秒后, 继续查询状态
            if (isPause || !isLoop) {
                //暂停状态, 循环继续, 但是指令不发送
                isLooping = false
                loopCheckDeviceState()
            } else {
                queryDeviceState { bean, error ->
                    isLooping = false
                    loopCheckDeviceState()
                }
            }
        }
    }

    /**查询设备状态*/
    fun queryDeviceState(
        flag: Int = CommandQueueHelper.FLAG_NORMAL,
        block: IReceiveBeanAction = { _, _ -> }
    ) {
        QueryCmd.workState.enqueue(flag) { bean, error ->
            bean?.let {
                it.parse<QueryStateParser>()?.let {
                    updateDeviceState(it)
                }
            }
            block(bean, error)
        }
    }

    /**设备当前的工作状态*/
    @AnyThread
    fun updateDeviceState(queryStateParser: QueryStateParser) {
        queryStateParser.deviceAddress = LaserPeckerHelper.initDeviceAddress
        "设备状态:$queryStateParser".writeBleLog(if (isLoop) L.NONE else L.INFO)

        //记录设备状态改变
        val stackList = deviceStateStackData.value
        val lastState = stackList!!.lastOrNull()
        if (lastState?.deviceAddress != queryStateParser.deviceAddress) {
            //设备不一样
            stackList.clear()
            stackList.add(queryStateParser)
            deviceStateStackData.updateValue(stackList)
        } else {
            //相同设备, 状态不一样才记录
            if (lastState?.mode != queryStateParser.mode &&
                lastState?.workState != queryStateParser.workState
            ) {
                stackList.add(queryStateParser)
                deviceStateStackData.updateValue(stackList)
            }
        }

        //空闲状态, 自动停止循环查询
        if (queryStateParser.mode == QueryStateParser.WORK_MODE_IDLE) {
            pauseLoopCheckState(true)
        } else if (queryStateParser.mode == QueryStateParser.WORK_MODE_FILE_DOWNLOAD ||
            queryStateParser.mode == QueryStateParser.WORK_MODE_SHUTDOWN ||
            queryStateParser.mode == QueryStateParser.WORK_MODE_DOWNLOAD ||
            queryStateParser.mode == QueryStateParser.WORK_MODE_SETUP
        ) {
            pauseLoopCheckState(true)
        } else if (isPause) {
            //其他模式下, 如果处理暂停状态, 则继续轮询
            pauseLoopCheckState(false)
        } else if (queryStateParser.mode == QueryStateParser.WORK_MODE_ENGRAVE ||
            queryStateParser.mode == QueryStateParser.WORK_MODE_ENGRAVE_PREVIEW
        ) {
            //工作模式下, 自动开始轮询
            //startLoopCheckState()
        }

        //设备错误码
        queryStateParser.error.toErrorStateString()?.let {
            "机器错误码[${queryStateParser.error}]:$it".writeErrorLog()

            //查询到设备异常
            doMain {
                toastQQ(it)
            }

            //查询错误日志
            QueryCmd.log.enqueue { bean, error ->
                if (error == null) {
                    bean?.parse<QueryLogParser>()?.log?.let {
                        "机器错误日志:$it".writeErrorLog()
                    }
                }
            }
        }
        deviceStateData.updateValue(queryStateParser)
        updateDeviceModel(queryStateParser.mode)
    }

    /**更新设备模式*/
    @AnyThread
    fun updateDeviceModel(model: Int) {
        deviceModelData.updateValue(model)
    }

    //---

    /**是否需要显示外设提示*/
    fun needShowExDeviceTipItem(): Boolean = laserPeckerModel.haveExDevice() ||
            laserPeckerModel.isSRepMode() ||
            isPenMode() ||
            laserPeckerModel.isCarOpen() ||
            laserPeckerModel.isCSeries() //C1

    /**是否是C1的握笔模块*/
    fun isPenMode(moduleState: Int? = deviceStateData.value?.moduleState): Boolean {
        return moduleState == 4 /*|| isDebugType()*/
    }

    /**是否是C1的小车模式
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel.isCarOpen]*/
    fun isCarMode(): Boolean {
        return deviceStateData.value?.carConnect == 1
    }

    /**激光是否异常, 比如C1未插好激光头*/
    fun isLaserException(): Boolean {
        return deviceStateData.value?.moduleState == 255
    }

    /**空闲模式*/
    fun isIdleMode(): Boolean {
        val deviceState = deviceStateData.value
        return deviceState?.mode == QueryStateParser.WORK_MODE_IDLE
    }

    /**关机状态, 设备已关机*/
    fun isShutdownMode(): Boolean {
        val deviceState = deviceStateData.value
        return deviceState?.mode == QueryStateParser.WORK_MODE_SHUTDOWN
    }

    /**雕刻预览模式, 并且非显示中心*/
    fun isEngravePreviewMode(): Boolean {
        val deviceState = deviceStateData.value
        return deviceState?.mode == QueryStateParser.WORK_MODE_ENGRAVE_PREVIEW &&
                deviceState.workState != 0x07
    }

    /**是否是雕刻预览模式下的显示中心*/
    fun isEngravePreviewShowCenterMode(): Boolean {
        val deviceState = deviceStateData.value
        return deviceState?.mode == QueryStateParser.WORK_MODE_ENGRAVE_PREVIEW &&
                deviceState.workState == 0x07
    }

    /**是否是雕刻预览模式下的显示中心*/
    fun isEngravePreviewPause(): Boolean {
        val deviceState = deviceStateData.value
        return deviceState?.mode == QueryStateParser.WORK_MODE_ENGRAVE_PREVIEW &&
                deviceState.workState == 0x04
    }

    /**Z轴滚动预览中*/
    fun isEngravePreviewZ(): Boolean {
        val deviceState = deviceStateData.value
        return deviceState?.mode == QueryStateParser.WORK_MODE_ENGRAVE_PREVIEW &&
                deviceState.workState == 0x05
    }

}

/**发送退出指令, 如果需要*/
fun checkExitIfNeed() {
    val queryStateParser = vmApp<DeviceStateModel>().deviceStateData.value
    if (queryStateParser?.isModeEngrave() == true || queryStateParser?.isModeIdle() == true) {
    } else {
        //进入空闲模式, 才能开始打印
        ExitCmd().enqueue()
    }
}

/**静态方法, 异步查询设备状态*/
fun asyncQueryDeviceState(
    flag: Int = CommandQueueHelper.FLAG_ASYNC,
    block: IReceiveBeanAction = { _, _ -> }
) {
    vmApp<DeviceStateModel>().queryDeviceState(flag, block)
}

/**同步查询设备状态*/
fun syncQueryDeviceState(
    flag: Int = CommandQueueHelper.FLAG_NORMAL,
    block: IReceiveBeanAction = { _, _ -> }
) {
    vmApp<DeviceStateModel>().queryDeviceState(flag, block)
}