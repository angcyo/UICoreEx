package com.angcyo.bluetooth.fsc.laserpacker.parse

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.IPacketParser
import com.angcyo.library.component.reader

/**
 * 支架返回指令解析
 *
 *  AA BB 08 02 06 00 01 00 00 00 09
 *  AA BB 08 02 06 00 00 00 00 00 08
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/01
 */
data class BracketParser(
    var func: Byte = 0, //功能码
    var state: Byte = 0,
    var custom: Byte = 0,
    //当state为0x06电动架升降指令时，返回指令中的res表示电动支架的连接状态，当res = 0x01时，表示已经连接，0为未连接。
    var res: Byte = 0
) : IPacketParser<BracketParser> {
    override fun parse(packet: ByteArray): BracketParser? {
        return try {
            packet.reader {
                offset(LaserPeckerHelper.packetHeadSize)//偏移头部
                offset(1)//偏移长度
                func = readByte() //偏移功能码
                state = readByte()
                custom = readByte()
                res = readByte()
            }
            this
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**支架是否连接*/
    fun isBracketConnected() = res == 0x01.toByte()
}
