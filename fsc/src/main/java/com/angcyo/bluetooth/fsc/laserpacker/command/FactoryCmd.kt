package com.angcyo.bluetooth.fsc.laserpacker.command

import android.graphics.Point
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.library.annotation.MM
import com.angcyo.library.annotation.Pixel
import com.angcyo.library.ex.toDC

/**
 * 出厂设置指令
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/06/25
 */
data class FactoryCmd(

    /**状态码*/
    var state: Byte = 0x0,
    var custom: Byte = 0x0,

    /**数据索引, 完成矫正指令*/
    var index: Int = 0,

    /**跳到指定ad点*/
    @Pixel
    var adX: Int = 0,
    var adY: Int = 0,
    var laser: Byte = 0x0, //激光功率 （范围0 - 255）
    /**[com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd.type]*/
    var type: Byte = LaserPeckerHelper.LASER_TYPE_BLUE, //激光类型

    /**跳到执行坐标*/
    var x: Int = 0,
    var y: Int = 0,

    /**data1=0时为计算数据，data1=1时使用校正数据*/
    var data1: Byte = 0x1,
    var data2: Byte = 0x0,
    var data3: Byte = 0x0,

    /**data4*/
    var data4: Byte = 0x0,

    /**[pointList] mm单位, 精度需要手动乘以10后发送来*/
    var pointList: List<Point> = emptyList(),
) : BaseCommand() {

    companion object {

        /**无矫正范围预览*/
        fun noAdjustRangePreviewCmd(): FactoryCmd {
            return FactoryCmd(state = 0x05)
        }

        /**较正数据传输完成*/
        fun finishAdjustDataCmd(index: Int): FactoryCmd {
            return FactoryCmd(state = 0x08, index = index).apply {
                receiveTimeout = 5 * 60 * 1000 //5分钟
            }
        }

        /**校正数据使能
         * [boolean] 是否使用矫正数据*/
        fun useAdjustData(boolean: Boolean = true): FactoryCmd {
            return FactoryCmd(state = 0x10, data1 = if (boolean) 0x1 else 0x0)
        }

        /**激光点跳至指定AD值
         * [laser] 激光功率 （范围0 - 255）
         * [type] 激光类型
         * */
        fun jumpToAdCmd(adX: Int, adY: Int, laser: Byte, type: Byte): FactoryCmd {
            return FactoryCmd(state = 0x09, adX = adX, adY = adY, laser = laser, type = type)
        }

        /**激光点跳到指定坐标*/
        fun jumpToCoordCmd(x: Int, y: Int): FactoryCmd {
            return FactoryCmd(state = 0x0A, x = x, y = y)
        }

        /**[pointList] mm单位, 精度需要手动乘以10后发送来*/
        fun jumpToCoordCmd(
            pwr: Byte,
            type: Byte,
            @MM pointList: List<Point>
        ): FactoryCmd {
            return FactoryCmd(state = 0x0A, laser = pwr, type = type, pointList = pointList)
        }

        /**激光点预览功率设置*/
        fun previewPowerSettingCmd(laser: Byte, type: Byte): FactoryCmd {
            return FactoryCmd(state = 0x0B, laser = laser, type = type)
        }

        /**LX1 出厂雕刻设置指令*/
        fun factoryPCTCmd(enable: Boolean): FactoryCmd {
            return FactoryCmd(state = 0x21, data4 = if (enable) 0x1 else 0x0)
        }

        /**LX1 黑白/抖动图片补偿设置*/
        fun factoryCompensateCmd(bwc: Byte, dc: Byte, enable: Boolean): FactoryCmd {
            return FactoryCmd(
                state = 0x22,
                data1 = bwc,
                data2 = dc,
                data3 = if (enable) 0x1 else 0x0
            )
        }

        /**保存对焦光标坐标值（L5有用）*/
        fun saveFocusCmd(): FactoryCmd {
            return FactoryCmd(state = 0x11)
        }
    }

    override fun toByteArray(): ByteArray {
        return commandByteWriter {
            writeUByte(commandFunc())
            writeUByte(state)

            when (state) {
                0x05.toByte() -> write(0, 4)//补齐4个字节
                0x08.toByte() -> {
                    write(index, 4) //数据索引
                    writeUByte(custom)
                }

                0x09.toByte() -> {
                    write(adX, 2)
                    write(adY, 2)
                    writeUByte(custom)
                    writeUByte(laser)
                    writeUByte(type)
                }

                0x0A.toByte() -> {
                    if (pointList.isEmpty()) {
                        write(x, 2)
                        write(y, 2)
                        writeUByte(custom)
                    } else {
                        writeUByte(laser)
                        writeUByte(type)
                        write(pointList.size)
                        pointList.forEach {
                            write(it.x, 2)
                            write(it.y, 2)
                        }
                        writeUByte(custom)
                    }
                }

                0x0B.toByte() -> {
                    writeUByte(laser)
                    writeUByte(type)
                    write(0, 2)//补齐2个字节
                    writeUByte(custom)
                }

                0x10.toByte() -> {
                    writeUByte(data1)
                    writeUByte(0)
                    writeUByte(custom)
                    writeUByte(0)
                }

                0x21.toByte() -> {
                    writeUByte(0)
                    writeUByte(0)
                    writeUByte(custom)
                    writeUByte(data4)
                }

                0x22.toByte() -> {
                    writeUByte(data1)
                    writeUByte(data2)
                    writeUByte(data3)
                    writeUByte(custom)
                }
            }
        }
    }

    /**超时时长, 毫秒*/
    var receiveTimeout: Long? = null

    override fun getReceiveTimeout(): Long {
        return receiveTimeout ?: super.getReceiveTimeout()
    }

    /**出厂设置：功能码为0x0f*/
    override fun commandFunc(): Byte = 0x0f

    override fun toHexCommandString(): String {
        return super.toHexCommandString()
    }

    override fun toCommandLogString(): String {
        return buildString {
            append("出厂设置指令:")
            val laser = laser.toUByte().toInt()
            when (state) {
                0x05.toByte() -> append("无较正范围预览")
                0x08.toByte() -> append("较正数据传输完成[$index]")
                0x09.toByte() -> append("激光点跳至指定AD值:x:${adX} y:${adY} laser:$laser type:${type}")
                0x0A.toByte() -> {
                    if (pointList.isEmpty()) {
                        append("激光点跳到指定坐标:x:${x} y:${y}")
                    } else {
                        append("激光点跳到指定坐标[${pointList.size}]: laser:$laser type:${type}")
                        pointList.forEach {
                            append(" (x:${it.x} y:${it.y})")
                        }
                    }
                }

                0x0B.toByte() -> append("激光点预览功率设置:laser:$laser type:${type}")
                0x10.toByte() -> append("校正数据使能:${if (data1 == 0x1.toByte()) "校正数据" else "计算数据"}")
                0x11.toByte() -> append("保存对焦光标坐标值")
                0x21.toByte() -> append("LX1出厂雕刻设置:${(data4 == 0x1.toByte()).toDC()}")
                0x22.toByte() -> append("LX1出厂雕刻补偿设置: 黑白:${data1} 抖动:${data2} ${(data3 == 0x1.toByte()).toDC()}")
            }
            append(" custom:${custom}")
        }
    }

}
