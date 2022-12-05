package com.angcyo.bluetooth.fsc.laserpacker.command

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.checksum
import com.angcyo.library.component.byteWriter
import com.angcyo.library.ex.*

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
    val index: Int = -1,//为将打印文件的索引。 文件编号 4字节
    val power: Byte = 0x64,//为当前打印激光强度.1 - 100，分100个等级。
    val depth: Byte = 0x03, //深度, 机器需要的是速度, 需要转换一下(101-)
    //val speed: Byte = 0x03,//0x0A,//0x32,//为当前打印速度。1 - 100，分100个等级。
    val state: Byte = 0x01,//0x01 从头开始打印文件，0x02继续打印文件，0x03结束打印，0x04暂停打印
    val x: Int = 0x0,//图片起始坐标。 2字节。 占位数据
    val y: Int = 0x0,//占位数据
    val time: Byte = 0x1,//打印次数
    val type: Byte = LaserPeckerHelper.LASER_TYPE_BLUE,//l_type：雕刻激光类型选择，0为1064nm激光 (白光-雕)，1为450nm激光 (蓝光-烧)。(L3max新增)
    val custom: Byte = 0x0,
    //雕刻物体直径, 如果是mm单位, 还需要乘以100
    val diameter: Int = 0,//雕刻物体直径（L4旋转轴生效），2字节,obj_d  = d * 100; d为物体实际尺寸单位为mm;
    //雕刻精度档位分：1至5档，它与系统加减速对应（C1新增）
    val precision: Int = 1,

    //---多文件批量雕刻---2022-11-30
    val indexNum: Int = 0, //是否是批量雕刻判断条件, 大于0则表示批量雕刻
    val bigIndex: Int = -1,//大索引
    val indexList: List<Int> = emptyList(),//文件索引列表
    val powerList: List<Byte> = emptyList(),//功率列表
    val depthList: List<Byte> = emptyList(),//深度列表
    val timeList: List<Byte> = emptyList(),//次数列表
) : BaseCommand() {

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

        /**深度转速度*/
        fun depthToSpeed(depth: Int) = clamp((101 - depth), 1, 100)

        /**速度转深度*/
        fun speedToDepth(speed: Int) = 101 - clamp(speed, 1, 100)

        /**批量雕刻文件指令
         * [bigIndex] 大索引*/
        fun batchEngrave(
            bigIndex: Int,
            indexList: List<Int>,
            powerList: List<Byte>,
            depthList: List<Byte>,
            timeList: List<Byte>,
            type: Byte,
            precision: Int,
            diameter: Int,
        ): EngraveCmd {
            return EngraveCmd(
                state = 0x05,
                custom = 0x08,
                type = type,
                precision = precision,
                diameter = diameter,
                bigIndex = bigIndex,
                indexNum = indexList.size(),
                indexList = indexList,
                powerList = powerList,
                depthList = depthList,
                timeList = timeList,
            )
        }
    }

    //功能码
    override fun commandFunc(): Byte = 0x01

    override fun toHexCommandString(): String {
        val check: String
        val cmd: String
        val data: String
        val dataLength: Int
        if (indexNum > 0) {
            //批量雕刻
            val bytes = byteWriter {
                write(commandFunc())
                write(state)
                write(custom)
                write(bigIndex, 4)
                write(indexNum)
                powerList.forEach {
                    write(it)
                }
                depthList.forEach {
                    write(depthToSpeed(it.toInt()))
                }
                timeList.forEach {
                    write(it)
                }
                indexList.forEach {
                    write(it, 4)
                }
                write(type)
                write(precision)
                write(diameter, 2)
            }
            data = bytes.toHexString(false)
            dataLength = bytes.size() + LaserPeckerHelper.CHECK_SIZE
            check = bytes.checksum()
        } else {
            dataLength = 0x14 //18 //数据长度
            data = buildString {
                append(commandFunc().toHexString())
                append(state.toHexString())
                append(power.toHexString())
                append(depthToSpeed(depth.toInt()).toHexString()) //打印速度
                //append(name.toHexString(8))
                append(index.toByteArray(4).toHexString(false))
                append(kotlin.math.max(x, 0).toHexString(4))
                append(kotlin.math.max(y, 0).toHexString(4))
                append(custom.toHexString())
                append(time.toHexString())
                append(type.toHexString())
                append(diameter.toByteArray(2).toHexString(false))
                append(precision.toHexString())
            }.padHexString(dataLength - LaserPeckerHelper.CHECK_SIZE)
            check = data.checksum() //“功能码”和“数据内容”在内的校验和
        }
        cmd = "${LaserPeckerHelper.PACKET_HEAD} ${dataLength.toHexString()} $data $check"
        return cmd
    }

    override fun toCommandLogString(): String = buildString {
        append(toHexCommandString().removeAll())
        when (state) {
            0x01.toByte() -> append(" 开始雕刻:文件:$index 功率:$power 深度:$depth 次数:$time state:$state x:$x y:$y type:$type 直径:${diameter} 加速级别:${precision}")
            0x02.toByte() -> append(" 继续雕刻!")
            0x03.toByte() -> append(" 停止雕刻!")
            0x04.toByte() -> append(" 暂停雕刻!")
            0x05.toByte() -> append(" 批量雕刻:大索引$bigIndex :$indexNum 索引:$indexList 功率:$powerList 深度:$depthList type:$type 直径:${diameter} 加速级别:${precision}")
        }
    }
}
