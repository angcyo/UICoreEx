package com.angcyo.bluetooth.fsc.laserpacker.parse

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.IPacketParser
import com.angcyo.library.component.reader

/**
 * 查询安全码与用户帐号
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/25
 */
data class DeviceSafeCodeParser(
    //Safe_code：占4个字节Data0为高字节。
    var safeCode: Int = 0,
    //Admin：用户帐号，占用40个字节。
    var account: String? = null,
    var custom: Int = -1,
    var state: Int = 0
) : IPacketParser<DeviceSafeCodeParser> {
    //解析数据
    override fun parse(packet: ByteArray): DeviceSafeCodeParser? {
        return try {
            packet.reader {
                offset(LaserPeckerHelper.packetHeadSize)//偏移头部
                offset(1)//偏移长度
                offset(1)//偏移功能码
                safeCode = readInt(4)
                account = readString(40, Charsets.US_ASCII)
                custom = readInt(1)
                state = readInt(1)
            }
            this
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
