package com.angcyo.bluetooth.fsc.laserpacker.parse

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.IPacketParser
import com.angcyo.library.component.reader

/**
 * 打印的文件历史记录
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/25
 */
data class QueryEngraveFileParser(
    //文件数量
    var num: Int = 0,
    //文件编号列表
    var nameList: List<Int>? = null,
    //发送指令时, 自定义的数据
    var custom: Int = -1,
    //发送查询指令时传递的state
    //0时表示查询工作状态
    //1时表示查询文件列表
    //2时表示查询设置状态
    //3时表示查询版本
    //4时表示查询安全码与用户帐号
    var state: Int = 0,
) : IPacketParser<QueryEngraveFileParser> {
    //解析数据
    override fun parse(packet: ByteArray): QueryEngraveFileParser? {
        return try {
            packet.reader {
                offset(LaserPeckerHelper.packetHeadSize)//偏移头部
                offset(1)//偏移长度
                offset(1)//偏移功能码
                num = readInt(1)

                if (num > 0) {
                    val list = mutableListOf<Int>()
                    for (i in 0 until num) {
                        list.add(readInt(4))
                    }
                    nameList = list
                }
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