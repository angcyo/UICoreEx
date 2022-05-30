package com.angcyo.bluetooth.fsc.laserpacker.parse

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.IPacketParser
import com.angcyo.library.component.reader

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/26
 */
data class EngraveReceiveParser(
    var func: Int = -1, //功能码
    var state: Int = 0,
    var custom: Int = -1,
    //预留位置
    var d1: Int = -1,
    var d2: Int = -1,
    var d3: Int = -1,
) : IPacketParser<EngraveReceiveParser> {
    override fun parse(packet: ByteArray): EngraveReceiveParser? {
        return try {
            packet.reader {
                offset(LaserPeckerHelper.packetHeadSize)//偏移头部
                offset(1)//偏移长度
                //offset(1)//偏移功能码
                func = readInt(1)
                state = readInt(1)
                custom = readInt(1)
                d1 = readInt(1)
                d2 = readInt(1)
                d3 = readInt(1)
            }
            this
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
