package com.angcyo.engrave.transition

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path
import android.view.Gravity
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd
import com.angcyo.canvas.graphics.IEngraveProvider
import com.angcyo.canvas.items.data.DataBitmapItem
import com.angcyo.canvas.items.data.DataPathItem
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.canvas.utils.CanvasDataHandleOperate
import com.angcyo.canvas.utils.isLineShape
import com.angcyo.core.vmApp
import com.angcyo.engrave.transition.IEngraveTransition.Companion.getDataMode
import com.angcyo.engrave.transition.IEngraveTransition.Companion.saveEngraveData
import com.angcyo.gcode.GCodeHelper
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.device.EngraveHelper.writeTransferDataPath
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.laserpacker.parseGCode
import com.angcyo.laserpacker.toPaintStyleInt
import com.angcyo.library.annotation.Private
import com.angcyo.library.app
import com.angcyo.library.ex.deleteSafe
import com.angcyo.library.ex.lines
import com.angcyo.library.ex.toBitmap
import com.angcyo.library.unit.toMm
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
        if (getDataMode(dataBean, transferConfigEntity) == LPDataConstant.DATA_MODE_GCODE) {

            //需要处理成GCode数据
            if (dataBean?.mtype == LPDataConstant.DATA_TYPE_GCODE &&
                !dataBean.data.isNullOrEmpty() &&
                !HawkEngraveKeys.enableGCodeTransform /*不激活gcode数据全转换*/
            ) {
                //如果是原始的GCode数据
                return _transitionGCodeTransferData(
                    engraveProvider,
                    transferConfigEntity,
                    dataBean.data!!,
                    param
                )
            } else if (dataBean?.mtype == LPDataConstant.DATA_TYPE_LINE &&
                dataBean.paintStyle == Paint.Style.STROKE.toPaintStyleInt()
            ) {
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
                )//这种方式生成的GCode,会丢失原先G2,G3指令
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
                    //此图片已经缩放并且旋转了
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
        val autoCnc = vmApp<LaserPeckerModel>().isC1()
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
            saveEngraveData(index, bitmap, LPDataConstant.EXT_PREVIEW, false)
        }
    }

    /**GCode原始数据旋转/缩放. 注意原始数据是需要偏移的*/
    @Private
    fun _transitionGCodeTransferData(
        engraveProvider: IEngraveProvider,
        transferConfigEntity: TransferConfigEntity,
        gcode: String,
        param: TransitionParam?
    ): TransferDataEntity {
        val renderer = engraveProvider.getEngraveRenderer()
        val isFirst = param?.gCodeStartRenderer == null || param.gCodeStartRenderer == renderer
        val isFinish = param?.gCodeEndRenderer == null || param.gCodeEndRenderer == renderer
        val autoCnc = vmApp<LaserPeckerModel>().isC1()

        val gCodeFile = CanvasDataHandleOperate.gCodeAdjust(
            gcode,
            engraveProvider.getEngraveBounds(),
            engraveProvider._rotate,
            autoCnc,
            isFinish
        )

        return _handleGCodeTransferDataEntity(
            engraveProvider,
            transferConfigEntity,
            gCodeFile
        ).apply {
            //1: 存一份原始可视化数据
            val bitmap = engraveProvider.getEngraveBitmap()
            saveEngraveData(index, bitmap, LPDataConstant.EXT_PREVIEW, false)
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
        val bounds = engraveProvider.getEngraveBounds()
        val rotateBounds = engraveProvider.getEngraveRotateBounds()
        val renderer = engraveProvider.getEngraveRenderer()
        val isFirst = param?.gCodeStartRenderer == null || param.gCodeStartRenderer == renderer
        val isFinish = param?.gCodeEndRenderer == null || param.gCodeEndRenderer == renderer
        val autoCnc = vmApp<LaserPeckerModel>().isC1()

        val pxBitmap = bitmap//LaserPeckerHelper.bitmapScale(bitmap, transferConfigEntity.dpi)
        var gCodeFile = OpenCV.bitmapToGCode(
            app(),
            pxBitmap,
            (rotateBounds.width() / 2).toMm().toDouble(),
            lineSpace = LPDataConstant.DEFAULT_LINE_SPACE.toDouble(),
            direction = 0,
            angle = 0.0,
            type = 2 //只获取轮廓
        )
        val gCodeText = gCodeFile.readText()
        //GCode数据
        gCodeFile =
            CanvasDataHandleOperate.gCodeAdjust(gCodeText, rotateBounds, 0f, autoCnc, isFinish)
        return _handleGCodeTransferDataEntity(
            engraveProvider,
            transferConfigEntity,
            gCodeFile
        ).apply {
            //1: 存一份原始可视化数据
            saveEngraveData(index, pxBitmap, LPDataConstant.EXT_PREVIEW)
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
        val pxBitmap = bitmap//LaserPeckerHelper.bitmapScale(bitmap, transferConfigEntity.dpi)
        val scanGravity = if (rotateBounds.width() > rotateBounds.height()) {
            //宽图
            Gravity.TOP
        } else {
            Gravity.LEFT
        }
        val autoCnc = vmApp<LaserPeckerModel>().isC1()
        var gCodeFile = CanvasDataHandleOperate.bitmapToGCode(
            pxBitmap,
            scanGravity,
            isFirst = isFirst,
            isFinish = isFinish,
            autoCnc = autoCnc,
            isSingleLine = engraveProvider.getEngraveDataItem()?.isLineShape() == true
        )
        val gCodeText = gCodeFile.readText()
        gCodeFile.deleteSafe()
        //GCode数据, 这里必须使用旋转后的bounds进行调整
        gCodeFile =
            CanvasDataHandleOperate.gCodeAdjust(gCodeText, rotateBounds, 0f, autoCnc, isFinish)
        return _handleGCodeTransferDataEntity(
            engraveProvider,
            transferConfigEntity,
            gCodeFile
        ).apply {
            //1: 存一份原始可视化数据
            saveEngraveData(index, pxBitmap, LPDataConstant.EXT_PREVIEW)
        }
    }

    /**GCode数据处理*/
    @Private
    fun _handleGCodeTransferDataEntity(
        engraveProvider: IEngraveProvider,
        transferConfigEntity: TransferConfigEntity,
        gCodeFile: File
    ): TransferDataEntity {
        val transferDataEntity = createTransferDataEntity(engraveProvider, transferConfigEntity)

        transferDataEntity.engraveDataType = DataCmd.ENGRAVE_TYPE_GCODE
        initTransferDataEntity(engraveProvider, transferConfigEntity, transferDataEntity)
        transferDataEntity.lines = gCodeFile.lines()

        val pathGCodeText = gCodeFile.readText()
        transferDataEntity.dataPath =
            pathGCodeText.toByteArray().writeTransferDataPath("${transferDataEntity.index}")

        //2:保存一份GCode文本数据/原始数据
        saveEngraveData(transferDataEntity.index, pathGCodeText, LPDataConstant.EXT_GCODE)

        val gCodeDrawable = GCodeHelper.parseGCode(pathGCodeText)

        //3:保存一份GCode的图片数据/预览数据, 数据的预览图片
        val previewBitmap = gCodeDrawable?.toBitmap()
        saveEngraveData(
            transferDataEntity.index,
            previewBitmap,
            LPDataConstant.EXT_DATA_PREVIEW,
            true
        )

        return transferDataEntity
    }
}