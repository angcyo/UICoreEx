package com.angcyo.bluetooth.fsc.laserpacker.command

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.checksum
import com.angcyo.library.ex.padHexString
import com.angcyo.library.ex.toByteArray
import com.angcyo.library.ex.toHexString

/**
 * 雕刻/打印指令
 *
 * 雕刻指令返回:
 * AA BB 08 01 01 00 00 00 00 00 02
 *
 * 雕刻结束返回: 当L1接收到退出指令时，L1退出当前工作模式，进入空闲状态。
 * AA BB 08 FF 01 64 32 62 95 02 8D
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/26
 */
data class EngraveCmd(
    val name: Int,//为将打印文件名。 文件编号 4字节
    val laser: Byte = 0x64,//为当前打印激光强度.1 - 100，分100个等级。
    val speed: Byte = 0x03,//0x0A,//0x32,//为当前打印速度。1 - 100，分100个等级。
    val state: Byte = 0x01,//0x01 从头开始打印文件，0x02继续打印文件，0x03结束打印，0x04暂停打印
    val x: Int = 0x0,//图片起始坐标。 2字节
    val y: Int = 0x0,
    val custom: Byte = 0x0,
    val time: Byte = 0x1,//打印次数
    val type: Byte = 0x0,//l_type：雕刻激光类型选择，0为1064nm激光 (白光)，1为450nm激光 (蓝光)。(L3max新增)
) : ICommand {
    override fun toHexCommandString(): String {
        val dataLength = 0x11 //16 //数据长度
        val func = "01" //功能码
        val data = buildString {
            append(func)
            append(state.toHexString())
            append(laser.toHexString())
            append(speed.toHexString())
            //append(name.toHexString(8))
            append(name.toByteArray(4).toHexString(false))
            append(x.toHexString(4))
            append(y.toHexString(4))
            append(custom.toHexString())
            append(time.toHexString())
            append(type.toHexString())
        }.padHexString(dataLength - LaserPeckerHelper.CHECK_SIZE)
        val check = data.checksum() //“功能码”和“数据内容”在内的校验和
        val cmd = "${LaserPeckerHelper.PACKET_HEAD} ${dataLength.toHexString()} $data $check"
        return cmd
    }
}
