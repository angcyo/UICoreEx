package com.angcyo.bluetooth.fsc.laserpacker.parse

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.FirmwareUpdateCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.IPacketParser
import com.angcyo.library.component.reader

/**
 * 固件升级指令, 返回的数据解析结构
 *
 * [AA BB 08 DD 00 00 00 00 00 00 DD ]
 *
 * [AA BB 08 DD 01 01 00 00 00 00 DF ]
 *                 △ 1表示接收不完整
 *
 * [AA BB 08 DD 01 00 00 00 00 00 DE]
 *                 △ 0表示成功
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/08
 */
data class FirmwareUpdateParser(
    /**功能码 0xdd*/
    var func: Byte = -1,
    /**下载功能码 0x00 or 0x01*/
    var func2: Byte = -1,
    /**指令接受状态，
     * 0表示成功。
     * rev为1表示固件文件接收不完整，下载失败，
     * 大于1表示不能执行该指令。*/
    var rev: Byte = -1,
    var custom: Byte = -1,
) : IPacketParser<FirmwareUpdateParser> {
    override fun parse(packet: ByteArray): FirmwareUpdateParser? {
        return try {
            packet.reader {
                offset(LaserPeckerHelper.packetHeadSize)//偏移头部
                offset(1)//偏移长度
                func = readByte() //offset(1)//偏移功能码

                if (func != FirmwareUpdateCmd.update(0, 0).commandFunc()) {
                    throw IllegalStateException("非固件升级指令!")
                }

                func2 = readByte()
                rev = readByte()
                custom = readByte()
            }
            this
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**是否固件升级完成*/
    fun isUpdateFinish(): Boolean = func2 == 0x01.toByte() && rev == 0x00.toByte()

}

