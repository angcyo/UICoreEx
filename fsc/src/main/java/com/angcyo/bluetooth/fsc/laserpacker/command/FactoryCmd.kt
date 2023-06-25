package com.angcyo.bluetooth.fsc.laserpacker.command

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.library.annotation.Pixel

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
    var laser: Byte = 0x0, //激光功率
    /**[com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd.type]*/
    var type: Byte = LaserPeckerHelper.LASER_TYPE_BLUE, //激光类型

    /**跳到执行坐标*/
    var x: Int = 0,
    var y: Int = 0,

    ) : BaseCommand() {

    companion object {

        /**无矫正范围预览*/
        fun noAdjustRangePreviewCmd(): FactoryCmd {
            return FactoryCmd(state = 0x05)
        }

        /**较正数据传输完成*/
        fun finishAdjustDataCmd(index: Int): FactoryCmd {
            return FactoryCmd(state = 0x08, index = index)
        }

        /**激光点跳至指定AD值*/
        fun jumpToAdCmd(adX: Int, adY: Int, laser: Byte, type: Byte): FactoryCmd {
            return FactoryCmd(state = 0x09, adX = adX, adY = adY, laser = laser, type = type)
        }

        /**激光点跳到指定坐标*/
        fun jumpToCoordCmd(x: Int, y: Int): FactoryCmd {
            return FactoryCmd(state = 0x0A, x = x, y = y)
        }

        /**激光点预览功率设置*/
        fun jumpToCoordCmd(laser: Byte, type: Byte): FactoryCmd {
            return FactoryCmd(state = 0x0B, laser = laser, type = type)
        }
    }

    override fun toByteArray(): ByteArray {
        return commandByteWriter {
            write(commandFunc())
            write(state)

            when (state) {
                0x05.toByte() -> write(0, 4)//补齐4个字节
                0x08.toByte() -> {
                    write(index, 4) //数据索引
                    write(custom)
                }

                0x09.toByte() -> {
                    write(adX, 2)
                    write(adY, 2)
                    write(laser)
                    write(type)
                    write(custom)
                }

                0x0A.toByte() -> {
                    write(x, 2)
                    write(y, 2)
                    write(custom)
                }

                0x0B.toByte() -> {
                    write(laser)
                    write(type)
                    write(0, 2)//补齐2个字节
                    write(custom)
                }
            }
        }
    }

    override fun commandFunc(): Byte = 0x0f

    override fun toHexCommandString(): String {
        return super.toHexCommandString()
    }

    override fun toCommandLogString(): String {
        return buildString {
            append("出厂设置指令:")
            when (state) {
                0x05.toByte() -> append("无较正范围预览")
                0x08.toByte() -> append("较正数据传输完成[$index]")
                0x09.toByte() -> append("激光点跳至指定AD值:x:${adX} y:${adY} laser:${laser} type:${type}")
                0x0A.toByte() -> append("激光点跳到指定坐标:x:${x} y:${y}")
                0x0B.toByte() -> append("激光点预览功率设置:laser:${laser} type:${type}")
            }
            append(" custom:${custom}")
        }
    }

}
