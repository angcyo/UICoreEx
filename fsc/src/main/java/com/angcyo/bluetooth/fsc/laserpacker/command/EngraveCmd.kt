package com.angcyo.bluetooth.fsc.laserpacker.command

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.checksum
import com.angcyo.library.ex.padHexString
import com.angcyo.library.ex.removeAll
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
    val name: Int = -1,//为将打印文件名。 文件编号 4字节
    val laser: Byte = 0x64,//为当前打印激光强度.1 - 100，分100个等级。
    val depth: Byte = 0x03, //深度, 机器需要的是速度, 需要转换一下(100-)
    //val speed: Byte = 0x03,//0x0A,//0x32,//为当前打印速度。1 - 100，分100个等级。
    val state: Byte = 0x01,//0x01 从头开始打印文件，0x02继续打印文件，0x03结束打印，0x04暂停打印
    val x: Int = 0x0,//图片起始坐标。 2字节
    val y: Int = 0x0,
    val time: Byte = 0x1,//打印次数
    val type: Byte = 0x0,//l_type：雕刻激光类型选择，0为1064nm激光 (白光)，1为450nm激光 (蓝光)。(L3max新增)
    val custom: Byte = 0x0,
) : ICommand {

    companion object {

        /**继续打印指令*/
        fun continueEngrave(): EngraveCmd {
            return EngraveCmd(state = 0x02)
        }

        /**暂停打印指令*/
        fun pauseEngrave(): EngraveCmd {
            return EngraveCmd(state = 0x04)
        }

        /**停止打印指令*/
        fun stopEngrave(): EngraveCmd {
            return EngraveCmd(state = 0x03)
        }
    }

    //功能码
    override fun commandFunc(): Byte = 0x01

    override fun toHexCommandString(): String {
        val dataLength = 0x11 //16 //数据长度
        val data = buildString {
            append(commandFunc().toHexString())
            append(state.toHexString())
            append(laser.toHexString())
            append((100 - depth).toHexString()) //打印速度
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

    override fun toCommandLogString(): String = buildString {
        append(toHexCommandString().removeAll())
        when (state) {
            0x01.toByte() -> append(" 开始雕刻:$name 激光强度:$laser 深度:$depth 次数:$time state:$state x:$x y:$y type:$type")
            0x02.toByte() -> append(" 继续雕刻!")
            0x03.toByte() -> append(" 停止雕刻!")
            0x04.toByte() -> append(" 暂停雕刻!")
        }

    }
}
