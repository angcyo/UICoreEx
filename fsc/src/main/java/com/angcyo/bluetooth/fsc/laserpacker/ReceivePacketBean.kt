package com.angcyo.bluetooth.fsc.laserpacker

/**
 * 接收包的数据
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/25
 */
class ReceivePacketBean {

    /**设备地址*/
    var address: String = ""

    //<editor-fold desc="send">

    /**发送的总数据*/
    var sendPacket: ByteArray = byteArrayOf()

    /**发了多少个包*/
    var sendPacketCount: Int = 0

    /**发送进度[0-100]*/
    var sendPacketPercentage: Int = -1

    /**发送的时间, 毫秒*/
    var sendStartTime: Long = -1

    /**发送完成的时间, 毫秒*/
    var sendFinishTime: Long = -1

    //</editor-fold desc="send">

    //<editor-fold desc="receive">

    /**解析到的数据长度, 包含校验位*/
    var receiveDataLength: Int = -1

    /**接收的时间, 毫秒*/
    var receiveStartTime: Long = -1

    /**接收完成的时间, 毫秒*/
    var receiveFinishTime: Long = -1

    /**接收的总数据*/
    var receivePacket: ByteArray = byteArrayOf()

    /**接收了多少个包*/
    var receivePacketCount: Int = 0

    //</editor-fold desc="receive">
}