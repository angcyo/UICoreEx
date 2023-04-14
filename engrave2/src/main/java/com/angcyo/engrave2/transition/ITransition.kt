package com.angcyo.engrave2.transition

import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.RectF
import com.angcyo.engrave2.data.BitmapPath
import com.angcyo.engrave2.data.TransitionParam
import java.io.File

/**
 * 数据转换算法/接口
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/29
 */
interface ITransition {

    /**将图片[bitmap]转换成机器雕刻的字节数据,
     * 通常取图片的[Color.RED]通道颜色即可*/
    @Deprecated("请使用性能更好的[covertBitmap2BytesJni]方法")
    fun covertBitmap2Bytes(bitmap: Bitmap): ByteArray

    /**
     * [outputFilePath] 数据写入到此文件
     * [covertBitmap2Bytes]*/
    fun covertBitmap2BytesJni(bitmap: Bitmap, outputFilePath: String?): Boolean

    /**将图片[bitmap]转换成的路径数据*/
    @Deprecated("请使用性能更好的[covertBitmap2BytesJni]方法")
    fun covertBitmap2BP(bitmap: Bitmap): List<BitmapPath>

    /**[covertBitmap2BP]
     * 返回的是数据的段数*/
    fun covertBitmap2BPJni(
        bitmap: Bitmap,
        outputFilePath: String?, //数据写入到此文件
        logFilePath: String?,  //日志写入到此文件
        grayThreshold: Int,
    ): Int

    /**将图片[bitmap]转换成的抖动数据
     * [compress] 是否要压缩数据, 将8个像素合并成1位, 否则1个像素1字节*/
    @Deprecated("请使用性能更好的[covertBitmap2DitheringJni]方法")
    fun covertBitmap2Dithering(bitmap: Bitmap, compress: Boolean): Pair<List<String>, ByteArray>

    /**[outputFilePath] 抖动后的数据输出路径
     * [logFilePath] 010101日志输出路径
     * [grayThreshold] 灰度阈值, 小于等于此值视为黑色
     * [compress] 是否要压缩数据
     *
     * [covertBitmap2Dithering]*/
    fun covertBitmap2DitheringJni(
        bitmap: Bitmap,
        outputFilePath: String?, //数据写入到此文件
        logFilePath: String?,  //日志写入到此文件
        grayThreshold: Int,
        compress: Boolean
    ): Boolean

    /**将图片[bitmap]转换成的GCode数据
     * [bounds] 用来平移GCode到这个坐标*/
    fun covertBitmap2GCode(bitmap: Bitmap, bounds: RectF): File

    /**将图片[bitmap]转换成的GCode数据, 用像素的方式转换数据
     * [bounds] 用来平移GCode到这个坐标
     *
     * [covertBitmap2GCode]*/
    fun covertBitmapPixel2GCode(bitmap: Bitmap, bounds: RectF, params: TransitionParam): File

    /**将路径[pathList]转换成的GCode数据
     * [pathList] 数据应该是所有缩放/旋转/倾斜之后的数据*/
    fun covertPathStroke2GCode(pathList: List<Path>, params: TransitionParam): File
}