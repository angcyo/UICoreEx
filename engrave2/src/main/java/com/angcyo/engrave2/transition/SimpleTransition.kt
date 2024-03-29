package com.angcyo.engrave2.transition

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.view.Gravity
import com.angcyo.bitmap.handle.BitmapHandle
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.bean._pathTolerance
import com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd
import com.angcyo.engrave2.data.TransitionParam
import com.angcyo.gcode.CollectPoint
import com.angcyo.gcode.GCodeWriteHandler
import com.angcyo.gcode.toCollectPointList
import com.angcyo.gcode.toPointArray
import com.angcyo.laserpacker.device.DeviceHelper._defaultGCodeOutputFile
import com.angcyo.library.annotation.MM
import com.angcyo.library.annotation.Pixel
import com.angcyo.library.app
import com.angcyo.library.component.hawk.LibHawkKeys
import com.angcyo.library.component.hawk.LibLpHawkKeys
import com.angcyo.library.ex.bounds
import com.angcyo.library.ex.computePathBounds
import com.angcyo.library.ex.deleteSafe
import com.angcyo.library.ex.readText
import com.angcyo.library.ex.rotate
import com.angcyo.library.ex.transform
import com.angcyo.library.libCacheFile
import com.angcyo.library.unit.IValueUnit
import com.angcyo.library.unit.toMm
import com.angcyo.library.unit.toPixel
import com.angcyo.library.utils.writeTo
import com.angcyo.opencv.OpenCV
import com.hingin.lp1.hiprint.rust.LdsCore
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

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

    override fun covertBitmap2GCode(
        bitmap: Bitmap,
        bitmapPath: String?,
        bounds: RectF,
        params: TransitionParam
    ): File {
        val file = OpenCV.bitmapToGCode(
            app(),
            bitmap,
            (1 / 1f.toPixel()).toDouble(),
            params.bitmapToGCodeLineSpace,
            0,
            0.0,
            type = params.bitmapToGCodeType, //只获取轮廓
            isLast = params.bitmapToGCodeIsLast,
            boundFirst = params.bitmapToGCodeBoundFirst,
            /*1~10*/
            numPixel = (params.pathStep?.toPixel()
                ?: LibHawkKeys._pathAcceptableError).roundToInt(),
            autoLaser = params.isAutoCnc,
            bitmapPath = bitmapPath,
        )
        val gCodeText = file.readText() + "\n" //2023-12-5
        file.deleteSafe()
        //添加关闭激光的指令

        /*val gcode = if (params.bitmapToGCodeIsLast) {
            "$gCodeText\nM2"
        } else {
            gCodeText
        }*/

        return gCodeTranslation(
            gCodeText,
            bounds.left,
            bounds.top,
            params
        )
    }

    override fun covertBitmap2GCodePoint(
        bitmap: Bitmap,
        bitmapPath: String?,
        bounds: RectF,
        params: TransitionParam
    ): List<CollectPoint> {
        val outputFile = OpenCV.bitmapToGCode(
            app(),
            bitmap,
            (1 / 1f.toPixel()).toDouble(),
            params.bitmapToGCodeLineSpace,
            0,
            0.0,
            type = params.bitmapToGCodeType, //只获取轮廓
            isLast = params.bitmapToGCodeIsLast,
            boundFirst = params.bitmapToGCodeBoundFirst,
            /*1~10*/
            numPixel = (params.pathStep?.toPixel()
                ?: LibHawkKeys._pathAcceptableError).roundToInt(),
            autoLaser = params.isAutoCnc,
            bitmapPath = bitmapPath,
            outputGcode = false
        )
        val result = mutableListOf<CollectPoint>()
        //x,y:x,y;x,y:x,y;
        val pointString = outputFile.readText()
        pointString?.let {
            it.writeTo(libCacheFile("point.txt"), false)
            it.split(";").forEach { lines ->
                val pointList = mutableListOf<PointF>()
                lines.split(":").forEach { points ->
                    val xy = points.split(",")
                    if (xy.size >= 2) {
                        val x = xy[0].toFloatOrNull()
                        val y = xy[1].toFloatOrNull()
                        if (x != null && y != null) {
                            pointList.add(PointF(x, y))
                        }
                    }
                }
                if (pointList.isNotEmpty()) {
                    result.add(CollectPoint(pointList))
                }
            }
        }
        outputFile.deleteSafe()
        return result
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
            isSingleLine = params.isSingleLine,
            isFinish = params.bitmapToGCodeIsLast
        )
    }

    override fun covertPathStroke2GCode(
        pathList: List<Path>,
        params: TransitionParam
    ): PathDataFile {
        //转换成GCode
        val gCodeHandler = GCodeWriteHandler()
        gCodeHandler.unit = IValueUnit.MM_UNIT
        gCodeHandler.isAutoCnc = params.isAutoCnc
        gCodeHandler.isCollectPoint = params.gcodeUsePathData

        //2024-2-22
        val needGCodePathOpt = params.enableGCodePathOpt && !params.enableGCodeCutData
        if (needGCodePathOpt) {
            gCodeHandler.gcodeHeader = ""
            gCodeHandler.gcodeFooter = ""
            gCodeHandler.turnOn = ""
            gCodeHandler.turnOff = ""
        }

        //2023-11-8
        gCodeHandler.enableVectorRadiansSample = LibLpHawkKeys.enableVectorRadiansSample
        gCodeHandler.pathSampleStepRadians = LibHawkKeys.pathSampleStepRadians
        gCodeHandler.updatePathToleranceByPixel(
            (params.pathTolerance ?: _pathTolerance).toPixel()
        )

        //平移
        var targetPathList = pathList
        params.translatePixelMatrix?.let {
            targetPathList = targetPathList.transform(it)
        }

        val outputFile = _defaultGCodeOutputFile()
        FileOutputStream(outputFile, false).writer().use { writer ->
            gCodeHandler.writer = writer
            gCodeHandler.enableGCodeShrink = params.enableGCodeShrink
            gCodeHandler.enableGCodeCut = params.enableGCodeCutData
            gCodeHandler.cutLoopCount = params.cutLoopCount ?: gCodeHandler.cutLoopCount
            gCodeHandler.cutGCodeWidth = params.cutGCodeWidth ?: gCodeHandler.cutGCodeWidth
            gCodeHandler.cutGCodeHeight = params.cutGCodeHeight ?: gCodeHandler.cutGCodeHeight
            //2023-12-29
            gCodeHandler.cutLimitRect = EngravePreviewCmd.getBoundsPath()
                ?.computePathBounds() //targetPathList.computePathBounds() //
            //2023-10-19
            @Pixel
            val pathStep = params.pathStep?.toPixel() ?: LibHawkKeys._pathAcceptableError
            gCodeHandler.updatePathStepByPixel(pathStep)
            //2023-12-18
            gCodeHandler.needCloseGcodeFile = !params.disableGcodeM2Range
            gCodeHandler.needMoveToOrigin = params.gcodeMoveToOriginRange
            gCodeHandler.pathStrokeToVector(targetPathList, true, true, 0f, 0f, pathStep)
        }

        //手机到的点坐标
        var collectPointList: List<CollectPoint>? = null
        if (gCodeHandler.isCollectPoint) {
            collectPointList = gCodeHandler._collectPointList
        }

        if (needGCodePathOpt) {
            //优化GCode, 并返回优化后的GCode数据
            val text = outputFile.readText()
            val newText = LdsCore.gcodeOptimiser(text, buildString {
                gCodeHandler.writeGCodeHeader(this)
            }, buildString {
                gCodeHandler.writeGCodeFooter(this)
            }, buildString {
                gCodeHandler.writeTurnOn(this)
            }, buildString {
                gCodeHandler.writeTurnOff(this)
            })
            //newText.writeTo(libCacheFile("test-gcode.gc"), false)
            newText?.writeTo(outputFile, false)//覆盖原始的GCode文件

            val array = collectPointList?.toPointArray()
            if (array != null) {
                //路径优化
                collectPointList = LdsCore.pointsOptimiser(array).toCollectPointList()
            }
            //gCodeHandler._collectPointList =
        }

        val result = PathDataFile(outputFile)
        if (gCodeHandler.isCollectPoint) {
            result.collectPointList = collectPointList
        }
        return result
    }

    /**调整GCode
     * 会强制将G21英寸单位转换成G20毫米单位
     * */
    override fun adjustGCode(
        gcodeText: String,
        @MM matrix: Matrix,
        params: TransitionParam
    ): PathDataFile {
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
        return PathDataFile(outputFile)
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
        return adjustGCode(gCode, matrix, params).targetFile
        /*val gCodeAdjust = GCodeAdjust()
        val outputFile = _defaultGCodeOutputFile()
        outputFile.writer().use { writer ->
            gCodeAdjust.gCodeTranslation(gCode, rotateBounds.left, rotateBounds.top, writer)
        }
        return outputFile*/
    }
}