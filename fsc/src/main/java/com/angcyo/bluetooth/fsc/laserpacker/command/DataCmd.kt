package com.angcyo.bluetooth.fsc.laserpacker.command

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.data.toDpiInt
import com.angcyo.library.annotation.Implementation
import com.angcyo.library.component.byteWriter
import com.angcyo.library.component.hawk.LibLpHawkKeys
import com.angcyo.library.ex.trimAndPad

/**
 *
 * [com.angcyo.bluetooth.fsc.laserpacker.command.FileModeCmd]
 * 数据传输指令, 需要先进入文件传输模式
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/30
 */
data class DataCmd(
    /**
     * 64位的文件数据头部信息
     * */
    val head: ByteArray,

    /**
     * 文件数据, 大小要等于
     * [com.angcyo.bluetooth.fsc.laserpacker.command.FileModeCmd.dataSize]
     * */
    val data: ByteArray,

    /**数据指令日志信息*/
    var log: String? = null
) : BaseCommand() {

    companion object {

        /**名字前面的字节数*/
        const val DEFAULT_NAME_BYTE_START = 34

        /**雕刻文件名称占用字节数量*/
        const val DEFAULT_NAME_BYTE_COUNT = 28

        /**数据传输超时时长, 30分钟*/
        const val MAX_RECEIVE_TIMEOUT = 30 * 60 * 1_000L

        //---支持的雕刻数据类型---

        /**雕刻图片数据类型.
         *
         * 图片白色像素不打印打印, 色值:255  byte:-1
         * 图片黑色像素打印,      色值:0    byte:0
         *
         * 色值决定机器的功率, 色值越大功率越大.
         * 但是设备进行了取反操作, 所有上层0,机器取反就是255
         * 机器打纸的时候, 才会取反
         * 机器打金属的时候, 不取反
         * */
        const val ENGRAVE_TYPE_BITMAP = 0x10

        /**GCode数据类型*/
        const val ENGRAVE_TYPE_GCODE = 0x20

        /**路径数据*/
        @Implementation
        const val ENGRAVE_TYPE_PATH = 0x30

        /**图片转路径数据格式*/
        const val ENGRAVE_TYPE_BITMAP_PATH = 0x40

        /**图片裁剪数据类型*/
        @Implementation
        const val ENGRAVE_TYPE_BITMAP_CROP = 0x50

        /**图片抖动数据类型*/
        const val ENGRAVE_TYPE_BITMAP_DITHERING = 0x60

        /**GCode数据类型, 切割*/
        const val ENGRAVE_TYPE_GCODE_CUT = 0x70

        //---

        /**
         * [index] 雕刻文件索引, 下位机用来查找并打印. 32位 最大值[4294967295]
         * [bitmapData] 图片数据
         * [px]  当PX为以下值时对应图片分辨率：
         *       PX = 0x05 时 图片分辨率为800*800
         *       PX = 0x04 时 图片分辨率为1000*1000
         *       PX = 0x03 时 图片分辨率为1300*1300
         *       PX = 0x02 时 图片分辨率为2000*2000
         *       PX = 0x01 时 图片分辨率为4000*4000
         *
         * 缩放倍数: = DPI / 254
         *
         * [bitmapWidth] 图片的宽高. px修正过后的数据
         * [bitmapHeight]
         *
         * [minX] 图片的x,y坐标. px修正过后的数据
         * [name] 下位机用来显示的文件名, 真正的文件名. 最大36个字节, 再补充一个1字节0的数据
         *
         * 0x10时图片数据
         * */
        fun bitmapData(
            index: Int,
            minX: Int = 0, //图片最小坐标(X,Y)。2字节
            minY: Int = 0,
            bitmapWidth: Int,
            bitmapHeight: Int,
            layerId: String,
            dpi: Float,
            name: String?,
            bitmapData: ByteArray?,
        ): DataCmd {
            val logBuilder = StringBuilder()
            //数据头
            val head = byteWriter {
                //0x10时图片数据
                write(ENGRAVE_TYPE_BITMAP)

                //图片的宽,2字节
                write(bitmapWidth and 0xff00 shr 8 and 0xff) //高8位
                //图片的宽低8位
                write(bitmapWidth and 0xff) //低8位

                //图片的高,2字节
                write(bitmapHeight and 0xff00 shr 8 and 0xff) //高8位
                //图片的高低8位
                write(bitmapHeight and 0xff) //低8位

                //图片索引，占用4个字节
                write(index, 4)

                write(LaserPeckerHelper.findPxInfo(layerId, dpi).px)
                write(minX, 2) //d3
                write(minY, 2) //d4

                //dpi,占用2个字节
                write(dpi.toDpiInt(), 2) //d5

                write((LibLpHawkKeys.lastSlipSpace * 10).toInt(), 2) //d6

                //塞满34个
                padLength(DEFAULT_NAME_BYTE_START)
                //第21个字节开始 共36个字节的文件名
                val nameBytes =
                    (name ?: "Default").toByteArray().trimAndPad(DEFAULT_NAME_BYTE_COUNT)
                write(nameBytes)
                write(0x00) //写入文件结束字节

                padLength(64) //需要64个字节

                //日志
                logBuilder.append("0x10 图片->")
                logBuilder.append(" index:$index")
                logBuilder.append(" name:$name")
                logBuilder.append(" w:$bitmapWidth")
                logBuilder.append(" h:$bitmapHeight")
                logBuilder.append(" minX:$minX")
                logBuilder.append(" minY:$minY")
            }
            //数据
            val data = byteWriter {
                write(bitmapData)
            }
            return DataCmd(head, data, logBuilder.toString())
        }

        /**GCode数据
         * [index] 数据索引
         * [name] 文件名
         * [lines] GCode数据行数
         * [x] GCode起始坐标, 相对于坐标原点
         * [y] GCode起始坐标, 相对于坐标原点
         * [width] GCode的宽度2字节
         * [height] GCode的高度2字节
         * [isCut] 是否是切割
         *
         * 0x20时为GCODE数据
         * */
        fun gcodeData(
            index: Int,
            x: Int,
            y: Int,
            width: Int,
            height: Int,
            name: String?,
            lines: Int,
            gcodeData: ByteArray?,
            dpi: Float,
            isCut: Boolean
        ): DataCmd {
            val logBuilder = StringBuilder()
            //数据头
            val head = byteWriter {
                if (isCut) {
                    //0x70时为GCODE切割数据
                    write(ENGRAVE_TYPE_GCODE_CUT)
                } else {
                    //0x20时为GCODE数据
                    write(ENGRAVE_TYPE_GCODE)
                }

                //宽,2字节
                write(width and 0xff00 shr 8 and 0xff) //高8位
                //图片的宽低8位
                write(width and 0xff) //低8位

                //高,2字节
                write(height and 0xff00 shr 8 and 0xff) //高8位
                //图片的高低8位
                write(height and 0xff) //低8位

                //数据索引，占用4个字节
                write(index, 4)

                //GCode行数
                write(lines, 4)

                //x,2字节
                write(x and 0xff00 shr 8 and 0xff) //高8位
                //图片的宽低8位
                write(x and 0xff) //低8位

                //y,2字节
                write(y and 0xff00 shr 8 and 0xff) //高8位
                //图片的高低8位
                write(y and 0xff) //低8位

                //dpi,占用2个字节
                write(dpi.toDpiInt(), 2) //d5

                write((LibLpHawkKeys.lastSlipSpace * 10).toInt(), 2) //d6

                //塞满34个
                padLength(DEFAULT_NAME_BYTE_START)
                //第21个字节开始 共36个字节的文件名
                val nameBytes =
                    (name ?: "Default").toByteArray().trimAndPad(DEFAULT_NAME_BYTE_COUNT)
                write(nameBytes)
                write(0x00) //写入文件结束字节

                //垫满
                padLength(64) //需要64个字节

                //日志
                if (isCut) {
                    logBuilder.append("0x70 GCode切割->")
                } else {
                    logBuilder.append("0x20 GCode->")
                }
                logBuilder.append(" index:$index")
                logBuilder.append(" lines:$lines")
                logBuilder.append(" name:$name")
                logBuilder.append(" w:$width")
                logBuilder.append(" h:$height")
                logBuilder.append(" x:$x")
                logBuilder.append(" y:$y")
            }
            //数据
            val data = byteWriter {
                write(gcodeData)
            }
            return DataCmd(head, data, logBuilder.toString())
        }

        /**图片转路径数据
         * [index] 数据索引
         * [name] 文件名
         * [lines] 路径的线段数量
         * [x] [y] 图片的起始坐标, 相对于坐标原点, 2字节
         * [width] [height] 图片的宽高, 2字节
         *
         * 0x40时为图片路径数据
         * */
        fun bitmapPathData(
            index: Int,
            x: Int,
            y: Int,
            width: Int,
            height: Int,
            name: String?,
            lines: Int,
            bytes: ByteArray?,
            layerId: String,
            dpi: Float,
        ): DataCmd {
            val logBuilder = StringBuilder()
            //数据头
            val head = byteWriter {
                //0x40时为图片路径数据
                write(ENGRAVE_TYPE_BITMAP_PATH)

                //宽,2字节
                write(width, 2)
                //高,2字节
                write(height, 2)

                //数据索引，占用4个字节
                write(index, 4)

                //线段数
                write(lines, 4)

                write(LaserPeckerHelper.findPxInfo(layerId, dpi).px)
                write(x, 2)
                write(y, 2)

                //dpi,占用2个字节
                write(dpi.toDpiInt(), 2) //d5

                write((LibLpHawkKeys.lastSlipSpace * 10).toInt(), 2) //d6

                /*以下是0x30数据
                //线段数 低16位
                val lLines = lines and 0xffff
                write(lLines, 2)

                write(px)
                write(x, 2)
                write(y, 2)

                //线段数 高16位
                val hLines = (lines shr 16) and 0xffff
                write(hLines, 2)*/

                //塞满34个
                padLength(DEFAULT_NAME_BYTE_START)
                //第21个字节开始 共36个字节的文件名
                val nameBytes =
                    (name ?: "Default").toByteArray().trimAndPad(DEFAULT_NAME_BYTE_COUNT)
                write(nameBytes)
                write(0x00) //写入文件结束字节

                //垫满
                padLength(64) //需要64个字节

                //日志
                logBuilder.append("0x40 BitmapPath->")
                logBuilder.append(" index:$index")
                logBuilder.append(" lines:$lines")
                logBuilder.append(" name:$name")
                logBuilder.append(" w:$width")
                logBuilder.append(" h:$height")
                logBuilder.append(" x:$x")
                logBuilder.append(" y:$y")
            }
            //数据
            val data = byteWriter {
                write(bytes)
            }
            return DataCmd(head, data, logBuilder.toString())
        }

        /**图片抖动数据, 按位压缩后的数据
         * [index] 数据索引
         * [name] 文件名
         * [x] [y] 图片的起始坐标, 相对于坐标原点, 2字节
         * [width] [height] 图片的宽高, 2字节
         *
         * 0x60时为图片抖动数据
         * */
        fun bitmapDitheringData(
            index: Int,
            x: Int,
            y: Int,
            width: Int,
            height: Int,
            name: String?,
            bytes: ByteArray?,
            layerId: String,
            dpi: Float,
        ): DataCmd {
            val logBuilder = StringBuilder()
            //数据头
            val head = byteWriter {
                //0x60时为图片抖动数据
                write(ENGRAVE_TYPE_BITMAP_DITHERING)

                //宽,2字节
                write(width, 2)
                //高,2字节
                write(height, 2)

                //数据索引，占用4个字节
                write(index, 4)

                write(LaserPeckerHelper.findPxInfo(layerId, dpi).px)
                write(x, 2)
                write(y, 2)

                //dpi,占用2个字节
                write(dpi.toDpiInt(), 2) //d5

                write((LibLpHawkKeys.lastSlipSpace * 10).toInt(), 2) //d6

                //塞满34个
                padLength(DEFAULT_NAME_BYTE_START)
                //第21个字节开始 共36个字节的文件名
                val nameBytes =
                    (name ?: "Default").toByteArray().trimAndPad(DEFAULT_NAME_BYTE_COUNT)
                write(nameBytes)
                write(0x00) //写入文件结束字节

                //垫满
                padLength(64) //需要64个字节

                //日志
                logBuilder.append("0x60 BitmapDithering->")
                logBuilder.append(" index:$index")
                logBuilder.append(" name:$name")
                logBuilder.append(" w:$width")
                logBuilder.append(" h:$height")
                logBuilder.append(" x:$x")
                logBuilder.append(" y:$y")
            }
            //数据
            val data = byteWriter {
                write(bytes)
            }
            return DataCmd(head, data, logBuilder.toString())
        }

        //---

        /**纯数据*/
        fun data(data: ByteArray): DataCmd {
            //数据头
            /*val head = byteWriter {
                //垫满
                padLength(64) //需要64个字节
            }*/
            return DataCmd(byteArrayOf(), data)
        }
    }

    //功能码
    override fun commandFunc(): Byte = 0x05

    override fun toByteArray(): ByteArray {
        return byteWriter {
            write(head)
            write(data)
        }
    }

    override fun toCommandLogString(): String = buildString {
        append("发送数据:head:${head.size}bytes data:${data.size}bytes :${head.size + data.size}bytes")
        log?.let { append(it) }
    }

    override fun getReceiveTimeout(): Long {
        return MAX_RECEIVE_TIMEOUT //30分钟
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DataCmd

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

/**将字符串剔除到36个字节*/
fun String.trimEngraveName(): String {
    return toByteArray(Charsets.UTF_8)
        .trimAndPad(DataCmd.DEFAULT_NAME_BYTE_COUNT, 1)
        .toString(Charsets.UTF_8)
}

fun Int.toEngraveTypeStr() = when (this) {
    DataCmd.ENGRAVE_TYPE_BITMAP -> "雕刻图片数据"
    DataCmd.ENGRAVE_TYPE_GCODE -> "雕刻GCode数据"
    DataCmd.ENGRAVE_TYPE_GCODE_CUT -> "雕刻GCode切割数据"
    DataCmd.ENGRAVE_TYPE_PATH -> "雕刻路径数据"
    DataCmd.ENGRAVE_TYPE_BITMAP_PATH -> "雕刻图片路径数据"
    DataCmd.ENGRAVE_TYPE_BITMAP_CROP -> "雕刻图片裁剪数据"
    DataCmd.ENGRAVE_TYPE_BITMAP_DITHERING -> "雕刻抖动数据"
    else -> "EngraveType-${this}"
}
