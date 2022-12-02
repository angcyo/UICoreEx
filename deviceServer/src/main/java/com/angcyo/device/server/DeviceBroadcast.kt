package com.angcyo.device.server

import com.angcyo.device.DeviceServerHelper
import com.angcyo.device.core.BaseServer
import com.angcyo.library.ex.size
import com.angcyo.library.ex.toBase64
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * 设备广播, 发送设备信息到局域网内
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/12/02
 */
class DeviceBroadcast(val port: Int /*端口*/, freq: Long = 160) : BaseServer(freq) {

    /**需要广播的内容, 会自动进行base64加密处理
     * [com.angcyo.device.bean.DeviceBean]
     * */
    var content: String? = null

    //---

    /**广播的地址*/
    var _broadcastAddress: String = "255.255.255.255"

    override fun runInner() {
        sendBroadcast()
    }

    /**创建广播内容,
     * 内容格式: AA{json.base64}BB
     * 首:AA 中间:json内容的base64字符串 尾:BB
     * */
    private fun createContent(): String {
        val header = DeviceServerHelper.HEADER
        val footer = DeviceServerHelper.FOOTER
        val content = buildString {
            append(header)
            append(content?.toBase64() ?: "")
            append(footer)
        }
        return content
    }

    /**发送广播*/
    private fun sendBroadcast() {
        val socket = DatagramSocket()
        val bytes = createContent().toByteArray(DeviceServerHelper.DEF_CHARSET)
        val data = DatagramPacket(
            bytes,
            0,
            bytes.size(),
            InetAddress.getByName(_broadcastAddress),
            port
        )//数据包
        socket.send(data)//发送UDP数据包
        socket.close()
    }

}