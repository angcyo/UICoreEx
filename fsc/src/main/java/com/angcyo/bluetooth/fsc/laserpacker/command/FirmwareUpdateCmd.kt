package com.angcyo.bluetooth.fsc.laserpacker.command

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.checksum
import com.angcyo.library.ex.padHexString
import com.angcyo.library.ex.removeAll
import com.angcyo.library.ex.toByteArray
import com.angcyo.library.ex.toHexString

/**
 * 固件更新执行, 发送此指令, 进入下载模式
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/08
 */
data class FirmwareUpdateCmd(
    /**
    func：下载功能码，0进入下载模式， 1数据传输完成。
    当func = 0x00时，进入固件升级模式，Number固件数据字节总数，占用4个节数，用于校验固件下载是否完	整。SW_Version为固件版本号，占用两个字节。此时上位机等待回应。下位机作出回应后，进入大数据接收状	态并开始计时，当无数据时间超过3秒钟，下位机会自动退出大数据接收状态，并较验数据完整。
    当func = 0x01时，完成数据传输，当上位机数据传输完成后，并等待下位回应，传输数据完整，rev位为0，	不完整为1。下位机回应完成后，自动重启系统。
     * */
    val func: Byte,
    /**固件数据字节总数, 4字节*/
    val number: Int,
    /**固件版本号, 2字节*/
    val version: Int,
    val custom: Byte = 0x00,
    /**CRC16校验和*/
    val crc16: Int = 0x0,
) : BaseCommand() {

    companion object {
        /**固件更新指令*/
        fun update(number: Int, version: Int, crc16: Int = 0x0): FirmwareUpdateCmd {
            return FirmwareUpdateCmd(0x00, number, version, crc16 = crc16)
        }
    }

    /**10秒超时*/
    override fun getReceiveTimeout(): Long {
        return 10_000
    }

    override fun commandFunc(): Byte {
        return 0xdd.toByte()
    }

    override fun toHexCommandString(): String {
        val dataLength: Byte = 0x0B + 2 //数据长度
        val data = buildString {
            append(commandFunc().toHexString())
            append(func.toHexString())
            append(number.toByteArray(4).toHexString(false))
            append(version.toByteArray(2).toHexString(false))
            append(crc16.toByteArray(2).toHexString(false)) //2023-12-14
            append(custom.toHexString())
        }.padHexString(dataLength - LaserPeckerHelper.CHECK_SIZE)
        val check = data.checksum() //“功能码”和“数据内容”在内的校验和
        val cmd =
            "${LaserPeckerHelper.PACKET_HEAD} ${dataLength.toHexString()} $data $check"
        return cmd
    }

    override fun toCommandLogString(): String = buildString {
        append(toHexCommandString().removeAll())
        when (func) {
            0x00.toByte() -> {
                append(" 进入固件升级模式")
                append(" 数据大小:${number} bytes")
                append(" 版本:${version}")
            }

            0x01.toByte() -> append(" 完成数据传输!")
        }
    }
}
