package com.angcyo.engrave2.transition

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Path
import android.graphics.RectF
import android.view.Gravity
import com.angcyo.bitmap.handle.BitmapHandle
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.engrave2.data.TransitionParam
import com.angcyo.gcode.GCodeWriteHandler
import com.angcyo.laserpacker.device.DeviceHelper._defaultGCodeOutputFile
import com.angcyo.library.annotation.MM
import com.angcyo.library.annotation.Pixel
import com.angcyo.library.app
import com.angcyo.library.component.hawk.LibHawkKeys
import com.angcyo.library.ex.bounds
import com.angcyo.library.ex.deleteSafe
import com.angcyo.library.ex.rotate
import com.angcyo.library.ex.transform
import com.angcyo.library.unit.IValueUnit
import com.angcyo.library.unit.toMm
import com.angcyo.library.unit.toPixel
import com.angcyo.opencv.OpenCV
import java.io.File
import java.io.FileOutputStream

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/29
 */
class SimpleTransition : ITransition {

    override fun covertBitmap2BytesJni(
        bitmap: Bitmap,
        outputFilePath: String?,
        orientation: Int
    ): Boolean = bitmap.toColorBytes(outputFilePath, HawkEngraveKeys.grayChannelType, orientation)

    override fun covertBitmap2BPJni(
        bitmap: Bitmap,
        outputFilePath: String?,
        logFilePath: String?,
        grayThreshold: Int,
        alphaThreshold: Int,
        orientation: Int
    ): Long = bitmap.toBitmapPathJni(
        outputFilePath,
        logFilePath,
        grayThreshold,
        alphaThreshold,
        orientation
    )

    override fun covertBitmap2DitheringJni(
        bitmap: Bitmap,
        outputFilePath: String?,
        logFilePath: String?,
        grayThreshold: Int,
        alphaThreshold: Int,
        compress: Boolean,
        orientation: Int
    ): Boolean = bitmap.toBitmapByteJni(
        outputFilePath,
        logFilePath,
        grayThreshold,
        alphaThreshold,
        compress,
        orientation
    )

    override fun covertBitmap2GCode(bitmap: Bitmap, bounds: RectF, params: TransitionParam): File {
        val file = OpenCV.bitmapToGCode(
            app(),
            bitmap,
            (1 / 1f.toPixel()).toDouble(),
            direction = 0,
            angle = 0.0,
            type = 2 //只获取轮廓
        )
        val gCodeText = file.readText()
        file.deleteSafe()
        //添加关闭激光的指令
        return gCodeTranslation(
            "$gCodeText\nM2",
            bounds.left,
            bounds.top,
            params
        )
    }

    override fun covertBitmapPixel2GCode(
        bitmap: Bitmap,
        bounds: RectF,
        params: TransitionParam
    ): File {
        val scanGravity = if (bounds.width() > bounds.height()) {
            //宽图
            Gravity.TOP
        } else {
            Gravity.LEFT
        }
        return bitmap.toGCode(
            scanGravity,
            bounds.left + params.gcodeOffsetLeft,
            bounds.top + params.gcodeOffsetTop,
            gapValue = params.pixelGCodeGapValue,
            autoCnc = params.isAutoCnc,
            isSingleLine = params.isSingleLine
        )
    }

    override fun covertPathStroke2GCode(pathList: List<Path>, params: TransitionParam): File {
        //转换成GCode
        val gCodeHandler = GCodeWriteHandler()
        gCodeHandler.unit = IValueUnit.MM_UNIT
        gCodeHandler.isAutoCnc = params.isAutoCnc

        //平移
        var targetPathList = pathList
        params.translatePixelMatrix?.let {
            targetPathList = targetPathList.transform(it)
        }

        val outputFile = _defaultGCodeOutputFile()
        FileOutputStream(outputFile, false).writer().use { writer ->
            gCodeHandler.writer = writer
            gCodeHandler.enableGCodeShrink = params.enableGCodeShrink
            gCodeHandler.pathStrokeToVector(
                targetPathList,
                true,
                true,
                0f,
                0f,
                LibHawkKeys.pathAcceptableError
            )
        }
        return outputFile
    }

    /**调整GCode
     * 会强制将G21英寸单位转换成G20毫米单位
     * */
    override fun adjustGCode(gcodeText: String, @MM matrix: Matrix, params: TransitionParam): File {
        val outputFile = _defaultGCodeOutputFile()
        var targetMatrix = matrix
        params.translateMatrix?.let {
            targetMatrix = Matrix(matrix).apply {
                postConcat(it)
            }
        }
        BitmapHandle.adjustGCode(
            gcodeText,
            targetMatrix,
            outputFile.absolutePath,
            params.enableGCodeShrink
        )
        return outputFile
    }

    /**GCode数据坐标平移
     * [gCode] 原始的GCode数据
     * [rotateBounds] 旋转后的bounds, 用来确定Left,Top坐标
     * [rotate] 旋转角度, 配合[bounds]实现平移
     * */
    private fun gCodeTranslation(
        gCode: String,
        @Pixel offsetLeft: Float,
        @Pixel offsetTop: Float,
        params: TransitionParam
    ): File {
        val matrix = Matrix()
        matrix.setTranslate(offsetLeft.toMm(), offsetTop.toMm())
        return adjustGCode(gCode, matrix, params)
        /*val gCodeAdjust = GCodeAdjust()
        val outputFile = _defaultGCodeOutputFile()
        outputFile.writer().use { writer ->
            gCodeAdjust.gCodeTranslation(gCode, rotateBounds.left, rotateBounds.top, writer)
        }
        return outputFile*/
    }
}