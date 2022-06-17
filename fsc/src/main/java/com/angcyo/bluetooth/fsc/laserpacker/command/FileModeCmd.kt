package com.angcyo.bluetooth.fsc.laserpacker.command

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.PACKET_FILE_HEAD_SIZE
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.checksum
import com.angcyo.library.ex.padHexString
import com.angcyo.library.ex.removeAll
import com.angcyo.library.ex.toHexString

/**
 * 文件传输模式指令, 进入数据传输模式.
 *
 * 300ms 下位机自动退出大数据模式。
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/30
 */
data class FileModeCmd(
    /**需要传输的数据字节大小, 不包含[PACKET_FILE_HEAD_SIZE] */
    val dataSize: Int,
    //State = 0x01时为传输文件，data为传输文件数据字节总数。
    //State = 0x02时为传输结束。当文件传输完成时下位自动回复指令。
    val state: Byte = 0x1,
    val custom: Byte = 0 //自定义的数据
) : ICommand {

    //功能码
    override fun commandFunc(): Byte = 0x05

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
            append((dataSize + PACKET_FILE_HEAD_SIZE).toHexString(8))
            append(custom.toHexString())
        }.padHexString(dataLength - LaserPeckerHelper.CHECK_SIZE)
        val check = data.checksum() //“功能码”和“数据内容”在内的校验和
        val cmd = "${LaserPeckerHelper.PACKET_HEAD} ${dataLength.toHexString()} $data $check"
        return cmd
    }

    override fun toCommandLogString(): String = buildString {
        append(toHexCommandString().removeAll())
        append(" 进入文件传输模式:数据大小:${dataSize}bytes state:$state")
    }
}
