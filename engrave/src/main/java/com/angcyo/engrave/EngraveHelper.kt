package com.angcyo.engrave

import android.graphics.Bitmap
import android.graphics.Paint
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd
import com.angcyo.canvas.LinePath
import com.angcyo.canvas.items.PictureShapeItem
import com.angcyo.canvas.items.renderer.BaseItemRenderer
import com.angcyo.canvas.utils.*
import com.angcyo.core.component.file.writeTo
import com.angcyo.engrave.canvas.CanvasBitmapHandler
import com.angcyo.engrave.data.EngraveDataInfo
import com.angcyo.gcode.GCodeHelper
import com.angcyo.library.ex.deleteSafe
import com.angcyo.library.ex.lines
import com.angcyo.library.ex.readText
import com.angcyo.library.ex.toBitmap
import java.io.File

/**
 * 雕刻助手
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/21
 */
object EngraveHelper {

    /**生成一个雕刻需要用到的文件索引*/
    fun generateEngraveIndex(): Int {
        return (System.currentTimeMillis() / 1000).toInt()
    }

    /**创建一个雕刻数据, 不处理雕刻数据, 加快响应速度*/
    fun generateEngraveDataInfo(renderer: BaseItemRenderer<*>?): EngraveDataInfo? {
        var result: EngraveDataInfo? = null
        val item = renderer?.getRendererItem() ?: return result
        result = EngraveDataInfo()

        //打印的文件索引
        val index = generateEngraveIndex()
        result.index = index
        result.name = item.itemName?.toString()
        result.rendererItemUuid = item.uuid

        val gCodeText = renderer.getGCodeText()
        if (!gCodeText.isNullOrEmpty()) {
            //GCode数据
            result.dataType = EngraveDataInfo.TYPE_GCODE
            return result
        }

        val svgPathList = renderer.getPathList()
        if (!svgPathList.isNullOrEmpty()) {
            //path路径
            result.dataType = EngraveDataInfo.TYPE_GCODE
            return result
        }

        if (item is PictureShapeItem) {
            result.dataType = EngraveDataInfo.TYPE_GCODE
            if (item.paint.style == Paint.Style.STROKE && item.shapePath !is LinePath) {
                //
            } else {
                result.optionMode = CanvasBitmapHandler.BITMAP_MODE_GCODE
                result.optionSupportModeList = listOf(
                    CanvasBitmapHandler.BITMAP_MODE_GREY,
                    CanvasBitmapHandler.BITMAP_MODE_BLACK_WHITE,
                    CanvasBitmapHandler.BITMAP_MODE_GCODE
                )
            }
            return result
        } else {
            result.px = LaserPeckerHelper.DEFAULT_PX
            result.dataType = EngraveDataInfo.TYPE_BITMAP

            //px list
            result.optionSupportPxList = LaserPeckerHelper.findProductSupportPxList()
        }
        return result
    }

    /**处理需要打印的雕刻数据, 保存对应的数据等*/
    fun handleEngraveData(renderer: BaseItemRenderer<*>?, info: EngraveDataInfo): EngraveDataInfo {
        val item = renderer?.getRendererItem() ?: return info

        //GCode
        val gCodeText = renderer.getGCodeText()
        if (!gCodeText.isNullOrEmpty()) {
            //GCode数据
            val gCodeFile = CanvasDataHandleOperate.gCodeAdjust(
                gCodeText,
                renderer.getBounds(),
                renderer.rotate
            )
            _handleGCodeEngraveDataInfo(info, gCodeFile)
            return info
        }

        //SVG
        val svgPathList = renderer.getPathList()
        if (!svgPathList.isNullOrEmpty()) {
            //path路径
            val gCodeFile = CanvasDataHandleOperate.pathStrokeToGCode(
                svgPathList,
                renderer.getBounds(),
                renderer.rotate
            )
            _handleGCodeEngraveDataInfo(info, gCodeFile)
            return info
        }

        //其他
        if (item is PictureShapeItem) {
            if (item.paint.style == Paint.Style.STROKE && item.shapePath !is LinePath) {
                //描边时, 才处理成GCode. 并且不是线段
                val path = item.shapePath
                if (path != null) {
                    val gCodeFile = CanvasDataHandleOperate.pathStrokeToGCode(
                        path,
                        renderer.getRotateBounds(),
                        renderer.rotate
                    )
                    _handleGCodeEngraveDataInfo(info, gCodeFile)
                    return info
                }
            } else {
                //填充情况下, 使用bitmap转gcode
                val bitmap = renderer.preview()?.toBitmap() ?: return info
                //OpenCV.bitmapToGCode(app(), bitmap)
                var gCodeFile = CanvasDataHandleOperate.bitmapToGCode(bitmap)
                val rotate = 0f//renderer.rotate
                val gCodeString = gCodeFile.readText()
                gCodeFile.deleteSafe()
                if (!gCodeString.isNullOrEmpty()) {
                    //GCode数据
                    gCodeFile = CanvasDataHandleOperate.gCodeTranslation(
                        gCodeString,
                        renderer.getRotateBounds()
                    )
                    /*CanvasDataHandleHelper.gCodeAdjust(
                        gCodeString,
                        renderer.getBounds(),
                        rotate
                    )*/ //这里只需要平移GCode即可
                    _handleGCodeEngraveDataInfo(info, gCodeFile)
                    return info
                }
            }
        } else {
            //其他方式, 使用图片雕刻
            val bounds = renderer.getRotateBounds()
            val bitmap = renderer.preview()?.toBitmap() ?: return info

            val x = bounds.left.toInt()
            val y = bounds.top.toInt()

            info.dataType = EngraveDataInfo.TYPE_BITMAP
            info.optionBitmap = bitmap
            info.optionX = x
            info.optionY = y

            updateBitmapPx(info, info.px)
        }

        return info
    }

    /**更新雕刻图片的分辨率
     * [px] 图片需要调整到的分辨率*/
    fun updateBitmapPx(info: EngraveDataInfo, px: Byte) {
        var bitmap = info.optionBitmap ?: return
        val width = bitmap.width
        val height = bitmap.height

        //scale
        bitmap = LaserPeckerHelper.bitmapScale(bitmap, px)

        //雕刻的数据
        val data = bitmap.engraveColorBytes()
        //保存一份byte数据
        info.dataPath = saveEngraveData(info.index, data)//数据路径

        //保存一份可视化的数据
        val channelBitmap = data.toEngraveBitmap(bitmap.width, bitmap.height)
        saveEngraveData(info.index, channelBitmap, "png")

        //根据px, 修正坐标
        val x = info.optionX
        val y = info.optionY
        val rect = EngravePreviewCmd.adjustBitmapRange(x, y, width, height, px)

        //雕刻的宽高使用图片本身的宽高, 否则如果宽高和数据不一致,会导致图片打印出来是倾斜的效果
        val engraveWidth = bitmap.width
        val engraveHeight = bitmap.height

        info.data = data
        info.x = rect.left
        info.y = rect.top
        info.width = engraveWidth
        info.height = engraveHeight
        info.px = px
    }

    /**更新雕刻时的数据模式, 比如图片可以转GCode, 灰度, 黑白数据等*/
    fun updateDataMode(info: EngraveDataInfo) {

    }

    /**保存雕刻数据到文件
     * [data]
     *   [String]
     *   [ByteArray]
     *   [Bitmap]
     * ]*/
    fun saveEngraveData(name: Int, data: Any, suffix: String = "engrave"): String? {
        //将雕刻数据写入文件
        return data.writeTo(CanvasDataHandleOperate.CACHE_FILE_FOLDER, "${name}.${suffix}")
    }

    fun _handleGCodeEngraveDataInfo(info: EngraveDataInfo, gCodeFile: File) {
        val pathGCodeText = gCodeFile.readText()
        val gCodeLines = gCodeFile.lines()
        gCodeFile.deleteSafe()

        if (!pathGCodeText.isNullOrEmpty()) {
            //GCode数据

            info.dataType = EngraveDataInfo.TYPE_GCODE
            info.lines = gCodeLines
            val data = pathGCodeText.toByteArray()
            info.data = data

            //保存一份byte数据
            info.dataPath = saveEngraveData(info.index, data)//数据路径

            saveEngraveData(info.index, pathGCodeText, "gcode")
            info.optionBitmap = GCodeHelper.parseGCode(pathGCodeText)?.toBitmap()
        }
    }
}