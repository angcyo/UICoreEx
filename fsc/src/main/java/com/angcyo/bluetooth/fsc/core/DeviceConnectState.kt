package com.angcyo.bluetooth.fsc.core

import android.bluetooth.BluetoothGatt
import com.feasycom.common.bean.ConnectType
import com.feasycom.common.bean.FscDevice

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/26
 */

/**连接状态*/
data class DeviceConnectState(
    val device: FscDevice,
    var state: Int = CONNECT_STATE_NORMAL,
    var gatt: BluetoothGatt? = null,
    var type: ConnectType? = null,
    var exception: Exception? = null,
    /**是否是主动断开*/
    var isActiveDisConnected: Boolean = false, //主动断开连接
    /**是否是自动连接*/
    var isAutoConnect: Boolean = false, //自动连接, 用来弹出连接成功后的界面
    var connectTime: Long = 0L, //触发连接的时间, 13位时间戳, 毫秒
    var connectedTime: Long = 0L, //连接上的时间, 13位时间戳, 毫秒
    var disconnectTime: Long = 0L, //断开请求的开始时间, 13位时间戳, 毫秒
) {
    companion object {
        const val CONNECT_STATE_NORMAL = 0
        const val CONNECT_STATE_START = 1 //开始连接
        const val CONNECT_STATE_SUCCESS = 2 //连接成功
        const val CONNECT_STATE_FAIL = 3
        const val CONNECT_STATE_DISCONNECT_START = 4 //开始断开连接
        const val CONNECT_STATE_DISCONNECT = 5 //断开成功
    }
}