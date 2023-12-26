package com.angcyo.bluetooth.fsc.laserpacker.parse

import androidx.lifecycle.LifecycleOwner
import com.angcyo.bluetooth.fsc.WaitReceivePacket
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.IPacketParser
import com.angcyo.bluetooth.fsc.laserpacker.command.QueryCmd
import com.angcyo.bluetooth.fsc.listenerReceivePacket
import com.angcyo.bluetooth.fsc.parse
import com.angcyo.library.L
import com.angcyo.library.component.reader
import com.angcyo.library.ex.toAsciiInt
import com.angcyo.library.ex.toByteInt
import com.angcyo.library.ex.toHexString

/**
 * 查询文件名列表结果解析
 * [com.angcyo.bluetooth.fsc.laserpacker.command.QueryCmd.Companion.fileSdNameList]
 * [com.angcyo.bluetooth.fsc.laserpacker.command.QueryCmd.Companion.fileUsbNameList]
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/08/03
 */
data class QueryFileListParser(
    var state: Byte = -1,
    var custom: Byte = -1,
    /**
     * 当mount=0时查询U盘列表。
     * 当mount=1时查询SD卡文件列表。
     * */
    var mount: Byte = -1,
    /**索引列表
     * 和[nameList]是一一对应关系
     * */
    var indexList: List<Int>? = null,
    /**名称列表*/
    var nameList: List<String>? = null
) : IPacketParser<QueryFileListParser> {

    //解析数据
    override fun parse(packet: ByteArray): QueryFileListParser? {
        return try {
            packet.reader {
                //offset(LaserPeckerHelper.packetHeadSize)//偏移头部
                val head = read(LaserPeckerHelper.packetHeadSize).toHexString()
                val length = if (head == LaserPeckerHelper.PACKET_HEAD_BIG) {
                    //读取4个字节的长度
                    read(4).toByteInt()
                } else {
                    //读取1个字节的长度
                    readByte().toInt() //长度
                }

                val func = readByte()  //偏移功能码
                state = readByte() //状态位
                custom = readByte()
                mount = readByte()

                if (func != QueryCmd.workState.commandFunc()) {
                    throw IllegalStateException("非查询指令!")
                }

                val indexList = mutableListOf<Int>()
                val nameList = mutableListOf<String>()
                val list = readStringList(length - 2)
                for (item in list) {
                    if (item.length > 8) {
                        val index = item.substring(IntRange(0, 7)).toAsciiInt()
                        val name = item.substring(IntRange(8, item.length - 1))
                        indexList.add(index)
                        nameList.add(name)
                    } else {
                        L.w("无效的历史文件数据:${item}")
                    }
                }

                this@QueryFileListParser.indexList = indexList
                this@QueryFileListParser.nameList = nameList
            }
            this
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

/**监听设备返回的文件名列表*/
fun listenerFileList(
    lifecycleOwner: LifecycleOwner? = null,
    receiveTimeout: Long = 10 * 60 * 1_000,
    action: (parser: QueryFileListParser?, error: Exception?) -> Unit
): WaitReceivePacket {
    return listenerReceivePacket(receiveTimeout, lifecycleOwner) { receivePacket, bean, error ->
        try {
            if (!receivePacket._isFinish) {
                receivePacket.end()
            }
            val parser = bean?.parse<QueryFileListParser>()
            action(parser, error)
        } catch (e: Exception) {
            e.printStackTrace()
            action(null, e)
        }
    }.apply {
        dataHead = LaserPeckerHelper.PACKET_HEAD_BIG
    }
}