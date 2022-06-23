package com.angcyo.engrave

import android.graphics.Paint
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd
import com.angcyo.canvas.LinePath
import com.angcyo.canvas.items.PictureShapeItem
import com.angcyo.canvas.items.renderer.BaseItemRenderer
import com.angcyo.canvas.utils.*
import com.angcyo.core.component.file.writeTo
import com.angcyo.core.vmApp
import com.angcyo.engrave.data.EngraveDataInfo
import com.angcyo.engrave.model.EngraveModel
import com.angcyo.library.ex.deleteSafe
import com.angcyo.library.ex.lines
import com.angcyo.library.ex.readText
import com.angcyo.library.ex.toBitmap

/**
 * 雕刻助手
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/21
 */
object EngraveHelper {

    /**生成一个雕刻需要用到的文件名*/
    fun generateEngraveName(): Int {
        return (System.currentTimeMillis() / 1000).toInt()
    }

    /**从[renderer]中获取需要雕刻的数据*/
    fun handleEngraveData(renderer: BaseItemRenderer<*>?): EngraveDataInfo? {
        var result: EngraveDataInfo? = null
        val item = renderer?.getRendererItem() ?: return result

        //打印的文件名
        val name = generateEngraveName()

        //GCode
        val gCodeText = renderer.getGCodeText()
        if (!gCodeText.isNullOrEmpty()) {
            //GCode数据
            val gCodeFile =
                CanvasDataHandleHelper.gCodeAdjust(gCodeText, renderer.getBounds(), renderer.rotate)
            val gCodeData = gCodeFile.readText()
            val gCodeLines = gCodeFile.lines()
            gCodeFile.deleteSafe()
            if (!gCodeData.isNullOrEmpty()) {
                return _handleGCodeEngraveData(name, gCodeData, gCodeLines)
            }
        }

        //SVG
        val svgPathList = renderer.getPathList()
        if (!svgPathList.isNullOrEmpty()) {
            //path路径
            val gCodeFile = CanvasDataHandleHelper.pathStrokeToGCode(
                svgPathList,
                renderer.getBounds(),
                renderer.rotate
            )
            val pathGCodeText = gCodeFile.readText()
            val gCodeLines = gCodeFile.lines()
            gCodeFile.deleteSafe()

            if (!pathGCodeText.isNullOrEmpty()) {
                //GCode数据
                return _handleGCodeEngraveData(name, pathGCodeText, gCodeLines)
            }
        }

        if (item is PictureShapeItem) {
            if (item.paint.style == Paint.Style.STROKE && item.shapePath !is LinePath) {
                //描边时, 才处理成GCode. 并且不是线段
                val path = item.shapePath
                if (path != null) {
                    val gCodeFile = CanvasDataHandleHelper.pathStrokeToGCode(
                        path,
                        renderer.getRotateBounds(),
                        renderer.rotate
                    )
                    val pathGCodeText = gCodeFile.readText()
                    val gCodeLines = gCodeFile.lines()
                    gCodeFile.deleteSafe()
                    if (!pathGCodeText.isNullOrEmpty()) {
                        //GCode数据
                        return _handleGCodeEngraveData(name, pathGCodeText, gCodeLines)
                    }
                }
            } else {
                //填充情况下, 使用bitmap转gcode
                val bitmap = renderer.preview()?.toBitmap() ?: return result
                //OpenCV.bitmapToGCode(app(), bitmap)
                var gCodeFile = CanvasDataHandleHelper.bitmapToGCode(bitmap)
                val rotate = 0f//renderer.rotate
                val gCodeString = gCodeFile.readText()
                gCodeFile.deleteSafe()
                if (!gCodeString.isNullOrEmpty()) {
                    //GCode数据
                    gCodeFile = CanvasDataHandleHelper.gCodeTranslation(
                        gCodeString,
                        renderer.getRotateBounds()
                    )
                    /*CanvasDataHandleHelper.gCodeAdjust(
                        gCodeString,
                        renderer.getBounds(),
                        rotate
                    )*/ //这里只需要平移GCode即可
                    val gCodeData = gCodeFile.readText()
                    val gCodeLines = gCodeFile.lines()
                    gCodeFile.deleteSafe()
                    if (!gCodeData.isNullOrEmpty()) {
                        return _handleGCodeEngraveData(name, gCodeData, gCodeLines)
                    }
                }
            }
        } else {
            //其他方式, 使用图片雕刻
            val bounds = renderer.getRotateBounds()
            var bitmap = renderer.preview()?.toBitmap() ?: return result

            val width = bitmap.width
            val height = bitmap.height

            val px: Byte = vmApp<EngraveModel>().engraveOptionInfoData.value!!.px

            bitmap = LaserPeckerHelper.bitmapScale(bitmap, px)

            val x = bounds.left.toInt()
            val y = bounds.top.toInt()

            val data = bitmap.engraveColorBytes()

            val channelBitmap = data.toEngraveBitmap(bitmap.width, bitmap.height)
            saveEngraveData(name, channelBitmap, "bitmap")

            //二进制数据
            saveEngraveData(name, data, "engrave")

            //根据px, 修正坐标
            val rect = EngravePreviewCmd.adjustBitmapRange(x, y, width, height, px)

            //雕刻的宽高使用图片本身的宽高, 否则如果宽高和数据不一致,会导致图片打印出来是倾斜的效果
            val engraveWidth = bitmap.width
            val engraveHeight = bitmap.height

            result = EngraveDataInfo(
                EngraveDataInfo.TYPE_BITMAP,
                data,
                engraveWidth,
                engraveHeight,
                rect.left,
                rect.top,
                px,
                name
            )
        }
        return result
    }

    /**保存雕刻数据到文件*/
    fun saveEngraveData(name: Int, data: Any, suffix: String = "engrave") {
        //将雕刻数据写入文件
        data.writeTo(CanvasDataHandleHelper.CACHE_FILE_FOLDER, "${name}.${suffix}")
    }

    //返回GCode雕刻的数据
    fun _handleGCodeEngraveData(name: Int, data: String, lines: Int): EngraveDataInfo {
        saveEngraveData(name, data, "gcode")
        return EngraveDataInfo(
            EngraveDataInfo.TYPE_GCODE,
            data.toByteArray(),
            name = name,
            lines = lines
        )
    }
}