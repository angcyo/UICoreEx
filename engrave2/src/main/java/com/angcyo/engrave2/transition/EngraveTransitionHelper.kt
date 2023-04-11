package com.angcyo.engrave2.transition

import android.graphics.Color
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd
import com.angcyo.core.component.file.writePerfLog
import com.angcyo.core.vmApp
import com.angcyo.engrave2.data.TransitionParam
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.device.EngraveHelper
import com.angcyo.laserpacker.device.EngraveHelper.writeTransferDataPath
import com.angcyo.library.LTime
import com.angcyo.library.annotation.MM
import com.angcyo.library.annotation.Pixel
import com.angcyo.library.annotation.Private
import com.angcyo.library.app
import com.angcyo.library.component.byteWriter
import com.angcyo.library.ex.*
import com.angcyo.library.unit.IValueUnit
import com.angcyo.objectbox.laser.pecker.entity.TransferConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity
import com.angcyo.opencv.OpenCV
import java.io.File
import kotlin.io.readText
import kotlin.math.max

/**
 * 雕刻数据转换助手工具类
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/29
 */
object EngraveTransitionHelper {

    /**数据最小的宽高*/
    @Pixel
    const val DATA_MIN_WIDTH = 1f

    @Pixel
    const val DATA_MIN_HEIGHT = 1f

    /**数据转换*/
    var transition: ITransition = SimpleTransition()

    //region ---核心---

    /**将[provider]转换成图片雕刻数据, 一般灰度图走这个数据
     * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_BITMAP]*/
    fun transitionToBitmap(
        provider: IEngraveDataProvider?,
        transferConfigEntity: TransferConfigEntity
    ): TransferDataEntity? {
        val bitmap = provider?.getEngraveBitmapData() ?: return null
        LTime.tick()
        val transferDataEntity =
            createTransferDataEntity(provider, transferConfigEntity, DataCmd.ENGRAVE_TYPE_BITMAP)
        val dpiBitmap = LaserPeckerHelper.bitmapScale(bitmap, transferConfigEntity.dpi)

        //1:保存一份原始可视化数据
        EngraveHelper.saveEngraveData(
            transferDataEntity.index,
            dpiBitmap,
            LPDataConstant.EXT_PREVIEW
        )

        val data = transition.covertBitmap2Bytes(dpiBitmap)
        //2: 数据没有log

        //要传输的数据路径
        transferDataEntity.dataPath =
            data.writeTransferDataPath("${transferDataEntity.index}")

        //3:数据的预览图片
        val previewBitmap = data.toEngraveBitmap(dpiBitmap.width, dpiBitmap.height)
        EngraveHelper.saveEngraveData(
            transferDataEntity.index,
            previewBitmap,
            LPDataConstant.EXT_DATA_PREVIEW,
            true
        )

        buildString {
            append("toBitmap[${transferDataEntity.index}]->")
            append(transferConfigEntity.name)
            append(" dpi:${transferConfigEntity.dpi}")
            append(" ${dpiBitmap.byteCount.toSizeString()}->${data.size().toSizeString()}")
            append(" 转换耗时:${LTime.time()}")
        }.writePerfLog()
        return transferDataEntity
    }

    /**将[provider]转换成图片路径数据, 一般黑白图走这个数据
     * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_BITMAP_PATH]*/
    fun transitionToBitmapPath(
        provider: IEngraveDataProvider?,
        transferConfigEntity: TransferConfigEntity
    ): TransferDataEntity? {
        val bitmap = provider?.getEngraveBitmapData() ?: return null
        LTime.tick()

        val transferDataEntity = createTransferDataEntity(
            provider,
            transferConfigEntity,
            DataCmd.ENGRAVE_TYPE_BITMAP_PATH
        )
        val dpiBitmap = LaserPeckerHelper.bitmapScale(bitmap, transferConfigEntity.dpi)

        //1:保存一份原始可视化数据
        EngraveHelper.saveEngraveData(
            transferDataEntity.index,
            dpiBitmap,
            LPDataConstant.EXT_PREVIEW
        )

        //val renderUnit = IValueUnit.MM_RENDER_UNIT
        //renderUnit.convertValueToPixel(transferDataEntity.x.toFloat()).ceilInt()
        //renderUnit.convertValueToPixel(transferDataEntity.y.toFloat()).ceilInt()
        val offsetLeft = 0
        val offsetTop = 0

        val bitmapPathList = transition.covertBitmap2BP(dpiBitmap)

        //要传输的数据路径
        val data = byteWriter {
            bitmapPathList.forEach {
                write(it.x, 2)
                write(it.y, 2)
                write(it.len, 2)
            }
        }
        transferDataEntity.dataPath =
            data.writeTransferDataPath("${transferDataEntity.index}")
        transferDataEntity.lines = bitmapPathList.size

        //2:路径数据写入日志
        EngraveHelper.saveEngraveData(
            transferDataEntity.index,
            bitmapPathList.toEngraveLog(),
            LPDataConstant.EXT_BP
        )
        //3:保存一份数据的预览图
        val previewBitmap =
            bitmapPathList.toEngraveBitmap(dpiBitmap.width, dpiBitmap.height, offsetLeft, offsetTop)
        EngraveHelper.saveEngraveData(
            transferDataEntity.index,
            previewBitmap,
            LPDataConstant.EXT_DATA_PREVIEW,
            true
        )

        buildString {
            append("toBitmapPath[${transferDataEntity.index}]->")
            append(transferConfigEntity.name)
            append(" dpi:${transferConfigEntity.dpi}")
            append(" ${dpiBitmap.byteCount.toSizeString()}->${data.size().toSizeString()}")
            append(" 转换耗时:${LTime.time()}")
        }.writePerfLog()
        return transferDataEntity
    }

    /**将[provider]转换成抖动数据, 一般抖动图走这个数据, 会进行8位压缩处理
     * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_BITMAP_DITHERING]*/
    fun transitionToBitmapDithering(
        provider: IEngraveDataProvider?,
        transferConfigEntity: TransferConfigEntity,
        params: TransitionParam
    ): TransferDataEntity? {
        val bitmap = provider?.getEngraveBitmapData() ?: return null
        LTime.tick()

        val transferDataEntity = createTransferDataEntity(
            provider,
            transferConfigEntity,
            DataCmd.ENGRAVE_TYPE_BITMAP_DITHERING
        )
        //抖动处理图片
        val dpiBitmap = LaserPeckerHelper.bitmapScale(bitmap, transferConfigEntity.dpi)
        val bgColor = Color.WHITE //if (params.invert) Color.BLACK else Color.WHITE
        val dpiBitmap2 = dpiBitmap.addBgColor(bgColor)
        dpiBitmap.recycle()

        val bitmapByteCount = dpiBitmap2.byteCount
        val operateBitmap = OpenCV.bitmapToDithering(
            app(), dpiBitmap2,
            params.invert,
            params.contrast.toDouble(),
            params.brightness.toDouble(),
        )!! //用灰度图进行抖动处理
        dpiBitmap2.recycle()

        //1:保存一份原始可视化数据
        EngraveHelper.saveEngraveData(
            transferDataEntity.index,
            operateBitmap,
            LPDataConstant.EXT_PREVIEW
        )

        //白色1 黑色0; 白色传1, 1不出光. 黑色传0, 0出光, 数据压缩
        val pair = transition.covertBitmap2Dithering(operateBitmap, true)
        transferDataEntity.dataPath =
            pair.second.writeTransferDataPath("${transferDataEntity.index}")

        //路径数据写入日志
        EngraveHelper.saveEngraveData(
            transferDataEntity.index,
            pair.first,
            LPDataConstant.EXT_DT
        )
        //3:保存一份数据的预览图
        val previewBitmap =
            pair.first.toEngraveDitheringBitmap(operateBitmap.width, operateBitmap.height)
        EngraveHelper.saveEngraveData(
            transferDataEntity.index,
            previewBitmap,
            LPDataConstant.EXT_DATA_PREVIEW,
            true
        )

        buildString {
            append("toBitmapDithering[${transferDataEntity.index}]->")
            append(transferConfigEntity.name)
            append(" dpi:${transferConfigEntity.dpi}")
            append(" ${bitmapByteCount.toSizeString()}->${pair.second.size().toSizeString()}")
            append(" 转换耗时:${LTime.time()}")
        }.writePerfLog()
        return transferDataEntity
    }

    /**将[provider]转换成GCode雕刻数据, 发送GCode数据给机器
     * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_GCODE]*/
    fun transitionToGCode(
        provider: IEngraveDataProvider?,
        transferConfigEntity: TransferConfigEntity,
        params: TransitionParam
    ): TransferDataEntity? {
        provider ?: return null
        LTime.tick()

        val transferDataEntity = createTransferDataEntity(
            provider,
            transferConfigEntity,
            DataCmd.ENGRAVE_TYPE_GCODE
        )

        val pathList = provider.getEngravePathData()
        val bitmap = provider.getEngraveBitmapData()

        //1:保存一份原始可视化数据
        EngraveHelper.saveEngraveData(
            transferDataEntity.index,
            bitmap,
            LPDataConstant.EXT_PREVIEW
        )

        if (params.onlyUseBitmapToGCode || pathList == null) {
            //如果没有路径, 获取强制使用图片转GCode
            if (bitmap == null) {
                //也没有图片
                return null
            } else {
                //使用图片转GCode

                /*val renderUnit = IValueUnit.MM_RENDER_UNIT
                val offsetX =
                    renderUnit.convertValueToPixel(transferDataEntity.x.toFloat()).ceilInt()
                val offsetY =
                    renderUnit.convertValueToPixel(transferDataEntity.y.toFloat()).ceilInt()*/
                val bounds = provider.getEngraveDataBounds()
                val gCodeFile = if (params.useOpenCvHandleGCode) {
                    transition.covertBitmap2GCode(bitmap, bounds)
                } else {
                    transition.covertBitmapPixel2GCode(bitmap, bounds, params)
                }
                val fileSize = gCodeFile.length()
                saveGCodeEngraveData(transferDataEntity, gCodeFile)

                buildString {
                    append("toGCode[${transferDataEntity.index}]->")
                    append(transferConfigEntity.name)
                    append(" dpi:${transferConfigEntity.dpi}")
                    append(" opencv:${params.useOpenCvHandleGCode.toDC()}")
                    append(" ${bitmap.byteCount.toSizeString()}->${fileSize.toSizeString()}")
                    append(" 转换耗时:${LTime.time()}")
                }.writePerfLog()
            }
        } else {
            //path转GCode
            val gCodeFile = transition.covertPathStroke2GCode(pathList, params)
            val fileSize = gCodeFile.length()
            saveGCodeEngraveData(transferDataEntity, gCodeFile)

            buildString {
                append("toGCode[${transferDataEntity.index}]->")
                append(transferConfigEntity.name)
                append(" dpi:${transferConfigEntity.dpi}")
                append(" [path]->${fileSize.toSizeString()}")
                append(" 转换耗时:${LTime.time()}")
            }.writePerfLog()
        }
        return transferDataEntity
    }

    /**原封不动转发数据*/
    fun transitionToRaw(
        provider: IEngraveDataProvider?,
        transferConfigEntity: TransferConfigEntity,
        engraveDataType: Int
    ): TransferDataEntity? {
        provider ?: return null
        LTime.tick()

        val data = provider.getEngraveRawData()
        val transferDataEntity = createTransferDataEntity(
            provider,
            transferConfigEntity,
            engraveDataType
        )

        //写入数据到路径, 用于发送到数据
        transferDataEntity.dataPath = data?.writeTransferDataPath("${transferDataEntity.index}")

        buildString {
            append("toRaw[${transferDataEntity.index}]->")
            append(transferConfigEntity.name)
            append(" dpi:${transferConfigEntity.dpi}")
            append(" ${data.size().toSizeString()}")
            append(" 转换耗时:${LTime.time()}")
        }.writePerfLog()
        return transferDataEntity
    }

    //endregion ---核心---

    //region ---辅助方法---

    /**创建一个传输的数据[TransferDataEntity], 并进行一些初始化
     * [engraveDataType] 雕刻数据的类型, 需要提前赋值, 后续需要此条件*/
    private fun createTransferDataEntity(
        provider: IEngraveDataProvider,
        transferConfigEntity: TransferConfigEntity,
        engraveDataType: Int
    ) = TransferDataEntity().apply {
        this.engraveDataType = engraveDataType
        initTransferDataIndex(this, provider, transferConfigEntity)
    }

    /**初始化传输数据的索引, 在构建[TransferDataEntity]之后, 尽快调用 */
    @Private
    private fun initTransferDataIndex(
        transferDataEntity: TransferDataEntity,
        provider: IEngraveDataProvider,
        configEntity: TransferConfigEntity,
    ) {
        transferDataEntity.taskId = configEntity.taskId
        transferDataEntity.dpi = configEntity.dpi
        transferDataEntity.name = configEntity.name
        transferDataEntity.index = provider.getEngraveDataIndex()

        @Pixel
        val bounds = provider.getEngraveDataBounds()

        val dataLeft = bounds.left
        val dataTop = bounds.top

        @Pixel
        val dataWidth = max(DATA_MIN_WIDTH, bounds.width())
        val dataHeight = max(DATA_MIN_HEIGHT, bounds.height())

        val mmValueUnit = IValueUnit.MM_RENDER_UNIT

        //产品名
        vmApp<LaserPeckerModel>().productInfoData.value?.apply {
            transferDataEntity.productName = name
            transferDataEntity.deviceAddress = deviceAddress
        }

        @MM
        val originWidth = mmValueUnit.convertPixelToValue(dataWidth)
        val originHeight = mmValueUnit.convertPixelToValue(dataHeight)
        transferDataEntity.originX = mmValueUnit.convertPixelToValue(dataLeft)
        transferDataEntity.originY = mmValueUnit.convertPixelToValue(dataTop)
        transferDataEntity.originWidth = originWidth
        transferDataEntity.originHeight = originHeight

        //雕刻数据坐标
        val engraveDataType = transferDataEntity.engraveDataType
        if (engraveDataType == DataCmd.ENGRAVE_TYPE_GCODE) {
            //mm单位
            transferDataEntity.x =
                (mmValueUnit.convertPixelToValue(dataLeft) * 10).floor().toInt()
            transferDataEntity.y =
                (mmValueUnit.convertPixelToValue(dataTop) * 10).floor().toInt()
            transferDataEntity.width = (originWidth * 10).ceil().toInt()
            transferDataEntity.height = (originHeight * 10).ceil().toInt()
        } else {
            //px单位
            val rect = EngravePreviewCmd.adjustRectRange(
                bounds,
                configEntity.dpi
            ).resultRect!!
            transferDataEntity.x = rect.left
            transferDataEntity.y = rect.top
            transferDataEntity.width = rect.width()
            transferDataEntity.height = rect.height()
        }
    }

    //endregion ---辅助方法---

    //region ---文件输出信息---

    private fun saveGCodeEngraveData(transferDataEntity: TransferDataEntity, gCodeFile: File) {
        transferDataEntity.lines = gCodeFile.lines()
        val gCodeText = gCodeFile.readText()
        gCodeFile.deleteSafe()

        transferDataEntity.dataPath =
            gCodeText.toByteArray().writeTransferDataPath("${transferDataEntity.index}")

        //2:保存一份GCode文本数据/原始数据
        EngraveHelper.saveEngraveData(
            transferDataEntity.index,
            gCodeText,
            LPDataConstant.EXT_GCODE
        )

        //val gCodeDrawable = GCodeHelper.parseGCode(gCodeText)
        val gCodeDrawable = gCodeText.toGCodePath()?.toDrawable()

        //3:保存一份GCode的图片数据/预览数据, 数据的预览图片
        val previewBitmap = gCodeDrawable?.toBitmap()
        EngraveHelper.saveEngraveData(
            transferDataEntity.index,
            previewBitmap,
            LPDataConstant.EXT_DATA_PREVIEW,
            true
        )
    }

    //endregion ---文件输出信息---

}