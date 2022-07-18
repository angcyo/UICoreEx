package com.angcyo.bluetooth.fsc.core

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/26
 */

/**数据发送状态*/
data class DevicePacketState(
    val address: String,
    val bytes: ByteArray,
    /**数据发送的进度[0-100]
     * -1表示发送的数据包
     * [0-100]表示发送包的进度*/
    val percentage: Int = -1,
    val state: Int = PACKET_STATE_NORMAL,
) {
    companion object {
        const val PACKET_STATE_NORMAL = 0

        /**开始发送数据*/
        const val PACKET_STATE_START = 1

        /**发送数据的进度*/
        const val PACKET_STATE_PROGRESS = 2

        /**暂停发送*/
        const val PACKET_STATE_PAUSE = 3

        /**停止发送*/
        const val PACKET_STATE_STOP = 4

        /**发送完成*/
        const val PACKET_STATE_FINISH = 5

        /**接收的数据*/
        const val PACKET_STATE_RECEIVED = 5
    }
}