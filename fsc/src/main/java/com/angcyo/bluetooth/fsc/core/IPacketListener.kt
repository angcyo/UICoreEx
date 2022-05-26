package com.angcyo.bluetooth.fsc.core

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/26
 */

/**数据包监听回调*/
interface IPacketListener {

    fun onPacketSend(address: String, strValue: String, data: ByteArray) {
    }

    fun onSendPacketProgress(address: String, percentage: Int, sendByte: ByteArray) {
    }

    fun onPacketReceived(
        address: String,
        strValue: String,
        dataHexString: String,
        data: ByteArray
    ) {

    }
}
