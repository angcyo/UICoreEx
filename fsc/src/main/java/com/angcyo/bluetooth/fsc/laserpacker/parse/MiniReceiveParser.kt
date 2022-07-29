package com.angcyo.bluetooth.fsc.laserpacker.parse

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.IPacketParser
import com.angcyo.library.component.reader
import com.angcyo.library.component.toHexString
import com.angcyo.library.ex.toHexInt

/**
 * 最小8字节解析
 *
 * AA BB 08 01 01 00 00 00 00 00 02
 *
 * AA BB 08 05 02 01 00 00 00 00 08
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/26
 */
data class MiniReceiveParser(
    //收到指令的16进制字符
    var hex: String? = null,
    var length: Int = -1,
    var func: Byte = -1, //功能码
    var d1: Byte = -1,
    var d2: Byte = -1,
    var d3: Byte = -1,
    var d4: Byte = -1,
    var d5: Byte = -1,
    //额外的输出log
    var log: String? = null
) : IPacketParser<MiniReceiveParser> {
    override fun parse(packet: ByteArray): MiniReceiveParser? {
        return try {
            hex = packet.toHexString()
            packet.reader {
                offset(LaserPeckerHelper.packetHeadSize)//偏移头部

                length = readByte().toHexInt()
                func = readByte()

                d1 = readByte()
                d2 = readByte()
                d3 = readByte()
                d4 = readByte()
                d5 = readByte()
            }

            if (func == 0x00.toByte()) {
                //log =
            }
            this
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
