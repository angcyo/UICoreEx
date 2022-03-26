package com.angcyo.bluetooth.fsc.laserpacker.bean

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.IPacketParse
import com.angcyo.library.component.reader

/**
 * 最小8字节解析
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/26
 */
data class MiniReceiveBean(
    var func: Int = -1, //功能码
    var d1: Int = -1,
    var d2: Int = -1,
    var d3: Int = -1,
    var d4: Int = -1,
    var d5: Int = -1
) : IPacketParse<MiniReceiveBean> {
    override fun parse(packet: ByteArray): MiniReceiveBean? {
        return try {
            packet.reader {
                offset(LaserPeckerHelper.packetHeadSize)//偏移头部
                offset(1)//偏移长度
                //offset(1)//偏移功能码
                func = readInt(1)

                d1 = readInt(1)
                d2 = readInt(1)
                d3 = readInt(1)
                d4 = readInt(1)
                d5 = readInt(1)
            }
            this
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}
