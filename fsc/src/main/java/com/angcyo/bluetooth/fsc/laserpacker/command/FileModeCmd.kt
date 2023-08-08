package com.angcyo.bluetooth.fsc.laserpacker.command

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.PACKET_FILE_HEAD_SIZE
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.checksum
import com.angcyo.bluetooth.fsc.laserpacker.parse.FileTransferParser
import com.angcyo.library.ex.padHexString
import com.angcyo.library.ex.removeAll
import com.angcyo.library.ex.toByteArray
import com.angcyo.library.ex.toHexString

/**
 * 文件传输模式指令, 进入数据传输模式.
 *
 * 300ms 下位机自动退出大数据模式。
 *
 * 最大的数据大小 30MB
 *
 * [FileTransferParser]
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/30
 */
data class FileModeCmd(
    /**需要传输的数据字节大小, 不包含[PACKET_FILE_HEAD_SIZE] */
    val dataSize: Int,
    //State = 0x01时为传输文件，data为传输文件数据字节总数。
    //State = 0x02时为传输结束。当文件传输完成时下位自动回复指令。
    //State = 0x04时为擦除所有文件。
    //State = 0x06时为擦除单个文件, data为指定文件索引。
    //State = 0x07时为擦除单个文件, fileName为指定文件名。
    val state: Byte = 0x1,
    val custom: Byte = 0, //自定义的数据
    //--2023-8-7
    val mount: Byte = 0,
    /**使用文件名, 删除sd/usb中的文件*/
    val fileName: String? = null,
) : BaseCommand() {

    companion object {

        /**删除所有历史文件*/
        fun deleteAllHistory(mount: Byte = QueryCmd.TYPE_SD.toByte()): FileModeCmd =
            FileModeCmd(0, 0x04, mount = mount)

        /**删除指定历史文件*/
        fun deleteHistory(index: Int, mount: Byte = QueryCmd.TYPE_SD.toByte()): FileModeCmd =
            FileModeCmd(index, 0x06, mount = mount)

        /**删除文件使用文件名[name]*/
        fun deleteHistory(name: String, mount: Byte = QueryCmd.TYPE_SD.toByte()): FileModeCmd =
            FileModeCmd(0, 0x07, mount = mount, fileName = name)
    }

    //功能码
    override fun commandFunc(): Byte = 0x05

    /**给足时间接收数据, 10分钟*/
    override fun getReceiveTimeout(): Long = if (state == 0x01.toByte()) {
        10 * 60_000
    } else {
        super.getReceiveTimeout()
    }

    override fun toByteArray(): ByteArray = commandByteWriter {
        writeUByte(commandFunc())
        writeUByte(state)
        when (state) {
            0x01.toByte() -> write(dataSize + PACKET_FILE_HEAD_SIZE, 4)
            0x02.toByte(), 0x04.toByte(), 0x06.toByte(), 0x07.toByte() -> write(dataSize, 4)
        }
        writeUByte(custom)
        writeUByte(mount)
        if (state == 0x07.toByte()) {
            fileName?.let {
                write(it)
                write(0)//结束字符
            }
        }
    }

    /**返回:
     * 2022-05-30 18:39:15.348
     * AA BB 08 05 01 01 00 00 00 00 07
     *
     * 2022-05-30 18:39:18.354
     * AA BB 08 05 02 00 00 00 00 00 07
     * */
    override fun toHexCommandString(): String {
        val dataLength = 0x09//16进制
        val data = buildString {
            append(commandFunc().toHexString())
            append(state.toHexString())
            when (state) {
                0x01.toByte() -> append((dataSize + PACKET_FILE_HEAD_SIZE).toHexString(8))
                0x06.toByte() -> append(dataSize.toByteArray(4).toHexString(false))
                else -> append(dataSize.toHexString(8))
            }
            append(custom.toHexString())
        }.padHexString(dataLength - LaserPeckerHelper.CHECK_SIZE)
        val check = data.checksum() //“功能码”和“数据内容”在内的校验和
        val cmd = "${LaserPeckerHelper.PACKET_HEAD} ${dataLength.toHexString()} $data $check"
        return cmd
    }

    override fun toCommandLogString(): String = buildString {
        when (state) {
            0x01.toByte() -> {
                append(toHexCommandString().removeAll())
                append(" 进入文件传输模式:数据大小:${dataSize}bytes state:$state")
            }

            0x02.toByte() -> append(" 传输结束! $dataSize")
            0x04.toByte() -> append(" 擦除所有文件!")
            0x06.toByte() -> append(" 擦除文件:$dataSize")
        }
    }
}
