package com.angcyo.bluetooth.fsc.laserpacker.parse

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.IPacketParser
import com.angcyo.bluetooth.fsc.laserpacker.command.QueryCmd
import com.angcyo.library.component.reader

/**
 * 查询设备名结果解析
 * [com.angcyo.bluetooth.fsc.laserpacker.command.QueryCmd.Companion.deviceName]
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/08/03
 */
data class QueryDeviceNameParser(
    var custom: Int = -1,
    /**设备蓝牙名*/
    var deviceName: String? = null
) : IPacketParser<QueryDeviceNameParser> {

    //解析数据
    override fun parse(packet: ByteArray): QueryDeviceNameParser? {
        return try {
            packet.reader {
                offset(LaserPeckerHelper.packetHeadSize)//偏移头部
                val length = readByte() //长度
                val func = readByte()  //偏移功能码
                val state = readByte() //状态位
                val custom = readByte() //状态位

                if (func != QueryCmd.workState.commandFunc()) {
                    throw IllegalStateException("非查询指令!")
                }

                val list = readStringList(length - 2)
                deviceName = list.firstOrNull()
            }
            this
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}
