package com.angcyo.bluetooth.fsc.core

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/26
 */

/**数据发送进度,速度*/
data class DevicePacketProgress(
    val address: String,
    /**已发送成功的字节大小*/
    var sendBytesSize: Long = 0,
    /**发送包的数量*/
    var sendPacketCount: Int = 0,
    /**发送进度[0-100]*/
    var percentage: Int = -1,
    /**发送的开始时间, 毫秒*/
    var startTime: Long = -1,
    /**完成时的时间, 毫秒*/
    var finishTime: Long = -1,
    /**是否暂停了*/
    var isPause: Boolean = false,
)

