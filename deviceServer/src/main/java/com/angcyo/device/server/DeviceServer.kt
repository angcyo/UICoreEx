package com.angcyo.device.server

import com.angcyo.device.DeviceServerHelper
import com.angcyo.device.bean.DeviceBean
import com.angcyo.http.base.toJson
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.utils.Device
import com.angcyo.library.utils.ID
import com.angcyo.library.utils.uuid

/**
 * 设备服务
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/12/02
 */
object DeviceServer {

    /**设备广播服务*/
    var deviceBroadcast: DeviceBroadcast? = null

    /**开始设备广播*/
    @CallPoint
    fun startBroadcast() {
        if (deviceBroadcast?.isRunning == true) {
            return
        }
        deviceBroadcast = DeviceBroadcast(DeviceServerHelper._deviceBroadcastPort)
        //需要广播的内容
        deviceBroadcast?.content = DeviceBean(
            ID.id,
            Device.deviceName,
            DeviceServerHelper._deviceServerPort,
            uuid()
        ).toJson()
        deviceBroadcast?.start()
    }

    /**停止广播*/
    @CallPoint
    fun stopBroadcast() {
        deviceBroadcast?.stop()
        deviceBroadcast = null
    }

}