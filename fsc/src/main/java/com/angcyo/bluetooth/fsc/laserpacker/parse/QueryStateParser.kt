package com.angcyo.bluetooth.fsc.laserpacker.parse

import com.angcyo.bluetooth.fsc.R
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.IPacketParser
import com.angcyo.library.component.reader
import com.angcyo.library.ex._string

/**
 * 蓝牙设备状态数据结构
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/25
 */
//DeviceStateParser(mode=2, workState=2, rate=0, laser=0, speed=0, error=0, state=0, name=0, temp=0, custom=0, zConnect=0, printTimes=1, angle=0)
data class QueryStateParser(
    //设备当前工作模式
    //L1当前工作模式，
    // 0x01为打印模式，
    // 0x02为打印预览模式，
    // 0x04为调焦模式，
    // 0x05为文件下载模式，
    // 0x06为空闲模式，
    // 0x07为关机状态，
    // 0x08为设置模式，
    // 0x09为下载模式。
    var mode: Int = WORK_MODE_IDLE,
    //当前模式下的工作状态, 暂停, 255:结束
    //空闲模式下: 0xff
    //打印模式下: 工作中0x01，暂停0x04，结束0x03 继续打印0x02
    //打印预览模式下: 0x01:预览图片 0x02:预览范围 0x04:预览暂停 0x06:支架调整 0x07:显示中心
    var workState: Int = 0,
    //打印进度百分比[0-100]
    var rate: Int = 0,
    //激光强度[1-100]
    var laser: Int = 0,
    //激光打印速度[1-100]
    var speed: Int = 0,
    //错误状态
    //0表示无错误状态
    //1表示不处于安全状态，且自由模式没开。
    //2打印超过边界报警
    //3激光工作温度报警
    //4打印过程中移动设备报警
    //5打印过程中遮挡激光报警
    //6打印数据错误
    //7文件编号查询错误
    //8陀螺仪自检错误
    //9flash自检错误
    var error: Int = 0,
    //发送查询指令时传递的state
    //0时表示查询工作状态
    //1时表示查询文件列表
    //2时表示查询设置状态
    //3时表示查询版本
    //4时表示查询安全码与用户帐号
    var state: Int = 0,
    //当前正在打印的文件编号(文本编号对应本地存储的文件信息)
    var name: Int = 0,
    //工作温度, 精度1度
    var temp: Int = 0,
    //发送指令时, 自定义的数据
    var custom: Int = -1,
    //Z轴连接状态, 0未连接, 1连接
    var zConnect: Int = 0,
    //当前图片打印的次数
    var printTimes: Int = 0,
    //设备与水平面的平角
    var angle: Int = 0,
    //雕刻模块识别位（C1专用位）
    //0 5W激光
    //1 10W激光
    //2 单色笔模式
    //3 刀切割模式
    //4 彩绘模式
    //5 CNC模式
    var moduleState: Int = -1
) : IPacketParser<QueryStateParser> {

    companion object {

        /**0x01为打印模式*/
        const val WORK_MODE_ENGRAVE = 0x01

        /**0x02为打印预览模式*/
        const val WORK_MODE_ENGRAVE_PREVIEW = 0x02

        /**0x03为结束预览模式*/
        const val WORK_MODE_STOP_ENGRAVE_PREVIEW = 0x03

        /**0x04为调焦模式*/
        const val WORK_MODE_FOCUSING = 0x04

        /**0x05为文件下载模式*/
        const val WORK_MODE_FILE_DOWNLOAD = 0x05

        /**0x06为空闲模式*/
        const val WORK_MODE_IDLE = 0x06

        /**0x07为关机状态*/
        const val WORK_MODE_SHUTDOWN = 0x07

        /**0x08为设置模式*/
        const val WORK_MODE_SETUP = 0x08

        /**0x09为下载模式(工厂)*/
        const val WORK_MODE_DOWNLOAD = 0x09
    }

    //解析数据
    override fun parse(packet: ByteArray): QueryStateParser? {
        return try {
            packet.reader {
                offset(LaserPeckerHelper.packetHeadSize)//偏移头部
                offset(1)//偏移长度
                offset(1)//偏移功能码
                mode = readInt(1)
                workState = readInt(1)
                rate = readInt(1)
                laser = readInt(1)
                speed = readInt(1)
                error = readInt(1)
                state = readInt(1)
                name = readInt(4)
                temp = readInt(1)
                custom = readInt(1)
                zConnect = readInt(1)
                printTimes = readInt(1)
                angle = readInt(1)
                moduleState = readInt(1)
            }
            this
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    //region ------Engrave------

    /**打印模式下, 打印是否暂停了*/
    fun isEngravePause(): Boolean = mode == WORK_MODE_ENGRAVE && workState == 0x04

    /**打印模式下, 打印是否正在打印*/
    fun isEngraving(): Boolean =
        mode == WORK_MODE_ENGRAVE && (workState == 0x01 || workState == 0x02)

    /**打印模式下, 打印是否停止了*/
    fun isEngraveStop(): Boolean = mode == WORK_MODE_ENGRAVE && workState == 0x03

    //endregion

    //region ------Mode------

    /**设备是否处于空闲模式*/
    fun isModeIdle() = mode == WORK_MODE_IDLE

    /**设备是否处于雕刻模式*/
    fun isModeEngrave() = mode == WORK_MODE_ENGRAVE

    /**雕刻预览模式*/
    fun isModeEngravePreview() = mode == WORK_MODE_ENGRAVE_PREVIEW

    //endregion
}

/**转换成模式状态文本*/
fun QueryStateParser.toDeviceStateString(): String? {
    val builder = StringBuilder()

    when (mode) {
        QueryStateParser.WORK_MODE_ENGRAVE -> {
            builder.append("镭雕")
            when (workState) {
                0x03 -> builder.append("结束")
                0x04 -> builder.append("暂停")
                else -> builder.append("中")
            }
        }
        QueryStateParser.WORK_MODE_ENGRAVE_PREVIEW -> {
            builder.append("预览")
            when (workState) {
                0x01 -> builder.append("图片")
                0x02 -> builder.append("范围")
                0x04 -> builder.append("第三轴暂停")
                0x05 -> builder.append("第三轴继续")
                0x06 -> builder.append("支架调整")
                0x07 -> builder.append("显示中心")
                else -> builder.append("中")
            }
        }
        QueryStateParser.WORK_MODE_FOCUSING -> builder.append("调焦模式")
        QueryStateParser.WORK_MODE_FILE_DOWNLOAD -> builder.append("文件下载模式")
        QueryStateParser.WORK_MODE_SHUTDOWN -> builder.append("关机状态")
        QueryStateParser.WORK_MODE_SETUP -> builder.append("设置模式")
        QueryStateParser.WORK_MODE_DOWNLOAD -> builder.append("下载模式")
    }

    if (builder.isEmpty()) {
        return null
    }

    //错误信息
    if (error != 0) {
        builder.appendLine()
        builder.append(error.toErrorStateString())
    }

    return builder.toString()
}

fun Int.toErrorStateString() = when (this) {
    1 -> _string(R.string.ex_tips_one)
    2 -> _string(R.string.ex_tips_two)
    3 -> _string(R.string.ex_tips_three)
    4 -> _string(R.string.ex_tips_four)
    5 -> _string(R.string.ex_tips_five)
    6 -> _string(R.string.ex_tips_six)
    7 -> _string(R.string.ex_tips_seven)
    8 -> _string(R.string.ex_tips_eight)
    9 -> _string(R.string.ex_tips_nine)
    else -> _string(R.string.ex_tips_six)
}