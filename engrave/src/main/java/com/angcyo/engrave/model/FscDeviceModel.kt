package com.angcyo.engrave.model

import android.app.Activity
import android.graphics.RectF
import com.angcyo.base.dslAHelper
import com.angcyo.bluetooth.fsc.FscBleApiModel
import com.angcyo.bluetooth.fsc.core.DeviceConnectState
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.canvas.graphics.GraphicsHelper
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.core.dslitem.DslLastDeviceInfoItem
import com.angcyo.core.lifecycle.LifecycleViewModel
import com.angcyo.core.vmApp
import com.angcyo.engrave.EngraveHelper
import com.angcyo.engrave.R
import com.angcyo.engrave.ble.DeviceConnectTipActivity
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.item.component.DebugAction
import com.angcyo.item.component.DebugFragment
import com.angcyo.library.L
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.annotation.Pixel
import com.angcyo.library.component.OnBackgroundObserver
import com.angcyo.library.component.RBackground
import com.angcyo.library.component.lastContext
import com.angcyo.library.ex._string
import com.angcyo.library.ex.nowTime
import com.angcyo.library.toastQQ
import com.angcyo.library.unit.toMm
import com.angcyo.library.utils.LogFile
import com.angcyo.library.utils.appFolderPath
import com.angcyo.library.utils.toLogFilePath
import com.angcyo.objectbox.findLastList
import com.angcyo.objectbox.laser.pecker.LPBox
import com.angcyo.objectbox.laser.pecker.entity.DeviceConnectEntity
import com.angcyo.objectbox.laser.pecker.lpBoxOf
import com.angcyo.objectbox.laser.pecker.lpSaveEntity
import com.angcyo.objectbox.saveEntity
import com.angcyo.viewmodel.observe
import com.angcyo.viewmodel.observeOnce
import com.angcyo.viewmodel.updateValue
import com.angcyo.viewmodel.vmDataOnce
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue

/**
 * 蓝牙设备模式
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/21
 */
class FscDeviceModel : LifecycleViewModel() {

    companion object {
        /**主动断开的连接, 1小时之内不自动连接*/
        var AUTO_CONNECT_DISCONNECTED_THRESHOLD = 1 * 60 * 60 * 1_000L

        /**临时禁用自动连接到这个时间点, 13位毫秒*/
        var disableAutoConnectToTime = 0L

        /**如果配置了此属性, 则分配位置的时候, 会在此矩形的中心*/
        @Pixel
        var productAssignLocationBounds: RectF? = null
    }

    val bleApiModel = vmApp<FscBleApiModel>()
    val laserPeckerModel = vmApp<LaserPeckerModel>()

    /**最后一次触发自动连接的时间, 毫秒*/
    var lastConnectTime: Long = -1

    /**通知, 是否要显示自动连接提示*/
    val autoConnectTipData = vmDataOnce<Boolean>(null)

    /**初始化*/
    @CallPoint
    fun initDevice() {
        //蓝牙状态监听
        bleApiModel.connectStateData.observe(this, allowBackward = false) {
            it?.let { deviceConnectState ->

                if (deviceConnectState.state == DeviceConnectState.CONNECT_STATE_START) {
                    //开始连接
                    UMEvent.CONNECT_DEVICE.umengEventValue {
                        put(UMEvent.KEY_START_TIME, nowTime().toString())
                    }
                } else if (deviceConnectState.state == DeviceConnectState.CONNECT_STATE_DISCONNECT) {
                    //蓝牙断开
                    GraphicsHelper.assignLocationBounds = null
                    productAssignLocationBounds = null

                    if (deviceConnectState.connectTime > 0 &&
                        deviceConnectState.connectedTime > 0 &&
                        !deviceConnectState.isActiveDisConnected
                    ) {
                        //连接成功过, 并且非主动断开蓝牙设备, 则toast提示
                        toastQQ(_string(R.string.blue_disconnected))
                    }

                    //蓝牙断开后,清空设备状态
                    laserPeckerModel.apply {
                        deviceStateData.postValue(null)
                        initializeData.updateValue(false)
                    }

                    if (deviceConnectState.isActiveDisConnected) {
                        //主动断开的连接, 1小时之内不自动连接
                        lpBoxOf(DeviceConnectEntity::class).findLastList().lastOrNull()
                            ?.let {
                                it.disconnectTime = nowTime()
                                it.lpSaveEntity()
                            }
                    }
                } else if (deviceConnectState.state == DeviceConnectState.CONNECT_STATE_SUCCESS && deviceConnectState.isNormalConnect) {
                    //蓝牙已连接

                    //发送初始化指令
                    LaserPeckerHelper.sendInitCommand(
                        deviceConnectState.device.name,
                        deviceConnectState.device.address,
                        deviceConnectState.isAutoConnect
                    )

                    //入库
                    DeviceConnectEntity::class.saveEntity(LPBox.PACKAGE_NAME) {
                        isAutoConnect = deviceConnectState.isAutoConnect
                        deviceAddress = deviceConnectState.device.address
                        deviceName = deviceConnectState.device.name
                    }

                    if (deviceConnectState.isAutoConnect) {
                        //自动连接成功后, 显示连接提示
                        lastConnectTime = nowTime()

                        if (autoConnectTipData.hasObservers()) {
                            //如果有观察者, 则说明自动连接提示需要被拦截
                            autoConnectTipData.postValue(true)
                        } else {
                            //否则, 直接下你是自动连接成功提示
                            laserPeckerModel.productInfoData.observeOnce(allowBackward = false) {
                                //等待设备信息读取结束之后才显示
                                if (it != null) {
                                    if (bleApiModel.haveDeviceConnected()) {
                                        lastContext.dslAHelper {
                                            start(DeviceConnectTipActivity::class)
                                        }
                                    }
                                }
                                it != null
                            }
                        }
                    } else {
                        toastQQ(_string(R.string.blue_connected))
                    }

                    //
                    UMEvent.CONNECT_DEVICE.umengEventValue {
                        val nowTime = nowTime()
                        put(UMEvent.KEY_FINISH_TIME, nowTime.toString())
                        put(
                            UMEvent.KEY_DURATION,
                            (nowTime - deviceConnectState.connectTime).toString()
                        )
                    }
                }
            }
        }

        //自动连接监听
        RBackground.registerObserver(object : OnBackgroundObserver() {
            override fun onActivityLifecycleChanged(activity: Activity, state: String) {
                super.onActivityLifecycleChanged(activity, state)
                if (state == RBackground.RESUMED) {
                    checkAutoConnect()
                }
            }
        })

        //监听设备变化
        laserPeckerModel.productInfoData.observe(this) {
            if (it == null) {
                GraphicsHelper.restoreLocation()
                GraphicsHelper.assignLocationBounds = null
                productAssignLocationBounds = null
            }
            it?.let { product ->
                //
                GraphicsHelper._minLeft = product.previewBounds.left.toMm()
                GraphicsHelper._minTop = product.previewBounds.top.toMm()
                GraphicsHelper.assignLocationBounds = product.previewBounds
                productAssignLocationBounds = product.previewBounds
                /*vmApp<EngraveModel>().engraveOptionInfoData.value?.let { option ->
                    if (product.laserTypeList.isNotEmpty() && !product.laserTypeList.contains(option.type)) {
                        //当前设备不支持选中的激光类型, 则调整一下
                        option.type = product.laserTypeList.first()
                    }
                }*/
            }
        }

        //设备初始化后回调, 初始化材质信息
        laserPeckerModel.initializeOnceData.observe(this) {
            if (it == true) {
                //保存设备信息到log
                DslLastDeviceInfoItem.additional_info = buildString {
                    appendLine()
                    appendLine()
                    appendLine("设备版本↓")
                    appendLine("${laserPeckerModel.deviceVersionData.value}")

                    appendLine()
                    appendLine("设备状态↓")
                    appendLine("${laserPeckerModel.deviceStateData.value}")

                    appendLine()
                    appendLine("设备设置↓")
                    appendLine("${laserPeckerModel.deviceSettingData.value}")

                    appendLine()
                    appendLine("产品信息↓")
                    appendLine("${laserPeckerModel.productInfoData.value}")
                }
                DslLastDeviceInfoItem.saveDeviceInfo()

                laserPeckerModel.productInfoData.value?.let {
                    LaserPeckerHelper.updateProductInfo(it, laserPeckerModel.deviceStateData.value)
                    EngraveHelper.initMaterial()
                }
            }
        }

        //ble日志
        DebugFragment.DEBUG_ACTION_LIST.add(
            DebugAction(LogFile.ble, LogFile.ble.toLogFilePath())
        )

        //雕刻目录
        DebugFragment.DEBUG_ACTION_LIST.add(
            DebugAction("engrave", appFolderPath(CanvasConstant.ENGRAVE_FILE_FOLDER))
        )

        //设备主动退出工作模式
        //AA BB 08 FF 00 00 00 00 00 00 FF
    }

    /**检查是否需要自动连接设备*/
    fun checkAutoConnect() {
        if (HawkEngraveKeys.AUTO_CONNECT_DEVICE && !bleApiModel.haveDeviceConnected() /*无设备连接*/) {
            //需要自动连接设备
            val nowTime = nowTime()
            //1分钟
            if (FscBleApiModel.haveBluetoothPermission()) {
                var autoConnect = true
                lpBoxOf(DeviceConnectEntity::class).findLastList().lastOrNull()
                    ?.let {
                        val disconnectTime = it.disconnectTime
                        if (disconnectTime != null) {
                            //主动断开了连接
                            if (nowTime - disconnectTime < AUTO_CONNECT_DISCONNECTED_THRESHOLD) {
                                //不自动连接
                                autoConnect = false
                            }
                        }
                        if (autoConnect) {
                            if (nowTime < disableAutoConnectToTime) {
                                autoConnect = false
                            }
                        }
                        if (autoConnect) {
                            L.i("准备自动连接设备:${it.deviceName} ${it.deviceAddress}")
                            bleApiModel.connect(it.deviceAddress, it.deviceName, true)
                        }
                    }
            }
        }
    }

    /**自动连接最后一次的设备, 如果有*/
    fun autoConnectLastDevice(autoConnect: Boolean = true) {
        if (FscBleApiModel.haveBluetoothPermission()) {
            //有权限
            lpBoxOf(DeviceConnectEntity::class).findLastList().lastOrNull()
                ?.let {
                    bleApiModel.connect(it.deviceAddress, it.deviceName, autoConnect)
                }
        }
    }
}