package com.angcyo.bluetooth.fsc.laserpacker.command

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.checksum
import com.angcyo.library.ex.padHexString
import com.angcyo.library.ex.toHexString

/**
 * 打印指令
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/26
 */
data class PrintCmd(
    val state: Byte,//0x01 从头开始打印文件，0x02继续打印文件，0x03结束打印，0x04暂停打印
    val laser: Byte,//为当前打印激光强度分为0 - 5，分6个等级。
    val speed: Byte,//为当前打印速度。
    val name: Int,//为将打印文件名。 文件编号 4字节
    val x: Int = 0x0,//图片起始坐标。 2字节
    val y: Int = 0x0,
    val custom: Byte = 0x0,
    val time: Byte = 0x1//打印次数
) : IDeviceCommand {
    override fun toHexCommandString(): String {
        val dataLength = 0x10 //16 //数据长度
        val func = "01" //功能码
        val data = buildString {
            append(func)
            append(state.toHexString())
            append(laser.toHexString())
            append(speed.toHexString())
            append(name.toHexString(8))
            append(x.toHexString(4))
            append(y.toHexString(4))
            append(custom.toHexString())
            append(time.toHexString())
        }.padHexString(dataLength - LaserPeckerHelper.CHECK_SIZE)
        val check = data.checksum() //“功能码”和“数据内容”在内的校验和
        val cmd = "${LaserPeckerHelper.PACKET_HEAD} ${dataLength.toHexString()} $data $check"
        return cmd
    }
}
