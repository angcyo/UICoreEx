package com.angcyo.wifip2p.data

/**
 * 设备连接状态包装
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/08/10
 */
data class ConnectStateWrap(
    var state: Int = 0,
    var deviceWrap: WifiP2pDeviceWrap
) {
    companion object {

        /**开始连接*/
        const val STATE_CONNECT_START = 1

        /**连接完成*/
        const val STATE_CONNECT_SUCCESS = 2

        /**连接失败*/
        const val STATE_CONNECT_FAILURE = -1
    }
}
