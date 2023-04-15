package com.angcyo.engrave2.transition

import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.RectF
import android.view.Gravity
import com.angcyo.engrave2.data.BitmapPath
import com.angcyo.engrave2.data.TransitionParam
import com.angcyo.gcode.GCodeAdjust
import com.angcyo.gcode.GCodeWriteHandler
import com.angcyo.laserpacker.device.DeviceHelper._defaultGCodeOutputFile
import com.angcyo.library.app
import com.angcyo.library.component.hawk.LibHawkKeys
import com.angcyo.library.ex.bounds
import com.angcyo.library.ex.deleteSafe
import com.angcyo.library.ex.rotate
import com.angcyo.library.unit.IValueUnit
import com.angcyo.library.unit.toPixel
import com.angcyo.opencv.OpenCV
import java.io.File
import java.io.FileOutputStream

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/29
 */
class SimpleTransition : ITransition {

    override fun covertBitmap2Bytes(bitmap: Bitmap) = bitmap.engraveColorBytes()

    override fun covertBitmap2BytesJni(bitmap: Bitmap, outputFilePath: String?): Boolean =
        bitmap.toColorBytes(outputFilePath)

    override fun covertBitmap2BP(bitmap: Bitmap): List<BitmapPath> =
        bitmap.toBitmapPath(LibHawkKeys.grayThreshold)

    override fun covertBitmap2BPJni(
        bitmap: Bitmap,
        outputFilePath: String?,
        logFilePath: String?,
        grayThreshold: Int,
        alphaThreshold: Int
    ): Long = bitmap.toBitmapPathJni(outputFilePath, logFilePath, grayThreshold, alphaThreshold)

    override fun covertBitmap2Dithering(
        bitmap: Bitmap,
        compress: Boolean
    ): Pair<List<String>, ByteArray> {
        return if (compress) {
            bitmap.toBitmapByte(LibHawkKeys.grayThreshold)
        } else {
            bitmap.toBitmapByteUncompress(LibHawkKeys.grayThreshold)
        }
    }

    override fun covertBitmap2DitheringJni(
        bitmap: Bitmap,
        outputFilePath: String?,
        logFilePath: String?,
        grayThreshold: Int,
        alphaThreshold: Int,
        compress: Boolean
    ): Boolean =
        bitmap.toBitmapByteJni(outputFilePath, logFilePath, grayThreshold, alphaThreshold, compress)

    override fun covertBitmap2GCode(bitmap: Bitmap, bounds: RectF): File {
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
        return gCodeTranslation(gCodeText, bounds)
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
            bounds.left,
            bounds.top,
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

        val outputFile = _defaultGCodeOutputFile()
        FileOutputStream(outputFile, false).writer().use { writer ->
            gCodeHandler.writer = writer
            gCodeHandler.pathStrokeToVector(
                pathList,
                true,
                true,
                0f,
                0f,
                LibHawkKeys.pathAcceptableError
            )
        }
        return outputFile
    }


    /**GCode数据坐标平移
     * [gCode] 原始的GCode数据
     * [rotateBounds] 旋转后的bounds, 用来确定Left,Top坐标
     * [rotate] 旋转角度, 配合[bounds]实现平移
     * */
    private fun gCodeTranslation(
        gCode: String,
        rotateBounds: RectF,
        outputFile: File = _defaultGCodeOutputFile()
    ): File {
        val gCodeAdjust = GCodeAdjust()
        outputFile.writer().use { writer ->
            gCodeAdjust.gCodeTranslation(gCode, rotateBounds.left, rotateBounds.top, writer)
        }
        return outputFile
    }

}