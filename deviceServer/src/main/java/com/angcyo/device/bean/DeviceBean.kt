package com.angcyo.device.bean

import com.angcyo.library.ex.connectUrl

/**
 * 广播的内容结构
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/12/02
 */
data class DeviceBean(
    /**设备的id, 标识设备*/
    val deviceId: String?,
    /**设备名称*/
    val name: String?,
    /**服务端口*/
    val port: Int,
    /**连接令牌*/
    val token: String?,
    /**设备的ip地址*/
    var address: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DeviceBean

        if (deviceId != other.deviceId) return false

        return true
    }

    override fun hashCode(): Int {
        return deviceId?.hashCode() ?: 0
    }

    /**转换成接口地址*/
    fun toApi(path: String?): String = "http://${address}:${port}".connectUrl(path)
}
