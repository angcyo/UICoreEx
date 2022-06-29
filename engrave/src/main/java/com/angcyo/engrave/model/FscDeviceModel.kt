package com.angcyo.engrave.model

import android.app.Activity
import android.os.Debug
import com.angcyo.base.dslAHelper
import com.angcyo.bluetooth.fsc.FscBleApiModel
import com.angcyo.bluetooth.fsc.core.DeviceConnectState
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.parse.QuerySettingParser
import com.angcyo.core.lifecycle.LifecycleViewModel
import com.angcyo.core.vmApp
import com.angcyo.engrave.R
import com.angcyo.engrave.ble.DeviceConnectTipActivity
import com.angcyo.library.L
import com.angcyo.library.app
import com.angcyo.library.component.OnBackgroundObserver
import com.angcyo.library.component.RBackground
import com.angcyo.library.ex._string
import com.angcyo.library.ex.nowTime
import com.angcyo.library.toast
import com.angcyo.objectbox.findLast
import com.angcyo.objectbox.laser.pecker.LPBox
import com.angcyo.objectbox.laser.pecker.entity.DeviceConnectEntity
import com.angcyo.objectbox.laser.pecker.lpBoxOf
import com.angcyo.objectbox.saveEntity

/**
 * 蓝牙设备模式
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/21
 */
class FscDeviceModel : LifecycleViewModel() {

    val bleApiModel = vmApp<FscBleApiModel>()

    /**最后一次触发自动连接的时间, 毫秒*/
    var lastConnectTime: Long = -1

    /**初始化*/
    fun initDevice() {

        //蓝牙状态监听
        bleApiModel.connectStateData.observe(this) {
            it?.let { deviceConnectState ->
                if (deviceConnectState.state == DeviceConnectState.CONNECT_STATE_DISCONNECT) {
                    //蓝牙设备断开
                    toast(_string(R.string.bluetooth_lib_scan_disconnected))
                } else if (deviceConnectState.state == DeviceConnectState.CONNECT_STATE_SUCCESS) {
                    //蓝牙已连接

                    //发送初始化指令
                    LaserPeckerHelper.sendInitCommand(deviceConnectState.device.address)

                    //入库
                    DeviceConnectEntity().apply {
                        deviceAddress = deviceConnectState.device.address
                        deviceName = deviceConnectState.device.name
                        saveEntity(LPBox.PACKAGE_NAME)
                    }

                    if (deviceConnectState.isAutoConnect) {
                        //自动连接成功后, 显示连接提示
                        lastConnectTime = nowTime()
                        (RBackground.lastActivityRef?.get() ?: app()).dslAHelper {
                            start(DeviceConnectTipActivity::class)
                        }
                    } else {
                        toast(_string(R.string.bluetooth_ft_scan_connected))
                    }
                }
            }
        }

        //自动连接监听
        RBackground.registerObserver(object : OnBackgroundObserver() {
            override fun onActivityLifecycleChanged(activity: Activity, state: String) {
                super.onActivityLifecycleChanged(activity, state)
                if (state == RBackground.RESUMED) {
                    if (QuerySettingParser.AUTO_CONNECT_DEVICE) {
                        //需要自动连接设备
                        val nowTime = nowTime()
                        if (nowTime - lastConnectTime > 1 * 60 * 1_000 || Debug.isDebuggerConnected()) {
                            //1分钟
                            lpBoxOf(DeviceConnectEntity::class).findLast()?.let {
                                L.i("准备自动连接设备:${it.deviceName} ${it.deviceAddress}")
                                bleApiModel.connect(it.deviceAddress, it.deviceName, true)
                            }
                        }
                    }
                }
            }
        })
    }

}