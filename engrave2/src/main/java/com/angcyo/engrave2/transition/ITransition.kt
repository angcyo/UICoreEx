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
    fun covertBitmap2Bytes(bitmap: Bitmap): ByteArray

    /**将图片[bitmap]转换成的路径数据*/
    fun covertBitmap2BP(bitmap: Bitmap): List<BitmapPath>

    /**将图片[bitmap]转换成的抖动数据
     * [compress] 是否要压缩数据, 将8个像素合并成1位, 否则1个像素1字节*/
    fun covertBitmap2Dithering(bitmap: Bitmap, compress: Boolean): Pair<List<String>, ByteArray>

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