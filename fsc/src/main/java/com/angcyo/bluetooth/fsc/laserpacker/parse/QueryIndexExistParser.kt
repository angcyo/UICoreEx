package com.angcyo.bluetooth.fsc.laserpacker.parse

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.IPacketParser
import com.angcyo.bluetooth.fsc.laserpacker.command.QueryCmd
import com.angcyo.library.component.reader

/**
 * 返回对应的索引是否存在
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/12/26
 */
data class QueryIndexExistParser(
    var func: Byte = 0, //功能码
    var state: Byte = 0, //0x02 表示数据传输结束回应。
    var custom: Byte = 0,
    var mount: Byte = 0,
    var res: Byte = 0, //1时表示接收成功，rev= 0时表示接收失败
) : IPacketParser<QueryIndexExistParser> {
    override fun parse(packet: ByteArray): QueryIndexExistParser? {
        return try {
            packet.reader {
                //AABB 08 05 01 00 0000000006
                offset(LaserPeckerHelper.packetHeadSize)//偏移头部
                offset(1)//偏移长度
                func = readByte()
                if (func != QueryCmd.QUERY_FUNC) {
                    throw IllegalStateException("非查询指令!")
                }
                state = readByte()
                custom = readByte()
                mount = readByte()
                res = readByte()
            }
            this
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**文件索引是否存在*/
    fun isIndexExist(): Boolean {
        return res == 0x01.toByte()
    }
}