package com.angcyo.engrave2.transition

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Path
import android.graphics.RectF
import com.angcyo.engrave2.data.TransitionParam
import com.angcyo.gcode.CollectPoint
import java.io.File

/**
 * 数据转换算法/接口
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/29
 */
interface ITransition {

    /**
     * [outputFilePath] 数据写入到此文件
     * [covertBitmap2Bytes]*/
    fun covertBitmap2BytesJni(bitmap: Bitmap, outputFilePath: String?, orientation: Int): Boolean

    /**[covertBitmap2BP]
     * 返回的是数据的段数*/
    fun covertBitmap2BPJni(
        bitmap: Bitmap,
        outputFilePath: String?, //数据写入到此文件
        logFilePath: String?,  //日志写入到此文件
        grayThreshold: Int,
        alphaThreshold: Int,
        orientation: Int
    ): Long

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
        alphaThreshold: Int,
        compress: Boolean,
        orientation: Int
    ): Boolean

    /**将图片[bitmap]转换成的GCode数据
     * [bitmapPath] 图片路径
     * [bounds] 用来平移GCode到这个坐标*/
    fun covertBitmap2GCode(
        bitmap: Bitmap,
        bitmapPath: String?,
        bounds: RectF,
        params: TransitionParam
    ): File

    /**[covertBitmap2GCode]*/
    fun covertBitmap2GCodePoint(
        bitmap: Bitmap,
        bitmapPath: String?,
        bounds: RectF,
        params: TransitionParam
    ): List<CollectPoint>

    /**将图片[bitmap]转换成的GCode数据, 用像素的方式转换数据
     * [bounds] 用来平移GCode到这个坐标
     *
     * [covertBitmap2GCode]*/
    fun covertBitmapPixel2GCode(bitmap: Bitmap, bounds: RectF, params: TransitionParam): File

    /**将路径[pathList]转换成的GCode数据
     * [pathList] 数据应该是所有缩放/旋转/倾斜之后的数据*/
    fun covertPathStroke2GCode(pathList: List<Path>, params: TransitionParam): PathDataFile

    /**调整原始的GCode数据, 单位会被强制转换成mm单位*/
    fun adjustGCode(gcodeText: String, matrix: Matrix, params: TransitionParam): PathDataFile
}