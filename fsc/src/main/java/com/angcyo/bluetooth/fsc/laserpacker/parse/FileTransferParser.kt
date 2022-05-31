package com.angcyo.bluetooth.fsc.laserpacker.parse

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.IPacketParser
import com.angcyo.library.component.reader

/**
 * 进入大数据模式, 传输完数据之后, 返回的数据格式
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/30
 */
data class FileTransferParser(
    var func: Byte = 0, //功能码
    var state: Byte = 0, //0x02 表示数据传输结束回应。
    var rev: Byte = 0, //1时表示接收成功，rev= 0时表示接收失败
) : IPacketParser<FileTransferParser> {

    override fun parse(packet: ByteArray): FileTransferParser? {
        return try {
            packet.reader {
                offset(LaserPeckerHelper.packetHeadSize)//偏移头部
                offset(1)//偏移长度
                func = readByte()
                state = readByte()
                rev = readByte()
            }
            this
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**是否成功进入文件传输模式*/
    fun isIntoFileMode(): Boolean {
        return func == 0x05.toByte() && state == 0x01.toByte() && rev == 0x01.toByte()
    }

    /**文件传输是否成功完成*/
    fun isFileTransferSuccess(): Boolean {
        return func == 0x05.toByte() && state == 0x02.toByte() && rev == 0x01.toByte()
    }
}