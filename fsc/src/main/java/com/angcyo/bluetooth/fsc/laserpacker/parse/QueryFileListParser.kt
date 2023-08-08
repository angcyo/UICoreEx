package com.angcyo.bluetooth.fsc.laserpacker.parse

import androidx.lifecycle.LifecycleOwner
import com.angcyo.bluetooth.fsc.WaitReceivePacket
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.IPacketParser
import com.angcyo.bluetooth.fsc.laserpacker.command.QueryCmd
import com.angcyo.bluetooth.fsc.listenerReceivePacket
import com.angcyo.bluetooth.fsc.parse
import com.angcyo.library.component.reader
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
    var custom: Int = -1,
    /**
     * 当mount=0时查询U盘列表。
     * 当mount=1时查询SD卡文件列表。
     * */
    var mount: Int = -1,
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
                val state = readByte() //状态位
                val custom = readByte() //状态位

                if (func != QueryCmd.workState.commandFunc()) {
                    throw IllegalStateException("非查询指令!")
                }

                val list = readStringList(length - 2)
                nameList = list
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
            //no op
        }
    }.apply {
        dataHead = LaserPeckerHelper.PACKET_HEAD_BIG
    }
}