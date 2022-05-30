package com.angcyo.bluetooth.fsc.laserpacker.command

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.checksum
import com.angcyo.library.ex.padHexString
import com.angcyo.library.ex.toHexString

/**
 * 退出指令, 退出机器的工作模式
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/30
 */
data class ExitCmd(
    val custom: Byte = 0 //自定义的数据
) : ICommand {

    override fun toHexCommandString(): String {
        val dataLength = 8 //数据长度
        val func = "FF" //功能码
        val data = buildString {
            append(func)
            append(custom.toHexString())
        }.padHexString(dataLength - LaserPeckerHelper.CHECK_SIZE)
        val check = data.checksum() //“功能码”和“数据内容”在内的校验和
        val cmd = "${LaserPeckerHelper.PACKET_HEAD} ${dataLength.toHexString()} $data $check"
        return cmd
    }

}
