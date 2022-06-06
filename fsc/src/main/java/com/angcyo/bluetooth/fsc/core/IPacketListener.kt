package com.angcyo.bluetooth.fsc.core

import androidx.annotation.WorkerThread

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/26
 */

/**数据包监听回调*/
@WorkerThread
interface IPacketListener {

    @WorkerThread
    fun onPacketSend(
        packetProgress: DevicePacketProgress,
        address: String,
        strValue: String,
        data: ByteArray
    ) {
    }

    @WorkerThread
    fun onSendPacketProgress(
        packetProgress: DevicePacketProgress,
        address: String,
        percentage: Int,
        sendByte: ByteArray
    ) {
    }

    @WorkerThread
    fun onPacketReceived(
        address: String,
        strValue: String,
        dataHexString: String,
        data: ByteArray
    ) {

    }
}
