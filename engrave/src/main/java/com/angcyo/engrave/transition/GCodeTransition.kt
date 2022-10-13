package com.angcyo.engrave.transition

import android.graphics.Bitmap
import android.graphics.Path
import android.view.Gravity
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
import com.angcyo.engrave.transition.EngraveTransitionManager.Companion.toTransferDataPath
import com.angcyo.engrave.transition.IEngraveTransition.Companion.getDataMode
import com.angcyo.engrave.transition.IEngraveTransition.Companion.saveEngraveData
import com.angcyo.gcode.GCodeHelper
import com.angcyo.library.annotation.Private
import com.angcyo.library.app
import com.angcyo.library.ex.lines
import com.angcyo.library.ex.toBitmap
import com.angcyo.objectbox.laser.pecker.entity.TransferConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity
import com.angcyo.opencv.OpenCV
import java.io.File

/**
 * GCode数据转换, 什么item要处理成GCode数据
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/08/22
 */
class GCodeTransition : IEngraveTransition {

    /**将可视化数据处理成机器需要的GCode数据
     * [CanvasConstant.DATA_MODE_GCODE]*/
    override fun doTransitionTransferData(
        renderer: BaseItemRenderer<*>,
        transferConfigEntity: TransferConfigEntity,
        param: TransitionParam
    ): TransferDataEntity? {
        if (renderer is DataItemRenderer) {
            val dataItem = renderer.dataItem
            val dataBean = dataItem?.dataBean
            if (getDataMode(dataBean, transferConfigEntity) == CanvasConstant.DATA_MODE_GCODE) {
                //需要处理成GCode数据
                /*if (dataBean?.mtype == CanvasConstant.DATA_TYPE_LINE) {
                    //线条转GCode使用图片的方式
                    val bitmap = renderer.getEngraveBitmap()
                    bitmap?.let {
                        return _transitionBitmapTransferData2(
                            renderer,
                            transferConfigEntity,
                            bitmap,
                            param
                        )
                    }
                } else */if (dataItem is DataPathItem) {
                    val pathList = dataItem.dataPathList
                    return _transitionPathTransferData(
                        renderer,
                        transferConfigEntity,
                        pathList,
                        param
                    )
                } else if (dataItem is DataBitmapItem && dataItem.gCodeDrawable != null) {
                    //图片元素, 路径转GCode算法
                    val gCodeDrawable = dataItem.gCodeDrawable
                    return _transitionPathTransferData(
                        renderer,
                        transferConfigEntity,
                        listOf(gCodeDrawable!!.gCodePath),
                        param
                    )
                } else {
                    //其他元素, 使用图片转GCode算法
                    val bitmap = renderer.getEngraveBitmap()
                    bitmap?.let {
                        return _transitionBitmapTransferData(
                            renderer,
                            transferConfigEntity,
                            bitmap,
                            param
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
        pathList: List<Path>,
        param: TransitionParam
    ): TransferDataEntity {
        val isFirst = param.gCodeStartRenderer == null || param.gCodeStartRenderer == renderer
        val isFinish = param.gCodeEndRenderer == null || param.gCodeEndRenderer == renderer
        val gCodeFile = CanvasDataHandleOperate.pathToGCode(
            pathList,
            renderer.getBounds(),
            renderer.rotate,
            isFirst = isFirst,
            isFinish = isFinish
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
        bitmap: Bitmap,
        param: TransitionParam
    ): TransferDataEntity {
        val isFirst = param.gCodeStartRenderer == null || param.gCodeStartRenderer == renderer
        val isFinish = param.gCodeEndRenderer == null || param.gCodeEndRenderer == renderer
        val pxBitmap = LaserPeckerHelper.bitmapScale(bitmap, transferConfigEntity.dpi)
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

    /**Bitmap转GCode*/
    @Private
    fun _transitionBitmapTransferData2(
        renderer: BaseItemRenderer<*>,
        transferConfigEntity: TransferConfigEntity,
        bitmap: Bitmap,
        param: TransitionParam
    ): TransferDataEntity {
        val bounds = renderer.getBounds()
        val isFirst = param.gCodeStartRenderer == null || param.gCodeStartRenderer == renderer
        val isFinish = param.gCodeEndRenderer == null || param.gCodeEndRenderer == renderer
        val pxBitmap = LaserPeckerHelper.bitmapScale(bitmap, transferConfigEntity.dpi)
        val scanGravity = if (bounds.width() > bounds.height()) {
            //宽图
            Gravity.TOP
        } else {
            Gravity.LEFT
        }
        var gCodeFile = CanvasDataHandleOperate.bitmapToGCode(
            pxBitmap,
            scanGravity,
            isFirst = isFirst,
            isFinish = isFinish
        )
        val gCodeText = gCodeFile.readText()
        //GCode数据
        gCodeFile = CanvasDataHandleOperate.gCodeAdjust(gCodeText, bounds, 0f)
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
        transferDataEntity.dataPath =
            pathGCodeText.toByteArray().toTransferDataPath("${transferDataEntity.index}")

        //2:保存一份GCode文本数据/原始数据
        saveEngraveData(transferDataEntity.index, pathGCodeText, "gcode")

        val gCodeDrawable = GCodeHelper.parseGCode(pathGCodeText)

        //3:保存一份GCode的图片数据/预览数据, 数据的预览图片
        val previewBitmap = gCodeDrawable?.toBitmap()
        saveEngraveData("${transferDataEntity.index}.p", previewBitmap, "png")

        return transferDataEntity
    }
}