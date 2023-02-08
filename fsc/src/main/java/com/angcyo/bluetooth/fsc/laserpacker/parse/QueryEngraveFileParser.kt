package com.angcyo.bluetooth.fsc.laserpacker.parse

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.IPacketParser
import com.angcyo.bluetooth.fsc.laserpacker.command.QueryCmd
import com.angcyo.library.component.reader
import com.angcyo.library.ex.size

/**AABB82001E0009A67B924FEFF1000ADC0B000AE0A50002369D0003D00F0009872C000B7515000B98B6000BF395000E316C000E475F000E57AA000E660B000E7696000DA6BF000DB07F000DB0EA000991CF000B445F0000754A000076EF0000787AFFFFFFFF0009930F000996A4000997C40009984100099F4D0009A27E0009A4DE000125E0
 * 打印的文件历史记录
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/25
 */
data class QueryEngraveFileParser(
    //文件数量
    var num: Int = 0,
    //文件编号列表
    var indexList: List<Int>? = null,
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
                val func = readByte()//  offset(1)//偏移功能码

                if (func != QueryCmd.workState.commandFunc()) {
                    throw IllegalStateException("非查询指令!")
                }

                num = readInt(1)

                val list = mutableListOf<Int>()
                indexList = list
                for (i in 0 until 30) {
                    val nameIndex = readByteInt(4)
                    if (nameIndex > 0 && list.size() < num) {
                        list.add(nameIndex)
                    }
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