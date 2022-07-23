package com.angcyo.bluetooth.fsc.laserpacker.command

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.DEFAULT_PX
import com.angcyo.library.component.byteWriter
import com.angcyo.library.ex.trimAndPad

/**
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
) : ICommand {

    companion object {

        /**雕刻文件名称占用字节数量*/
        const val DEFAULT_NAME_BYTE_COUNT = 36

        /**
         * [index] 雕刻文件索引, 下位机用来查找并打印. 32位 最大值[4294967295]
         * [bitmapData] 图片数据
         * [px]  当PX为以下值时对应图片分辨率：
         *       PX = 0x05 时 图片分辨率为800*800
         *       PX = 0x04 时 图片分辨率为1000*1000
         *       PX = 0x03 时 图片分辨率为1300*1300
         *       PX = 0x02 时 图片分辨率为2000*2000
         *       PX = 0x01 时 图片分辨率为4000*4000
         * [bitmapWidth] 图片的宽高. px修正过后的数据
         * [bitmapHeight]
         *
         * [minX] 图片的x,y坐标. px修正过后的数据
         * [name] 下位机用来显示的文件名, 真正的文件名. 最大36个字节, 再补充一个1字节0的数据
         * */
        fun bitmapData(
            index: Int,
            bitmapData: ByteArray?,
            bitmapWidth: Int,
            bitmapHeight: Int,
            minX: Int = 0, //图片最小坐标(X,Y)。2字节
            minY: Int = 0,
            px: Byte = DEFAULT_PX,
            name: String?,
        ): DataCmd {
            val logBuilder = StringBuilder()
            //数据头
            val head = byteWriter {
                //0x10时图片数据
                write(0x10)

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

                write(px)
                write(minX, 2)
                write(minY, 2)

                //塞满20个
                padLength(20)
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
         * [lines] GCode数据行数
         * [x] GCode起始坐标, 相对于坐标原点
         * [y] GCode起始坐标, 相对于坐标原点
         * [width] GCode的宽度2字节
         * [height] GCode的高度2字节
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
        ): DataCmd {
            val logBuilder = StringBuilder()
            //数据头
            val head = byteWriter {
                //0x20时为GCODE数据
                write(0x20)

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

                //塞满20个
                padLength(20)
                //第21个字节开始 共36个字节的文件名
                val nameBytes =
                    (name ?: "Default").toByteArray().trimAndPad(DEFAULT_NAME_BYTE_COUNT)
                write(nameBytes)
                write(0x00) //写入文件结束字节

                //垫满
                padLength(64) //需要64个字节

                //日志
                logBuilder.append("0x20 GCode->")
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
        append("发送数据:size:${head.size + data.size}bytes ")
        log?.let { append(it) }
    }

    override fun getReceiveTimeout(): Long {
        return 30 * 60 * 1_000 //10分钟
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
        .trimAndPad(DataCmd.DEFAULT_NAME_BYTE_COUNT, false)
        .toString(Charsets.UTF_8)
}
