package com.angcyo.bluetooth.fsc.core

/**
 * 接收的数据包结构
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/26
 */

/**数据接收*/
data class PacketReceive(
    val address: String,
    /**已接收的字节数量*/
    var receiveBytesSize: Long = 0,
    /**接收包的数量*/
    var receivePacketCount: Int = 0,
    /**接收的开始时间, 毫秒*/
    var startTime: Long = -1
)
