package com.angcyo.device

import com.angcyo.device.client.DeviceDiscover
import com.angcyo.device.server.DeviceServer
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.component.Port
import java.nio.charset.Charset

/**
 * 设备服务助手
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/12/02
 */
object DeviceServerHelper {

    /**buffer大小*/
    const val BUFFER_SIZE = 1024

    /**默认的字符编码*/
    val DEF_CHARSET: Charset = Charsets.UTF_8

    //

    /**广播数据的首尾*/
    const val HEADER = "AA"
    const val FOOTER = "BB"

    //---

    /**设备广播的端口*/
    var _deviceBroadcastPort = 6800

    /**设备服务的端口*/
    var _deviceServerPort = 7800

    /**初始化*/
    @CallPoint
    fun init() {

    }

    //---

    /**启动设备广播服务*/
    @CallPoint
    fun startServer() {
        _deviceBroadcastPort = Port.generatePort(_deviceBroadcastPort)
        _deviceServerPort = Port.generatePort(_deviceServerPort)

        //启动设备广播
        DeviceServer.startBroadcast()

        //启动接口服务
        DeviceServer.startServer(_deviceServerPort)
    }

    /**停止设备广播服务*/
    @CallPoint
    fun stopServer() {
        DeviceServer.stopBroadcast()
        DeviceServer.stopServer()
    }

    //---

    /**开始发现设备*/
    @CallPoint
    fun startDiscoverServer() {
        DeviceDiscover.start()
    }

    /**停止发现设备*/
    @CallPoint
    fun stopDiscoverServer() {
        DeviceDiscover.stop()
    }
}