package com.angcyo.device.server

import com.angcyo.device.DeviceServerHelper
import com.angcyo.device.bean.DeviceBean
import com.angcyo.http.base.toJson
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.ex.getWifiIP
import com.angcyo.library.utils.Device
import com.angcyo.library.utils.ID
import com.angcyo.library.utils.uuid
import com.koushikdutta.async.http.server.AsyncHttpServer


/**
 * 设备服务
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/12/02
 */
class DeviceServer(val port: Int) {

    companion object {

        /**设备服务*/
        var deviceServer: DeviceServer? = null

        /**设备广播服务*/
        var deviceBroadcast: DeviceBroadcast? = null

        //---

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
                uuid(),
                getWifiIP()
            ).toJson()
            deviceBroadcast?.start()
        }

        /**停止广播*/
        @CallPoint
        fun stopBroadcast() {
            deviceBroadcast?.stop()
            deviceBroadcast = null
        }

        //---

        /**启动接口服务*/
        @CallPoint
        fun startServer(port: Int) {
            if (deviceServer == null) {
                deviceServer = DeviceServer(port)
                deviceServer?.start()
            }
        }

        /**停止接口服务*/
        @CallPoint
        fun stopServer() {
            deviceServer?.stop()
            deviceServer = null
        }
    }

    /**服务*/
    var server: AsyncHttpServer? = null

    /**开启服务*/
    @CallPoint
    fun start() {
        if (server == null) {
            server = AsyncHttpServer().apply {
                DeviceServerAction.initServerAction(this)
                listen(port)
            }
        }
    }

    /**停止服务*/
    @CallPoint
    fun stop() {
        server?.stop()
        server = null
    }

}