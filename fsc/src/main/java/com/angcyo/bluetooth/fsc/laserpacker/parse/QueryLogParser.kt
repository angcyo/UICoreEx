package com.angcyo.bluetooth.fsc.laserpacker.parse

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.IPacketParser
import com.angcyo.library.component.reader

/**
 * 查询日志
 * AABB5500060000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/11/08
 */
data class QueryLogParser(
    var length: Int = 0,
    var state: Int = 0,
    var custom: Int = -1,
    var log: String? = null
) : IPacketParser<QueryLogParser> {
    override fun parse(packet: ByteArray): QueryLogParser? {
        return try {
            packet.reader {
                offset(LaserPeckerHelper.packetHeadSize)//偏移头部
                length = readInt(1)//长度
                offset(1)//偏移功能码
                state = readInt(1)
                custom = readInt(1)
                log = readString(
                    length
                            - 2 //去掉检验和
                            - 1 //去掉功能码
                            - 1 //去掉状态码
                            - 1 //去掉custom
                    ,
                    Charsets.US_ASCII
                )
            }
            this
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}