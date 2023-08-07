package com.angcyo.bluetooth.fsc.laserpacker.parse

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.IPacketParser
import com.angcyo.bluetooth.fsc.laserpacker.command.QueryCmd
import com.angcyo.library.component.reader

/**
 * [com.angcyo.bluetooth.fsc.laserpacker.command.QueryCmd.Companion.fileSdNameList]
 * [com.angcyo.bluetooth.fsc.laserpacker.command.QueryCmd.Companion.fileUsbNameList]
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/08/07
 */
data class QueryFileNameParser(
    var func: Byte = 0,
    var state: Byte = 0,
    var custom: Byte = 0,
    var mount: Byte = 0,
) : IPacketParser<QueryFileNameParser> {
    override fun parse(packet: ByteArray): QueryFileNameParser? {
        return try {
            packet.reader {
                offset(LaserPeckerHelper.packetHeadSize)//偏移头部
                val length = readByte() //长度
                func = readByte()

                if (func != QueryCmd.workState.commandFunc()) {
                    throw IllegalStateException("非查询指令!")
                }

                state = readByte()
                custom = readByte()
                mount = readByte()
            }
            this
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

