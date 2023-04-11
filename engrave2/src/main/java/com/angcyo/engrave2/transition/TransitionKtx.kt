package com.angcyo.engrave2.transition

import android.graphics.*
import android.view.Gravity
import android.widget.LinearLayout
import com.angcyo.bitmap.handle.BitmapHandle
import com.angcyo.engrave2.data.BitmapPath
import com.angcyo.gcode.GCodeDrawable
import com.angcyo.gcode.GCodeHelper
import com.angcyo.gcode.GCodeWriteHandler
import com.angcyo.library.L
import com.angcyo.library.annotation.Pixel
import com.angcyo.library.component.byteWriter
import com.angcyo.library.component.lastContext
import com.angcyo.library.ex.*
import com.angcyo.library.libCacheFile
import com.angcyo.library.unit.IValueUnit
import java.io.File
import kotlin.experimental.or
import kotlin.math.max

/**
 * 传输扩展工具类
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/29
 */

//region ---图片数据生成---

/**从图片中, 获取雕刻需要用到的像素信息*/
fun Bitmap.engraveColorBytes(channelType: Int = Color.RED): ByteArray {
    return colorChannel(channelType) { color, channelValue ->
        if (color == Color.TRANSPARENT) {
            0xFF //255 白色像素, 白色在纸上不雕刻, 在金属上雕刻
        } else {
            channelValue
        }
    }
}

/**机器雕刻的色彩数据可视化*/
fun ByteArray.toEngraveBitmap(width: Int, height: Int): Bitmap {
    val channelBitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(channelBitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.style = Paint.Style.FILL
        strokeWidth = 1f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    val bytes = this
    for (y in 0 until height) {
        for (x in 0 until width) {
            val value: Int = bytes[y * width + x].toHexInt()
            paint.color = Color.argb(255, value, value, value)
            canvas.drawCircle(x.toFloat(), y.toFloat(), 1f, paint)//绘制圆点
        }
    }
    return channelBitmap
}

//endregion ---图片数据生成---

//region ---图片线段数据生成---

/**[bitmap] 图片转路径数据
 * [threshold] 颜色阈值, 此值以下的色值视为黑色0
 *
 * [offsetLeft] 坐标偏移量
 * [offsetTop] 坐标偏移量
 *
 * [orientation] 像素扫描的方向
 *
 * [Bitmap.eachPixel]
 * */
fun Bitmap.toBitmapPath(
    threshold: Int,
    offsetLeft: Int = 0,
    offsetTop: Int = 0,
    orientation: Int = LinearLayout.HORIZONTAL
): List<BitmapPath> {
    val bitmap = this
    val result = mutableListOf<BitmapPath>()

    var lastBitmapPath: BitmapPath? = null
    var isLTR = true

    //追加一段路径
    fun appendPath(ltr: Boolean, lineEnd: Boolean = false) {
        lastBitmapPath?.apply {
            this.lineEnd = lineEnd
            result.add(this)
            lastBitmapPath = null
            isLTR = ltr
        }
    }

    //判断色值
    fun handleColor(x: Int, y: Int, color: Int, ltr: Boolean) {
        val gray = color.toGrayInt()
        if (gray <= threshold && color != Color.TRANSPARENT) {
            //00, 黑色纸上雕刻, 金属不雕刻
            if (lastBitmapPath == null) {
                //第一个点不进行len累加
                lastBitmapPath =
                    BitmapPath(x + offsetLeft, y + offsetTop, 0, ltr, orientation)
            } else {
                //第二个点开始才进行len累加, 表示多少个线段的意思
                lastBitmapPath?.apply {
                    len++
                }
            }
        } else {
            appendPath(!ltr)
        }
    }

    val width = bitmap.width
    val height = bitmap.height

    if (orientation == LinearLayout.VERTICAL) {
        //纵向枚举, 从左往右, 从上到下
        for (x in 0 until width) {
            val ltr = isLTR
            for (y in 0 until height) {
                //一列
                val hIndex = if (ltr) y else height - y - 1
                val color = bitmap.getPixel(x, hIndex)
                handleColor(x, hIndex, color, ltr)
            }
            //收尾
            appendPath(!ltr, true)
        }
    } else {
        //横向枚举, 从上往下, 从左到右
        for (y in 0 until height) {
            val ltr = isLTR
            for (x in 0 until width) {
                //一行
                val wIndex = if (ltr) x else width - x - 1
                val color = bitmap.getPixel(wIndex, y)
                handleColor(wIndex, y, color, ltr)
            }
            //收尾
            appendPath(!ltr, true)
        }
    }
    return result
}


/**反向转成图片
 * [width] 图片真实的宽高, 不需要加上偏移
 * [offsetLeft] 线段数据的偏移量, 绘制时会减去此值
 * */
fun List<BitmapPath>.toEngraveBitmap(
    width: Int,
    height: Int,
    offsetLeft: Int = 0,
    offsetTop: Int = 0
): Bitmap {
    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.style = Paint.Style.FILL
        strokeWidth = 1f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    forEach { bp ->
        if (bp.orientation == LinearLayout.VERTICAL) {
            val x: Float = bp.x.toFloat()
            val top: Float
            val bottom: Float
            if (bp.ltr) {
                //上到下
                top = bp.y.toFloat()
                bottom = top + bp.len
            } else {
                //下到上
                bottom = bp.y.toFloat()
                top = bottom - bp.len
            }
            canvas.drawLine(
                x - offsetLeft,
                top - offsetTop,
                x - offsetLeft,
                bottom - offsetTop,
                paint
            )
        } else {
            val y: Float = bp.y.toFloat()
            val left: Float
            val right: Float
            if (bp.ltr) {
                //左到右
                left = bp.x.toFloat()
                right = left + bp.len
            } else {
                //右到左
                right = bp.x.toFloat()
                left = right - bp.len
            }
            canvas.drawLine(
                left - offsetLeft,
                y - offsetTop,
                right - offsetLeft,
                y - offsetTop,
                paint
            )
        }
    }
    return result
}

/**转换成调试的输出日志*/
fun List<BitmapPath>.toEngraveLog(): String = buildString {
    this@toEngraveLog.forEach { bp ->
        if (bp.orientation == LinearLayout.VERTICAL) {
            //垂直
            if (bp.ltr) {
                append("y:${bp.y}→${bp.y + bp.len} ")
            } else {
                append("${bp.y - bp.len}←${bp.y}:y ")
            }
        } else {
            //水平
            if (bp.ltr) {
                append("x:${bp.x}→${bp.x + bp.len} ")
            } else {
                append("${bp.x - bp.len}←${bp.x}:x ")
            }
        }

        if (bp.lineEnd) {
            appendLine()
        }
    }
}

//endregion ---图片线段数据生成---

//region ---抖动数据生成---

/**[bitmap] 图片转抖动数据, 在黑色金属上雕刻效果正确, 在纸上雕刻时反的
 * [threshold] 颜色阈值, 此值以下的色值视为黑色0
 * 白色传1, 1不出光. 黑色传0, 0出光, 数据压缩
 * */
fun Bitmap.toBitmapByte(threshold: Int): Pair<List<String>, ByteArray> {
    val bitmap = this
    val width = bitmap.width
    val height = bitmap.height

    var byte: Byte = 0 //1个字节8位
    var bit = 0
    val logList = mutableListOf<String>()
    val lineBuilder = StringBuilder()
    val bytes = byteWriter {
        for (y in 0 until height) {
            bit = 7
            byte = 0
            for (x in 0 until width) {
                //一行
                val color = bitmap.getPixel(x, y)
                //val channelColor = Color.red(color)
                val grayInt = color.toGrayInt()

                if (grayInt <= threshold) {
                    //黑色传0, 黑色纸上雕刻, 金属不雕刻
                    //byte = byte or (0b1 shl bit)
                    lineBuilder.append("0")
                } else {
                    //白色传1
                    byte = byte or (0b1 shl bit).toByte()
                    lineBuilder.append("1")
                }
                bit--
                if (bit < 0) {
                    //8位
                    write(byte) //写入1个字节
                    bit = 7
                    byte = 0
                }
            }

            //
            if (bit != 7) {
                write(byte)//写入1个字节
            }
            //
            logList.add(lineBuilder.toString())
            lineBuilder.clear()
        }
    }
    return logList to bytes
}

/**[toBitmapByte]不压缩*/
fun Bitmap.toBitmapByteUncompress(threshold: Int): Pair<List<String>, ByteArray> {
    val bitmap = this
    val width = bitmap.width
    val height = bitmap.height

    val logList = mutableListOf<String>()
    val lineBuilder = StringBuilder()
    val bytes = byteWriter {
        for (y in 0 until height) {
            for (x in 0 until width) {
                //一行
                val color = bitmap.getPixel(x, y)
                //val channelColor = Color.red(color)
                val grayInt = color.toGrayInt()

                if (grayInt <= threshold) {
                    //黑色传0, 黑色纸上雕刻, 金属不雕刻
                    write(0)
                    lineBuilder.append("0")
                } else {
                    //白色传1
                    write(1)
                    lineBuilder.append("1")
                }
            }
            logList.add(lineBuilder.toString())
            lineBuilder.clear()
        }
    }
    return logList to bytes
}

/**将抖动数据 00011110001010\n00011110001010 描述字符串, 转换成可视化图片*/
fun String.toEngraveDitheringBitmap(width: Int, height: Int): Bitmap {
    return lines().toEngraveDitheringBitmap(width, height)
}

/**将抖动数据 00011110001010\n00011110001010 描述字符串, 转换成可视化图片
 * 白色传1, 1不出光. 黑色传0, 0出光, 数据压缩
 * */
fun List<String>.toEngraveDitheringBitmap(width: Int, height: Int): Bitmap {
    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.style = Paint.Style.FILL
        strokeWidth = 1f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    var x = 0f
    var y = 0f
    forEach { line ->
        x = 0f
        line.forEach { char ->
            if (char == '0') {
                //1绘制
                canvas.drawCircle(x, y, 1f, paint)//绘制圆点
            }
            x++
        }
        y++
    }
    return result
}

//endregion ---抖动数据生成---

//region ---GCode数据生成---

/**简单的将[Bitmap]转成GCode数据
 * 横向扫描像素点,白色像素跳过,黑色就用G1打印
 * [gravity] 线的扫描方向, 为null, 自动选择. 宽图使用[Gravity.LEFT], 长图使用[Gravity.TOP]
 * [Gravity.LEFT]:垂直从左开始上下下上扫描 [Gravity.RIGHT]:
 * [Gravity.TOP]:水平从上开始左右右左扫描 [Gravity.BOTTOM]:
 *
 * [threshold] 当色值>=此值时, 忽略数据 255白色 [0~255]
 * [isSingleLine] 当前的图片是否是简单的线段, 如果是, 则每一行或者每一列只取一个像素点, 用来处理旋转了的虚线,也就是斜线, 非斜线, 会自动关闭此值
 *
 * 采样的时候, 使用像素单位, 但是写入文件的时候转换成mm单位
 * */
fun Bitmap.toGCode(
    gravity: Int? = null,
    offsetX: Float = 0f, //偏移距离
    offsetY: Float = 0f,//偏移距离
    @Pixel
    gapValue: Float = 1f,//这里的gap用像素单位, 表示采样间隙
    threshold: Int = 255, //255白色不输出GCode
    outputFile: File = libCacheFile(),
    isFirst: Boolean = true,
    isFinish: Boolean = true,
    autoCnc: Boolean = false,
    isSingleLine: Boolean = false,
): File {
    val bitmap = this
    val gCodeWriteHandler = GCodeWriteHandler()
    //像素单位转成mm单位
    val mmValueUnit = IValueUnit.MM_UNIT
    gCodeWriteHandler.isPixelValue = true
    gCodeWriteHandler.unit = mmValueUnit
    gCodeWriteHandler.isAutoCnc = autoCnc
    gCodeWriteHandler.gapValue = gapValue
    gCodeWriteHandler.gapMaxValue = gCodeWriteHandler.gapValue

    val width = bitmap.width
    val height = bitmap.height

    //是否是斜线
    val isObliqueLine = isSingleLine && width > 1 && height > 1

    if (isDebuggerConnected()) {
        val pixels = bitmap.getPixels()
        L.i(pixels)
    }
    val data = bitmap.engraveColorBytes()

    val scanGravity = gravity ?: if (width > height) {
        //宽图
        Gravity.LEFT
    } else {
        Gravity.TOP
    }

    val pixelStep = gapValue.toInt()//1 * dpi //横纵像素采样率

    //反向读取数据, Z形方式
    var isReverseDirection = false
    //最后的有效数据坐标
    var lastGCodeLineRef: Int = -1

    outputFile.writer().use { writer ->
        gCodeWriteHandler.writer = writer

        //最后的坐标
        var lastLineRef = 0

        if (isFirst) {
            gCodeWriteHandler.onPathStart()
        }

        if (scanGravity == Gravity.LEFT || scanGravity == Gravity.RIGHT) {
            //从左到右, 垂直扫描
            val xFrom: Int
            val xTo: Int
            val xStep: Int //跳跃的像素

            if (scanGravity == Gravity.LEFT) {
                xFrom = 0
                xTo = width - 1
                xStep = pixelStep
            } else {
                xFrom = width - 1
                xTo = 0
                xStep = -pixelStep
            }

            var currentX = xFrom
            while (true) {//列
                lastLineRef = currentX + pixelStep
                for (y in 0 until height step pixelStep) {//行
                    //rtl
                    val lineY = if (isReverseDirection) {
                        (height - 1 - y)
                    } else {
                        y
                    }
                    val index = max(0, (lineY - 1)) * width + currentX
                    val value: Int = data[index].toHexInt()
                    if (value < threshold) {
                        //有效的像素
                        /*val yValue = mmValueUnit.convertPixelToValue(lineY.toFloat())
                        val xValue = mmValueUnit.convertPixelToValue(lastLineRef.toFloat())*/

                        val yValue = lineY.toDouble()
                        val xValue = lastLineRef.toDouble()

                        gCodeWriteHandler.writePoint(xValue + offsetX, yValue + offsetY)
                        lastGCodeLineRef = lastLineRef //有数据的列

                        if (isObliqueLine) {
                            break
                        }
                    }
                }
                if (!isObliqueLine) {
                    gCodeWriteHandler.clearLastPoint()

                    //rtl
                    if (lastGCodeLineRef == lastLineRef) {
                        //这一行有GCode数据
                        isReverseDirection = !isReverseDirection
                    }
                }

                //到底了
                if (currentX == xTo) {
                    break
                } else {
                    //最后一行校验, 忽略step的值
                    currentX += xStep
                    if (currentX + 1 >= width) {
                        currentX = width - 1
                    } else if (currentX + 1 <= 0) {
                        currentX = 0
                    }
                }
            }
        } else {
            //从上到下, 水平扫描, 第几行. 从0开始
            val yFrom: Int
            val yTo: Int
            val yStep: Int //跳跃的像素

            if (scanGravity == Gravity.TOP) {
                yFrom = 0
                yTo = height - 1
                yStep = pixelStep
            } else {
                yFrom = height - 1
                yTo = 0
                yStep = -pixelStep
            }

            var currentY = yFrom
            while (true) {//行
                lastLineRef = currentY + pixelStep
                for (x in 0 until width step pixelStep) {//列
                    //rtl
                    val lineX = if (isReverseDirection) {
                        (width - 1 - x)
                    } else {
                        x
                    }
                    val index = currentY * width + lineX
                    val value: Int = data[index].toHexInt()
                    if (value < threshold) {
                        //有效的像素
                        /*val xValue = mmValueUnit.convertPixelToValue(lineX.toFloat())
                        val yValue = mmValueUnit.convertPixelToValue(lastLineRef.toFloat())*/

                        val xValue = lineX.toDouble()
                        val yValue = lastLineRef.toDouble()

                        gCodeWriteHandler.writePoint(xValue + offsetX, yValue + offsetY)
                        lastGCodeLineRef = lastLineRef //有数据的行

                        if (isObliqueLine) {
                            break
                        }
                    }
                }
                if (!isObliqueLine) {
                    gCodeWriteHandler.clearLastPoint()

                    //rtl
                    if (lastGCodeLineRef == lastLineRef) {
                        //这一行有GCode数据
                        isReverseDirection = !isReverseDirection
                    }
                }

                //到底了
                if (currentY == yTo) {
                    break
                } else {
                    //最后一行校验, 忽略step的值
                    currentY += yStep
                    if (currentY + 1 >= height) {
                        currentY = height - 1
                    } else if (currentY + 1 <= 0) {
                        currentY = 0
                    }
                }
            }
        }
        gCodeWriteHandler.clearLastPoint()
        if (isFinish) {
            gCodeWriteHandler.onPathEnd()
        }
    }
    return outputFile
}

/**将GCode字符串, 转换成Android的[Path]
 * [Path.toDrawable]*/
fun String.toGCodePath() = BitmapHandle.parseGCode(this, lastContext)

/**扩展*/
@Deprecated("请使用性能更好的Jni方法:[String.toGCodePath]")
private fun GCodeHelper.parseGCode(gCodeText: String?): GCodeDrawable? =
    parseGCode(gCodeText, createPaint(Color.BLACK))

//endregion ---GCode数据生成---