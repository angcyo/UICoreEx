package com.angcyo.bluetooth.fsc.core

import android.bluetooth.BluetoothGatt
import androidx.annotation.MainThread
import com.feasycom.common.bean.ConnectType

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/07/31
 */
@MainThread
interface ICallbackListener {

    /**设备连接成功回调
     * [com.angcyo.bluetooth.fsc.FscBleApiModel._peripheralConnected]*/
    fun onPeripheralConnected(address: String, gatt: BluetoothGatt?, type: ConnectType) {}

    /**[com.angcyo.bluetooth.fsc.FscBleApiModel._peripheralDisconnected]*/
    fun onPeripheralDisconnected(address: String, gatt: BluetoothGatt?) {}
}