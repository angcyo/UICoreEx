package com.angcyo.engrave.transition

import android.graphics.Bitmap
import android.graphics.Path
import android.view.Gravity
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd
import com.angcyo.canvas.data.CanvasProjectItemBean.Companion.DEFAULT_LINE_SPACE
import com.angcyo.canvas.data.toMm
import com.angcyo.canvas.graphics.IEngraveProvider
import com.angcyo.canvas.items.data.DataBitmapItem
import com.angcyo.canvas.items.data.DataPathItem
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.canvas.utils.CanvasDataHandleOperate
import com.angcyo.canvas.utils.parseGCode
import com.angcyo.core.vmApp
import com.angcyo.engrave.transition.EngraveTransitionManager.Companion.writeTransferDataPath
import com.angcyo.engrave.transition.IEngraveTransition.Companion.getDataMode
import com.angcyo.engrave.transition.IEngraveTransition.Companion.saveEngraveData
import com.angcyo.gcode.GCodeHelper
import com.angcyo.library.annotation.Private
import com.angcyo.library.app
import com.angcyo.library.ex.deleteSafe
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
        engraveProvider: IEngraveProvider,
        transferConfigEntity: TransferConfigEntity,
        param: TransitionParam?
    ): TransferDataEntity? {
        val dataItem = engraveProvider.getEngraveDataItem()
        val dataBean = dataItem?.dataBean
        if (getDataMode(dataBean, transferConfigEntity) == CanvasConstant.DATA_MODE_GCODE) {
            //需要处理成GCode数据
            if (dataBean?.mtype == CanvasConstant.DATA_TYPE_LINE && dataBean.paintStyle == 1) {
                //线条转GCode使用图片的方式
                val bitmap = engraveProvider.getEngraveBitmap()
                bitmap?.let {
                    return _transitionBitmapTransferData2(
                        engraveProvider,
                        transferConfigEntity,
                        bitmap,
                        param
                    )
                }
            } else if (dataItem is DataPathItem) {
                val pathList = dataItem.dataPathList
                return _transitionPathTransferData(
                    engraveProvider,
                    transferConfigEntity,
                    pathList,
                    param
                )
            } else if (dataItem is DataBitmapItem && dataItem.gCodeDrawable != null) {
                //图片元素, 路径转GCode算法
                val gCodeDrawable = dataItem.gCodeDrawable
                return _transitionPathTransferData(
                    engraveProvider,
                    transferConfigEntity,
                    listOf(gCodeDrawable!!.gCodePath),
                    param
                )
            } else {
                //其他元素, 使用图片转GCode算法
                val bitmap = engraveProvider.getEngraveBitmap()
                bitmap?.let {
                    return _transitionBitmapTransferData(
                        engraveProvider,
                        transferConfigEntity,
                        bitmap,
                        param
                    )
                }
            }
        }
        return null
    }

    /**Path路径转GCode*/
    @Private
    fun _transitionPathTransferData(
        engraveProvider: IEngraveProvider,
        transferConfigEntity: TransferConfigEntity,
        pathList: List<Path>,
        param: TransitionParam?
    ): TransferDataEntity {
        val renderer = engraveProvider.getEngraveRenderer()
        val isFirst = param?.gCodeStartRenderer == null || param.gCodeStartRenderer == renderer
        val isFinish = param?.gCodeEndRenderer == null || param.gCodeEndRenderer == renderer
        val autoCnc = vmApp<LaserPeckerModel>().productInfoData.value?.isCI() == true
        val gCodeFile = CanvasDataHandleOperate.pathStrokeToGCode(
            pathList,
            engraveProvider.getEngraveBounds(),
            engraveProvider._rotate,
            writeFirst = isFirst,
            writeLast = isFinish,
            autoCnc = autoCnc
        )
        return _handleGCodeTransferDataEntity(
            engraveProvider,
            transferConfigEntity,
            gCodeFile
        ).apply {
            //1: 存一份原始可视化数据
            val bitmap = engraveProvider.getEngraveBitmap()
            saveEngraveData("$index", bitmap, "png")
        }
    }

    /**Bitmap转GCode*/
    @Private
    fun _transitionBitmapTransferData(
        engraveProvider: IEngraveProvider,
        transferConfigEntity: TransferConfigEntity,
        bitmap: Bitmap,
        param: TransitionParam?
    ): TransferDataEntity {
        val renderer = engraveProvider.getEngraveRenderer()
        val isFirst = param?.gCodeStartRenderer == null || param.gCodeStartRenderer == renderer
        val isFinish = param?.gCodeEndRenderer == null || param.gCodeEndRenderer == renderer
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
            engraveProvider.getEngraveBounds(),
            engraveProvider._rotate
        )
        return _handleGCodeTransferDataEntity(
            engraveProvider,
            transferConfigEntity,
            gCodeFile
        ).apply {
            //1: 存一份原始可视化数据
            saveEngraveData("$index", pxBitmap, "png")
        }
    }

    /**Bitmap转GCode*/
    @Private
    fun _transitionBitmapTransferData2(
        engraveProvider: IEngraveProvider,
        transferConfigEntity: TransferConfigEntity,
        bitmap: Bitmap,
        param: TransitionParam?
    ): TransferDataEntity {
        val bounds = engraveProvider.getEngraveBounds()
        val rotateBounds = engraveProvider.getEngraveRotateBounds()
        val renderer = engraveProvider.getEngraveRenderer()
        val isFirst = param?.gCodeStartRenderer == null || param.gCodeStartRenderer == renderer
        val isFinish = param?.gCodeEndRenderer == null || param.gCodeEndRenderer == renderer
        val pxBitmap = LaserPeckerHelper.bitmapScale(bitmap, transferConfigEntity.dpi)
        val scanGravity = if (bounds.width() > bounds.height()) {
            //宽图
            Gravity.TOP
        } else {
            Gravity.LEFT
        }
        val autoCnc = vmApp<LaserPeckerModel>().productInfoData.value?.isCI() == true
        var gCodeFile = CanvasDataHandleOperate.bitmapToGCode(
            pxBitmap,
            scanGravity,
            isFirst = isFirst,
            isFinish = isFinish,
            autoCnc = autoCnc
        )
        val gCodeText = gCodeFile.readText()
        gCodeFile.deleteSafe()
        //GCode数据, 此时的GCode只需要平移
        gCodeFile = CanvasDataHandleOperate.gCodeTranslation(gCodeText, rotateBounds)
        return _handleGCodeTransferDataEntity(
            engraveProvider,
            transferConfigEntity,
            gCodeFile
        ).apply {
            //1: 存一份原始可视化数据
            saveEngraveData("$index", pxBitmap, "png")
        }
    }

    /**GCode数据处理*/
    @Private
    fun _handleGCodeTransferDataEntity(
        engraveProvider: IEngraveProvider,
        transferConfigEntity: TransferConfigEntity,
        gCodeFile: File
    ): TransferDataEntity {
        val transferDataEntity = TransferDataEntity()
        transferDataEntity.index = EngraveTransitionManager.generateEngraveIndex()

        transferDataEntity.engraveDataType = DataCmd.ENGRAVE_TYPE_GCODE
        initTransferDataEntity(engraveProvider, transferConfigEntity, transferDataEntity)
        transferDataEntity.lines = gCodeFile.lines()

        val pathGCodeText = gCodeFile.readText()
        transferDataEntity.dataPath =
            pathGCodeText.toByteArray().writeTransferDataPath("${transferDataEntity.index}")

        //2:保存一份GCode文本数据/原始数据
        saveEngraveData(transferDataEntity.index, pathGCodeText, "gcode")

        val gCodeDrawable = GCodeHelper.parseGCode(pathGCodeText)

        //3:保存一份GCode的图片数据/预览数据, 数据的预览图片
        val previewBitmap = gCodeDrawable?.toBitmap()
        saveEngraveData("${transferDataEntity.index}.p", previewBitmap, "png")

        return transferDataEntity
    }
}