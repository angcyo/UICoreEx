package com.angcyo.device.client

import com.angcyo.core.vmApp
import com.angcyo.device.DeviceServerHelper
import com.angcyo.device.bean.DeviceBean
import com.angcyo.device.core.BaseServer
import com.angcyo.http.base.fromJson
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.ex.fromBase64
import com.angcyo.library.ex.size
import java.net.DatagramPacket
import java.net.DatagramSocket

/**
 * 设备发现, 接收UDP广播
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/12/02
 */
class DeviceDiscover(val port: Int, freq: Long = 160) : BaseServer(freq) {

    companion object {

        /**设备发现*/
        var deviceDiscover: DeviceDiscover? = null

        /**开始发现设备*/
        @CallPoint
        fun start() {
            if (deviceDiscover?.isRunning == true) {
                return
            }
            deviceDiscover = DeviceDiscover(DeviceServerHelper._deviceBroadcastPort)
            deviceDiscover?.start()
        }

        /**停止发现设备*/
        @CallPoint
        fun stop() {
            deviceDiscover?.stop()
            deviceDiscover = null
        }
    }

    override fun runInner() {
        val socket = DatagramSocket(port)
        val buffer = ByteArray(DeviceServerHelper.BUFFER_SIZE)
        val data = DatagramPacket(buffer, buffer.size())//数据包存放
        socket.receive(data)//接收UDP数据
        socket.close()

        //内容结构: AABB
        val contentBuffer = buffer.sliceArray(0 until data.length)
        val content = contentBuffer.toString(DeviceServerHelper.DEF_CHARSET)
        if (content.startsWith(DeviceServerHelper.HEADER) && content.endsWith(DeviceServerHelper.FOOTER)) {
            val body = content.substring(
                DeviceServerHelper.HEADER.length,
                content.length - DeviceServerHelper.FOOTER.length
            ).fromBase64()
            val bean = body.fromJson<DeviceBean>()
            if (bean != null && !bean.deviceId.isNullOrBlank()) {
                bean.address = data.address?.hostAddress
                //有效的设备
                vmApp<DeviceDiscoverModel>().onDiscoverDevice(bean)
            }
        }
    }
}