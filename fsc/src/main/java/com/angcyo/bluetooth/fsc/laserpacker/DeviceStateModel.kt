package com.angcyo.bluetooth.fsc.laserpacker

import androidx.annotation.AnyThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.angcyo.bluetooth.fsc.CommandQueueHelper
import com.angcyo.bluetooth.fsc.FscBleApiModel
import com.angcyo.bluetooth.fsc.IReceiveBeanAction
import com.angcyo.bluetooth.fsc.WifiApiModel
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.bean.DeviceConfigBean
import com.angcyo.bluetooth.fsc.laserpacker.bean.matchesProductVersion
import com.angcyo.bluetooth.fsc.laserpacker.command.ExitCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.QueryCmd
import com.angcyo.bluetooth.fsc.laserpacker.data.LaserTypeInfo
import com.angcyo.bluetooth.fsc.laserpacker.parse.QueryLogParser
import com.angcyo.bluetooth.fsc.laserpacker.parse.QueryStateParser
import com.angcyo.bluetooth.fsc.laserpacker.parse.toErrorStateString
import com.angcyo.bluetooth.fsc.parse
import com.angcyo.core.component.file.writeErrorLog
import com.angcyo.core.component.file.writeToLog
import com.angcyo.core.vmApp
import com.angcyo.http.tcp.TcpConnectInfo
import com.angcyo.library.L
import com.angcyo.library.component._removeMainRunnable
import com.angcyo.library.component.onMainOnce
import com.angcyo.library.ex.add
import com.angcyo.library.ex.have
import com.angcyo.library.ex.isDebug
import com.angcyo.library.ex.remove
import com.angcyo.library.ex.toStr
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

    /**自动等到设备空闲后, 再退出*/
    var waitForExit = false

    /**查询状态的动作*/
    private val _queryStateRunnable = Runnable {
        //延迟1秒后, 继续查询状态
        if (isLoop) {
            if (isPause) {
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

    /**开始循环查询设备状态*/
    fun startLoopCheckState(start: Boolean = true, removePause: Boolean = true, reason: String?) {
        waitForExit = false
        queryMode = if (!start) {
            0x0
        } else {
            if (removePause) {
                pauseLoopCheckState(false, reason)
            }
            queryMode.add(QUERY_MODE_LOOP)
        }
        if (start) {
            "开始轮询查询设备状态:$reason".writeToLog()
            loopCheckDeviceState()
        } else {
            "结束轮询查询设备状态:$reason".writeToLog()
            removeLoopCheck()
        }
    }

    /**暂停轮询状态查询*/
    fun pauseLoopCheckState(pause: Boolean = true, reason: String?) {
        if (pause) {
            "暂停轮询查询设备状态:$reason".writeToLog()
            queryMode = queryMode.add(QUERY_MODE_PAUSE)
            removeLoopCheck()
        } else {
            "继续轮询查询设备状态:$reason".writeToLog()
            queryMode = queryMode.remove(QUERY_MODE_PAUSE)
            loopCheckDeviceState()
        }
    }

    /**持续检查工作作态*/
    private fun loopCheckDeviceState() {
        if (isLooping) return
        if (!isLoop) return
        isLooping = true
        onMainOnce(HawkEngraveKeys.minQueryDelayTime, _queryStateRunnable)
    }

    /**移除循环检查*/
    private fun removeLoopCheck() {
        _removeMainRunnable(_queryStateRunnable)
        isLooping = false
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
        var lastState = stackList?.lastOrNull()
        if (lastState?.deviceAddress != queryStateParser.deviceAddress) {
            //设备不一样
            lastState = null
            stackList?.clear()
            stackList?.add(queryStateParser)
            deviceStateStackData.updateValue(stackList)
        } else {
            //相同设备, 状态不一样才记录
            if (lastState?.mode != queryStateParser.mode &&
                lastState?.workState != queryStateParser.workState
            ) {
                stackList?.add(queryStateParser)
                deviceStateStackData.updateValue(stackList)
            }
        }

        //空闲状态, 自动停止循环查询
        if (queryStateParser.mode == QueryStateParser.WORK_MODE_IDLE) {
            if (waitForExit) {
                startLoopCheckState(false, reason = "waitForExit")
            } else {
                pauseLoopCheckState(true, reason = "空闲状态")
            }
        } else if (queryStateParser.mode == QueryStateParser.WORK_MODE_FILE_DOWNLOAD ||
            queryStateParser.mode == QueryStateParser.WORK_MODE_SHUTDOWN ||
            queryStateParser.mode == QueryStateParser.WORK_MODE_DOWNLOAD ||
            queryStateParser.mode == QueryStateParser.WORK_MODE_SETUP
        ) {
            pauseLoopCheckState(true, reason = "不支持轮询的模式")
        } else if (isPause) {
            //其他模式下, 如果处理暂停状态, 则继续轮询
            pauseLoopCheckState(false, "恢复轮询状态")
        } else if (queryStateParser.mode == QueryStateParser.WORK_MODE_ENGRAVE ||
            queryStateParser.mode == QueryStateParser.WORK_MODE_ENGRAVE_PREVIEW
        ) {
            //工作模式下, 自动开始轮询
            //startLoopCheckState()
        }

        //设备错误码
        queryStateParser.error.toErrorStateString()?.let {
            //查询到设备异常
            if (lastState?.error != queryStateParser.error) {
                "机器错误码[${queryStateParser.error}:${queryStateParser.error.toErrorStateString()}]:$it".writeErrorLog()

                //错误码不一样, 才提示
                if (queryStateParser.error != 1) {
                    toastQQ(it)

                    //查询错误日志
                    QueryCmd.log.enqueue { bean, error ->
                        if (error == null) {
                            bean?.parse<QueryLogParser>()?.log?.let {
                                "机器错误日志:$it".writeErrorLog()
                            }
                        }
                    }
                }
            }
        }
        if (queryStateParser.error == 1) {
            //非安全状态下, 开始轮询状态, 知道进入安全状态
            startLoopCheckState(true, reason = "非安全状态")
        }
        deviceStateData.updateValue(queryStateParser)
        updateDeviceModel(queryStateParser.mode)
    }

    /**更新设备模式*/
    @AnyThread
    fun updateDeviceModel(model: Int) {
        deviceModelData.updateValue(model)
    }

    /**获取当前设备对应的设备配置信息
     * [type] 激光类型*/
    fun getDeviceConfig(
        type: Byte,
        moduleState: Int? = deviceStateData.value?.moduleState
    ): DeviceConfigBean? {
        var result = laserPeckerModel.productInfoData.value?.laserTypeList?.find {
            if (it.moduleState == -1) {
                it.type == type
            } else {
                it.moduleState == moduleState
            }
        }
        if (result == null) {
            val configList = LaserPeckerConfigHelper.readDeviceConfig()
            if (!configList.isNullOrEmpty()) {
                for (config in configList) {
                    result = config.laserTypeList?.find {
                        if (it.moduleState == -1) {
                            it.type == type
                        } else {
                            it.moduleState == moduleState
                        }
                    }
                    if (result != null) {
                        return config
                    }
                }
            }
        }
        return laserPeckerModel.productInfoData.value?.deviceConfigBean
    }

    /**获取当前设备对应的模块信息
     * [type] 激光类型*/
    fun getDeviceLaserModule(
        type: Byte,
        moduleState: Int? = deviceStateData.value?.moduleState
    ): LaserTypeInfo? {
        var result = laserPeckerModel.productInfoData.value?.laserTypeList?.find {
            if (it.moduleState == -1) {
                it.type == type
            } else {
                it.moduleState == moduleState
            }
        }
        if (result == null) {
            val configList = LaserPeckerConfigHelper.readDeviceConfig()
            if (!configList.isNullOrEmpty()) {
                for (config in configList) {
                    result = config.laserTypeList?.find {
                        if (it.moduleState == -1) {
                            it.type == type
                        } else {
                            it.moduleState == moduleState
                        }
                    }
                    if (result != null) {
                        return result
                    }
                }
            }
        }
        return result
    }

    /**获取当前安装的模块, 主要针对C1*/
    fun getDeviceModuleLabel(moduleState: Int? = deviceStateData.value?.moduleState): String {
        return laserPeckerModel.productInfoData.value?.laserTypeList?.find {
            it.moduleState == moduleState
        }?.toLabel()?.toStr() ?: "Unknown${moduleState ?: -1}"

        /*return when (moduleState) {
            //0 5W激光
            0 -> "5W 450nm"
            //1 10W激光
            1 -> "10W 450nm"
            //2 20W激光
            2 -> "20W 450nm"
            //3 1064激光
            3 -> "2W 1064nm"
            //4 单色笔模式
            4 -> _string(R.string.engrave_module_single_pen)
            //5 彩色笔模式
            5 -> _string(R.string.engrave_module_color_pen)
            //6 刀切割模式
            6 -> _string(R.string.engrave_module_knife_cutting)
            //7 CNC模式
            7 -> _string(R.string.engrave_module_cnc)
            else -> "Unknown$moduleState"
        }*/
    }

    //---

    /**是否有设备连接, 包括ble / wifi 连接*/
    fun isDeviceConnect() =
        vmApp<FscBleApiModel>().haveDeviceConnected() || vmApp<WifiApiModel>().isTcpConnected()

    /**断开设备连接*/
    fun disconnectDevice(info: TcpConnectInfo?) {
        vmApp<FscBleApiModel>().disconnectAll()
        vmApp<WifiApiModel>().disconnect(info)
    }

    /**设备是否处理不安全状态, 此状态下禁止操作预览/打印*/
    fun isUnsafe() = deviceStateData.value?.error == 1

    /**是否需要显示外设提示*/
    fun needShowExDeviceTipItem(): Boolean = laserPeckerModel.haveExDevice() ||
            laserPeckerModel.isSRepMode() ||
            isPenMode() ||
            laserPeckerModel.isCarConnect() ||
            laserPeckerModel.isCSeries() //C1

    /**是否是C1的握笔模块*/
    fun isPenMode(moduleState: Int? = deviceStateData.value?.moduleState): Boolean {
        return moduleState == 4 /*|| isDebugType()*/
    }

    /**是否是切割模块*/
    fun isCutModule(moduleState: Int? = deviceStateData.value?.moduleState): Boolean {
        val cut = _deviceSettingBean?.cutLayerModule?.split(",")
            ?.contains("$moduleState") == true
        return cut
    }

    /**是否有切割图层*/
    fun haveCutLayer(moduleState: Int? = deviceStateData.value?.moduleState): Boolean {
        return HawkEngraveKeys.enableForceCut ||
                (laserPeckerModel.isCSeries() && isCutModule(moduleState)) ||
                (laserPeckerModel.productInfoData.value?.deviceConfigBean?.supportCut == true)
    }

    /**是否是C1的小车模式
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel.isCarConnect]*/
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

    /**是否处于雕刻预览中*/
    fun isEngravePreview(): Boolean {
        val deviceState = deviceStateData.value
        return deviceState?.mode == QueryStateParser.WORK_MODE_ENGRAVE_PREVIEW
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

    /**自动检查, 是否需要发送退出指令
     * 目前在预览中, 杀掉APP机器需要退出
     * */
    fun exitIfNeed() {
        startLoopCheckState(false, reason = "exitIfNeed")
        if (isEngravePreview()) {
            ExitCmd().enqueue(CommandQueueHelper.FLAG_ASYNC)
        }
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

/**是否要气泵参数*/
val _showPumpConfig: Boolean
    get() = _deviceSettingBean?.showPumpRange?.matchesProductVersion() == true &&
            vmApp<DeviceStateModel>().getDeviceLaserModule(255.toByte())?.showPump == true

/**是否要显示频率配置*/
val _showLaserFrequencyConfig: Boolean
    get() = isDebug() ||
            _deviceSettingBean?.showLaserFrequencyRange?.matchesProductVersion() == true ||
            HawkEngraveKeys.showLaserFrequencyRange?.matchesProductVersion() == true