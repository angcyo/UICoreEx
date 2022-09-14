package com.angcyo.engrave.transition

import android.graphics.Path
import android.graphics.RectF
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd
import com.angcyo.canvas.core.renderer.SelectGroupRenderer
import com.angcyo.canvas.items.PictureBitmapItem
import com.angcyo.canvas.items.PictureGCodeItem
import com.angcyo.canvas.items.PictureShapeItem
import com.angcyo.canvas.items.PictureSharpItem
import com.angcyo.canvas.items.renderer.BaseItemRenderer
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.canvas.utils.CanvasDataHandleOperate
import com.angcyo.canvas.utils.getEngraveBitmap
import com.angcyo.canvas.utils.parseGCode
import com.angcyo.engrave.data.EngraveReadyInfo
import com.angcyo.gcode.GCodeHelper
import com.angcyo.library.ex.deleteSafe
import com.angcyo.library.ex.lines
import com.angcyo.library.ex.readText
import com.angcyo.library.ex.toBitmap
import com.angcyo.library.unit.MmValueUnit
import com.angcyo.svg.StylePath
import java.io.File

/**
 * GCode数据转换, 什么item要处理成GCode数据
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/08/22
 */
class GCodeTransition : IEngraveTransition {

    override fun doTransitionReadyData(renderer: BaseItemRenderer<*>): EngraveReadyInfo? {
        val item = renderer.getRendererRenderItem() ?: return null
        val dataType = item.dataType

        var result: EngraveReadyInfo? = null

        //init
        fun initReadyInfo() {
            result?.apply {
                initReadyEngraveData(renderer, this)

                this.itemUuid = item.uuid
                this.dataType = item.dataType
                this.dataMode = CanvasConstant.DATA_MODE_GCODE

                dataSupportModeList
                dataSupportPxList
            }
        }

        //
        if (dataType == CanvasConstant.DATA_TYPE_SVG ||
            dataType == CanvasConstant.DATA_TYPE_GCODE ||
            dataType == CanvasConstant.DATA_TYPE_PATH
        ) {
            //GCode数据
            result = EngraveReadyInfo()
            initReadyInfo()
            return result
        }

        //
        if (item is PictureGCodeItem) {
            val gCodeText = item.gCode
            if (gCodeText.isNotEmpty()) {
                //GCode数据
                result = EngraveReadyInfo()
                initReadyInfo()
                return result
            }
        }

        //

        //group
        if (dataType == CanvasConstant.DATA_TYPE_GROUP) {
            if (renderer is SelectGroupRenderer) {
                renderer.selectItemList

                var allGCodeItem = true
                for (itemRenderer in renderer.selectItemList) {
                    val readyInfo = doTransitionReadyData(itemRenderer)
                    if (readyInfo?.dataMode == CanvasConstant.DATA_MODE_GCODE) {
                        allGCodeItem = true
                    } else {
                        allGCodeItem = false
                        break
                    }
                }
                if (allGCodeItem) {
                    //全部都是gcode数据
                    result = EngraveReadyInfo()
                    initReadyInfo()
                    return result
                }
            }
        }

        return null
    }

    override fun doTransitionEngraveData(
        renderer: BaseItemRenderer<*>,
        engraveReadyInfo: EngraveReadyInfo
    ): Boolean {
        val item = renderer.getRendererRenderItem()

        //init
        fun initEngraveData() {
            engraveReadyInfo.engraveData?.apply {
                engraveDataType = DataCmd.ENGRAVE_TYPE_GCODE
            }
        }

        if (engraveReadyInfo.dataMode == CanvasConstant.DATA_MODE_GCODE) {

            //GCode
            if (item is PictureGCodeItem) {
                val gCodeText = item.gCode
                if (gCodeText.isNotEmpty()) {
                    initEngraveData()
                    //GCode数据
                    val gCodeFile = CanvasDataHandleOperate.gCodeAdjust(
                        gCodeText,
                        renderer.getBounds(),
                        renderer.rotate
                    )
                    _handleGCodeEngraveDataInfo(
                        engraveReadyInfo,
                        gCodeFile,
                        renderer.getRotateBounds()
                    )
                    return true
                }
            }

            //bitmap gcode
            if (item is PictureBitmapItem) {
                val gCodeText = item.data
                if (gCodeText is String) {
                    if (gCodeText.isNotEmpty()) {
                        initEngraveData()
                        //GCode数据
                        val gCodeFile = CanvasDataHandleOperate.gCodeAdjust(
                            gCodeText,
                            renderer.getBounds(),
                            renderer.rotate
                        )
                        _handleGCodeEngraveDataInfo(
                            engraveReadyInfo,
                            gCodeFile,
                            renderer.getRotateBounds()
                        )
                        return true
                    }
                }
            }

            //SVG
            if (item is PictureSharpItem) {
                val svgPathList = item.sharpDrawable.pathList
                if (!svgPathList.isNullOrEmpty()) {
                    initEngraveData()
                    //path路径
                    val gCodeFile = CanvasDataHandleOperate.pathToGCode(
                        svgPathList,
                        renderer.getBounds(),
                        renderer.rotate
                    )
                    _handleGCodeEngraveDataInfo(
                        engraveReadyInfo,
                        gCodeFile,
                        renderer.getRotateBounds()
                    )
                    return true
                }
            }

            //path
            if (item is PictureShapeItem) {
                initEngraveData()
                val stylePath = StylePath()
                stylePath.style = renderer.paint.style

                val path = Path()
                item.transformPath(renderer, item.shapePath, path)

                stylePath.set(path)
                val gCodeFile = CanvasDataHandleOperate.pathToGCode(stylePath)
                _handleGCodeEngraveDataInfo(
                    engraveReadyInfo,
                    gCodeFile,
                    renderer.getRotateBounds()
                )
                return true
            }

            //group
            if (renderer is SelectGroupRenderer) {

            }

            //other
            //使用bitmap转gcode
            val bitmap = renderer.getEngraveBitmap()
            if (bitmap != null) {
                initEngraveData()
                var gCodeFile = CanvasDataHandleOperate.bitmapToGCode(bitmap)
                val gCodeString = gCodeFile.readText()
                gCodeFile.deleteSafe()
                if (!gCodeString.isNullOrEmpty()) {
                    //GCode数据
                    gCodeFile = CanvasDataHandleOperate.gCodeTranslation(
                        gCodeString,
                        renderer.getRotateBounds()
                    )
                    _handleGCodeEngraveDataInfo(
                        engraveReadyInfo,
                        gCodeFile,
                        renderer.getRotateBounds()
                    )
                    return true
                }
            }
        }

        return false
    }

    /**
     * [engraveReadyInfo] 需要赋值的数据对象
     * [gCodeFile] gcode数据文件对象
     * [rotateBounds] itemRenderer旋转后的矩形坐标, px坐标, 内部自行转换成mm
     * */
    fun _handleGCodeEngraveDataInfo(
        engraveReadyInfo: EngraveReadyInfo,
        gCodeFile: File,
        rotateBounds: RectF
    ) {
        val pathGCodeText = gCodeFile.readText()
        val gCodeLines = gCodeFile.lines()
        gCodeFile.deleteSafe()

        if (!pathGCodeText.isNullOrEmpty()) {
            //GCode数据

            val gCodeDrawable = GCodeHelper.parseGCode(pathGCodeText)
            val data = pathGCodeText.toByteArray()
            engraveReadyInfo.engraveData?.apply {
                engraveDataType = DataCmd.ENGRAVE_TYPE_GCODE
                lines = gCodeLines
                this.data = data

                val mmValueUnit = MmValueUnit()
                x = (mmValueUnit.convertPixelToValue(rotateBounds.left) * 10).toInt()
                y = (mmValueUnit.convertPixelToValue(rotateBounds.top) * 10).toInt()
                width = (mmValueUnit.convertPixelToValue(rotateBounds.width()) * 10).toInt()
                height = (mmValueUnit.convertPixelToValue(rotateBounds.height()) * 10).toInt()
                px = LaserPeckerHelper.DEFAULT_PX

                //val gCodeBound = gCodeDrawable?.gCodeBound
                //width = gCodeBound?.width()?.toInt() ?: 0
                //height = gCodeBound?.height()?.toInt() ?: 0

                //1:保存一份byte数据
                engraveReadyInfo.dataPath = saveEngraveData(index, data)//数据路径

                //2:保存一份GCode文本数据/原始数据
                saveEngraveData(index, pathGCodeText, "gcode")

                //3:保存一份GCode的图片数据/预览数据
                val bitmap = gCodeDrawable?.toBitmap()
                engraveReadyInfo.dataBitmap = bitmap
                engraveReadyInfo.previewDataPath = saveEngraveData("${index}.p", bitmap, "png")
            }
        }
    }
}