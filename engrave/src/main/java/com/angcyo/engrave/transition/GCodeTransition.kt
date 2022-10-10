package com.angcyo.engrave.transition

import android.graphics.Bitmap
import android.graphics.Path
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd
import com.angcyo.canvas.data.ItemDataBean.Companion.DEFAULT_LINE_SPACE
import com.angcyo.canvas.data.toMm
import com.angcyo.canvas.items.data.DataBitmapItem
import com.angcyo.canvas.items.data.DataItemRenderer
import com.angcyo.canvas.items.data.DataPathItem
import com.angcyo.canvas.items.renderer.BaseItemRenderer
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.canvas.utils.CanvasDataHandleOperate
import com.angcyo.canvas.utils.getEngraveBitmap
import com.angcyo.canvas.utils.parseGCode
import com.angcyo.gcode.GCodeHelper
import com.angcyo.library.annotation.Private
import com.angcyo.library.app
import com.angcyo.library.ex.lines
import com.angcyo.library.ex.toBitmap
import com.angcyo.objectbox.laser.pecker.entity.TransferConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity
import com.angcyo.objectbox.laser.pecker.entity.toTransferData
import com.angcyo.opencv.OpenCV
import java.io.File

/**
 * GCode数据转换, 什么item要处理成GCode数据
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/08/22
 */
class GCodeTransition : IEngraveTransition {
/*

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
                //item.transformPath(renderer, item.shapePath, path)

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
*/

    /**将可视化数据处理成机器需要的GCode数据
     * [CanvasConstant.DATA_MODE_GCODE]*/
    override fun doTransitionTransferData(
        renderer: BaseItemRenderer<*>,
        transferConfigEntity: TransferConfigEntity
    ): TransferDataEntity? {
        if (renderer is DataItemRenderer) {
            val dataItem = renderer.dataItem
            val dataBean = dataItem?.dataBean
            if (getDataMode(dataBean, transferConfigEntity) == CanvasConstant.DATA_MODE_GCODE) {
                //需要处理成GCode数据
                if (dataItem is DataPathItem) {
                    val pathList = dataItem.dataPathList
                    return _transitionPathTransferData(renderer, transferConfigEntity, pathList)
                } else if (dataItem is DataBitmapItem && dataItem.gCodeDrawable != null) {
                    //图片元素, 路径转GCode算法
                    val gCodeDrawable = dataItem.gCodeDrawable
                    return _transitionPathTransferData(
                        renderer,
                        transferConfigEntity,
                        listOf(gCodeDrawable!!.gCodePath)
                    )
                } else {
                    //其他元素, 使用图片转GCode算法
                    val bitmap = renderer.getEngraveBitmap()
                    bitmap?.let {
                        return _transitionBitmapTransferData(
                            renderer,
                            transferConfigEntity,
                            bitmap
                        )
                    }
                }
            }
        }
        return null
    }

    /**Path路径转GCode*/
    @Private
    fun _transitionPathTransferData(
        renderer: BaseItemRenderer<*>,
        transferConfigEntity: TransferConfigEntity,
        pathList: List<Path>
    ): TransferDataEntity {
        val gCodeFile = CanvasDataHandleOperate.pathToGCode(
            pathList,
            renderer.getBounds(),
            renderer.rotate
        )
        return _handleGCodeTransferDataEntity(renderer, transferConfigEntity, gCodeFile).apply {
            //1: 存一份原始可视化数据
            val bitmap = renderer.getEngraveBitmap()
            saveEngraveData("$index", bitmap, "png")
        }
    }

    /**Bitmap转GCode*/
    @Private
    fun _transitionBitmapTransferData(
        renderer: BaseItemRenderer<*>,
        transferConfigEntity: TransferConfigEntity,
        bitmap: Bitmap
    ): TransferDataEntity {
        val pxBitmap = LaserPeckerHelper.bitmapScale(bitmap, transferConfigEntity.px)
        var gCodeFile = OpenCV.bitmapToGCode(
            app(),
            pxBitmap,
            (pxBitmap.width / 2).toMm().toDouble(),
            lineSpace = DEFAULT_LINE_SPACE.toDouble(),
            direction = 0,
            angle = 0.0
        )
        val gCodeText = gCodeFile.readText()
        //GCode数据
        gCodeFile = CanvasDataHandleOperate.gCodeAdjust(
            gCodeText,
            renderer.getBounds(),
            renderer.rotate
        )
        return _handleGCodeTransferDataEntity(renderer, transferConfigEntity, gCodeFile).apply {
            //1: 存一份原始可视化数据
            saveEngraveData("$index", pxBitmap, "png")
        }
    }

    /**GCode数据处理*/
    @Private
    fun _handleGCodeTransferDataEntity(
        renderer: BaseItemRenderer<*>,
        transferConfigEntity: TransferConfigEntity,
        gCodeFile: File
    ): TransferDataEntity {
        val transferDataEntity =
            TransferDataEntity(index = EngraveTransitionManager.generateEngraveIndex())
        transferDataEntity.engraveDataType = DataCmd.ENGRAVE_TYPE_GCODE
        initTransferDataEntity(renderer, transferConfigEntity, transferDataEntity)
        transferDataEntity.lines = gCodeFile.lines()

        val pathGCodeText = gCodeFile.readText()
        transferDataEntity.data = pathGCodeText.toByteArray().toTransferData()

        //2:保存一份GCode文本数据/原始数据
        saveEngraveData(transferDataEntity.index, pathGCodeText, "gcode")

        val gCodeDrawable = GCodeHelper.parseGCode(pathGCodeText)

        //3:保存一份GCode的图片数据/预览数据, 数据的预览图片
        val previewBitmap = gCodeDrawable?.toBitmap()
        saveEngraveData("${transferDataEntity.index}.p", previewBitmap, "png")

        return transferDataEntity
    }
}