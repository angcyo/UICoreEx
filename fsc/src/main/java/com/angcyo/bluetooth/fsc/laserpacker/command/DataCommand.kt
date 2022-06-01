package com.angcyo.bluetooth.fsc.laserpacker.command

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.library.component.byteWriter

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/30
 */
data class DataCommand(
    /**
     * 64位的文件数据头部信息
     * */
    val head: ByteArray,

    /**
     * 文件数据, 大小要等于
     * [com.angcyo.bluetooth.fsc.laserpacker.command.FileModeCmd.dataSize]
     * */
    val data: ByteArray
) : ICommand {

    companion object {

        /**
         * [name] 雕刻文件名,用来打印. 32位 最大值[4294967295]
         * [bitmapData] 图片数据
         * [px]  当PX为以下值时对应图片分辨率：
         *       PX = 0x05 时 图片分辨率为800*800
         *       PX = 0x04 时 图片分辨率为1000*1000
         *       PX = 0x03 时 图片分辨率为1300*1300
         *       PX = 0x02 时 图片分辨率为2000*2000
         *       PX = 0x01 时 图片分辨率为4000*4000
         * [bitmapWidth] 图片编辑时的宽高
         * [bitmapHeight]
         *
         * [minX] 图片编辑时的x,y坐标
         *
         * */
        fun bitmapData(
            name: Int,
            bitmapData: ByteArray,
            bitmapWidth: Int,
            bitmapHeight: Int,
            minX: Int = 0, //图片最小坐标(X,Y)。2字节
            minY: Int = 0,
            px: Byte = 0x04,
        ): DataCommand {
            //数据头
            val head = byteWriter {
                //0x10时图片数据
                write(0x10)

                //图片的宽高8位
                val width = LaserPeckerHelper.transformWidth(bitmapWidth, px)
                write(width and 0xff00 shr 8 and 0xff) //高8位
                //图片的宽低8位
                write(width and 0xff) //低8位

                val height = LaserPeckerHelper.transformWidth(bitmapHeight, px)
                //图片的高高8位
                write(height and 0xff00 shr 8 and 0xff) //高8位
                //图片的高低8位
                write(height and 0xff) //低8位

                //图片名称，占用4个字节
                write(name, 4)

                write(px)
                val x = LaserPeckerHelper.transformX(minX, px)
                val y = LaserPeckerHelper.transformY(minY, px)
                write(x, 2)
                write(y, 2)

                padLength(64) //需要64个字节
            }
            //数据
            val data = byteWriter {
                write(bitmapData)
            }
            return DataCommand(head, data)
        }

        /**GCode数据*/
        fun gcodeData(
            name: Int,
            gcodeData: ByteArray,
        ): DataCommand {
            //数据头
            val head = byteWriter {
                //0x20时为GCODE数据
                write(0x20)
                //占位
                writeSpace(4)
                //图片名称，占用4个字节
                write(name, 4)
                //垫满
                padLength(64) //需要64个字节
            }
            //数据
            val data = byteWriter {
                write(gcodeData)
            }
            return DataCommand(head, data)
        }
    }

    override fun toByteArray(): ByteArray {
        return byteWriter {
            write(head)
            write(data)
        }
    }

    override fun getReceiveTimeout(): Long {
        return 1 * 60 * 60 * 1_000
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DataCommand

        if (!head.contentEquals(other.head)) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = head.contentHashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}
