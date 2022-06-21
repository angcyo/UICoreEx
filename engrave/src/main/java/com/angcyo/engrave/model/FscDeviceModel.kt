package com.angcyo.engrave.model

import com.angcyo.bluetooth.fsc.FscBleApiModel
import com.angcyo.bluetooth.fsc.core.DeviceConnectState
import com.angcyo.core.lifecycle.LifecycleViewModel
import com.angcyo.core.vmApp
import com.angcyo.engrave.R
import com.angcyo.library.ex._string
import com.angcyo.library.toast

/**
 * 蓝牙设备模式
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/21
 */
class FscDeviceModel : LifecycleViewModel() {

    val apiModel = vmApp<FscBleApiModel>()

    init {
        //蓝牙状态监听
        apiModel.connectStateData.observe(this) {
            it?.let { deviceConnectState ->
                if (deviceConnectState.state == DeviceConnectState.CONNECT_STATE_DISCONNECT) {
                    //蓝牙设备断开
                    toast(_string(R.string.bluetooth_lib_scan_disconnected))
                } else if (deviceConnectState.state == DeviceConnectState.CONNECT_STATE_SUCCESS) {
                    toast(_string(R.string.bluetooth_ft_scan_connected))
                }
            }
        }
    }

}