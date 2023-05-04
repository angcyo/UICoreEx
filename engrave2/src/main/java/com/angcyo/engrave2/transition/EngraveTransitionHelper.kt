package com.angcyo.engrave2.transition

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import com.angcyo.bitmap.handle.BitmapHandle
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd
import com.angcyo.core.component.file.writePerfLog
import com.angcyo.core.component.file.writeToLog
import com.angcyo.core.component.model.DataShareModel
import com.angcyo.core.vmApp
import com.angcyo.engrave2.BuildConfig
import com.angcyo.engrave2.EngraveFlowDataHelper
import com.angcyo.engrave2.R
import com.angcyo.engrave2.data.TransitionParam
import com.angcyo.http.rx.doBack
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.device.DeviceHelper
import com.angcyo.laserpacker.device.EngraveHelper
import com.angcyo.laserpacker.device.EngraveHelper.writeTransferDataPath
import com.angcyo.laserpacker.toGCodePath
import com.angcyo.library.L
import com.angcyo.library.LTime
import com.angcyo.library.annotation.MM
import com.angcyo.library.annotation.Pixel
import com.angcyo.library.annotation.Private
import com.angcyo.library.app
import com.angcyo.library.component.hawk.LibHawkKeys
import com.angcyo.library.ex._color
import com.angcyo.library.ex.addBgColor
import com.angcyo.library.ex.ceil
import com.angcyo.library.ex.createOverrideBitmapCanvas
import com.angcyo.library.ex.createPaint
import com.angcyo.library.ex.deleteSafe
import com.angcyo.library.ex.file
import com.angcyo.library.ex.fileSizeString
import com.angcyo.library.ex.floor
import com.angcyo.library.ex.lines
import com.angcyo.library.ex.size
import com.angcyo.library.ex.toBitmap
import com.angcyo.library.ex.toDC
import com.angcyo.library.ex.toDrawable
import com.angcyo.library.ex.toSizeString
import com.angcyo.library.ex.withBitmapPaint
import com.angcyo.library.unit.IValueUnit
import com.angcyo.objectbox.laser.pecker.entity.EngraveDataEntity
import com.angcyo.objectbox.laser.pecker.entity.TransferConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity
import com.angcyo.opencv.OpenCV
import com.angcyo.widget.span.span
import java.io.File
import kotlin.math.max
import kotlin.math.min

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

    /**日志输出的强调颜色*/
    var accentColor = _color(R.color.error)

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

        //转换数据
        val index = transferDataEntity.index
        val dataPath = EngraveHelper.getTransferDataPath("$index")
        transferDataEntity.dataPath = dataPath
        transition.covertBitmap2BytesJni(dpiBitmap, transferDataEntity.dataPath)

        //灰度图, 需要严格使用图片的宽高否则雕刻出来的数据会异常
        transferDataEntity.width = dpiBitmap.width
        transferDataEntity.height = dpiBitmap.height

        span {
            append("转图片") {
                foregroundColor = accentColor
            }
            append("[$index]->")
            append(transferConfigEntity.name)
            append(" [${dpiBitmap.width} x ${dpiBitmap.height}] dpi:${transferConfigEntity.dpi}") {
                foregroundColor = accentColor
            }
            append(" ${dpiBitmap.byteCount.toSizeString()}->${transferDataEntity.dataPath.fileSizeString()}")
            append(" 耗时:${LTime.time()}") {
                foregroundColor = accentColor
            }
        }.apply { vmApp<DataShareModel>().shareTextOnceData.postValue(this) }.writePerfLog()

        doBack {
            //1:保存一份原始可视化数据
            if (HawkEngraveKeys.engraveDataLogLevel >= L.DEBUG) {
                EngraveHelper.saveEngraveData(index, dpiBitmap, LPDataConstant.EXT_PREVIEW)
            }

            //2: 数据没有log
            if (HawkEngraveKeys.engraveDataLogLevel >= L.WARN && !ignoreBitmapLog(dpiBitmap)) {
                //3:数据的预览图片
                val previewBitmap = withBitmapPaint(dpiBitmap) {
                    BitmapHandle.parseColorBytesToBitmap(dataPath, this, it, dpiBitmap.width)
                }
                EngraveHelper.saveEngraveData(
                    index,
                    previewBitmap,
                    LPDataConstant.EXT_DATA_PREVIEW,
                    true
                )
            }
            dpiBitmap.recycle()//回收内存
        }
        return transferDataEntity
    }

    /**将[provider]转换成图片路径数据, 一般黑白图走这个数据
     * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_BITMAP_PATH]*/
    fun transitionToBitmapPath(
        provider: IEngraveDataProvider?,
        transferConfigEntity: TransferConfigEntity,
        retryCount: Int = 0, /*失败后重试的次数*/
    ): TransferDataEntity? {
        val bitmap = provider?.getEngraveBitmapData() ?: return null
        LTime.tick()

        val transferDataEntity = createTransferDataEntity(
            provider,
            transferConfigEntity,
            DataCmd.ENGRAVE_TYPE_BITMAP_PATH
        )
        val index = transferDataEntity.index
        //testSaveBitmap(index, bitmap)

        val dpiBitmap = LaserPeckerHelper.bitmapScale(bitmap, transferConfigEntity.dpi)
        val dataPath = EngraveHelper.getTransferDataPath("$index")
        transferDataEntity.dataPath = dataPath

        val logPath = if (HawkEngraveKeys.engraveDataLogLevel >= L.INFO)
            EngraveHelper.getSaveEngraveDataFilePath(
                index,
                LPDataConstant.EXT_BP
            ) else null //路径图的日志输出路径
        transferDataEntity.lines =
            transition.covertBitmap2BPJni(
                dpiBitmap,
                dataPath,
                logPath,
                LibHawkKeys.grayThreshold,
                LibHawkKeys.alphaThreshold
            ).toInt()

        if (dataPath.file().length() <= 0 && retryCount <= HawkEngraveKeys.engraveRetryCount) {
            //数据大小为0, 重试1次
            return transitionToBitmapPath(provider, transferConfigEntity, retryCount + 1)
        }

        doBack {
            //1:保存一份原始可视化数据
            if (HawkEngraveKeys.engraveDataLogLevel >= L.DEBUG) {
                EngraveHelper.saveEngraveData(
                    index,
                    dpiBitmap,
                    LPDataConstant.EXT_PREVIEW
                )
            }

            //3:保存一份数据的预览图
            if (HawkEngraveKeys.engraveDataLogLevel >= L.WARN && !ignoreBitmapLog(dpiBitmap)) {
                val previewBitmap = withBitmapPaint(dpiBitmap) {
                    BitmapHandle.parseBitmapPathToBitmap(logPath, this, it)
                }
                EngraveHelper.saveEngraveData(
                    index,
                    previewBitmap,
                    LPDataConstant.EXT_DATA_PREVIEW,
                    true
                )
            }

            dpiBitmap.recycle()
        }

        span {
            append("转路径") {
                foregroundColor = accentColor
            }
            append("[$index]->")
            append(transferConfigEntity.name)
            append(" ${transferDataEntity.lines}行 ")
            append(" [${dpiBitmap.width} x ${dpiBitmap.height}] dpi:${transferConfigEntity.dpi}") {
                foregroundColor = accentColor
            }
            append(" ${dpiBitmap.byteCount.toSizeString()}->${dataPath.fileSizeString()}")
            append(" 耗时:${LTime.time()}") {
                foregroundColor = accentColor
            }
        }.apply { vmApp<DataShareModel>().shareTextOnceData.postValue(this) }.writePerfLog()
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
        val index = transferDataEntity.index

        //testSaveBitmap(index, bitmap)

        //抖动处理图片
        val dpiBitmap = LaserPeckerHelper.bitmapScale(bitmap, transferConfigEntity.dpi)
        var bitmapByteCount = 0
        val operateBitmap: Bitmap

        if (params.isBitmapInvert) {
            //图片已经反色, 则添加反色背景后直接进行抖动处理
            val bgColor = Color.WHITE//图片已经反色, 则透明背景变成白色, 白色不雕刻
            val dpiBitmap2 = dpiBitmap.addBgColor(bgColor)
            dpiBitmap.recycle()
            bitmapByteCount = dpiBitmap2.byteCount
            operateBitmap = OpenCV.bitmapToDithering(
                app(), dpiBitmap2,
                false,
                0.0,
                0.0,
            )!! //用灰度图进行抖动处理
            dpiBitmap2.recycle()
        } else {
            //未反色的图片
            val bgColor = if (params.invert) Color.BLACK else Color.WHITE //白色不雕刻
            val dpiBitmap2 = dpiBitmap.addBgColor(bgColor)
            dpiBitmap.recycle()
            bitmapByteCount = dpiBitmap2.byteCount
            operateBitmap = OpenCV.bitmapToDithering(
                app(), dpiBitmap2,
                params.invert,
                params.contrast.toDouble(),
                params.brightness.toDouble(),
            )!! //用未反色的图进行抖动处理
            dpiBitmap2.recycle()
        }

        //开始转换成机器数据
        val dataPath = EngraveHelper.getTransferDataPath("$index")
        transferDataEntity.dataPath = dataPath
        val logPath = if (HawkEngraveKeys.engraveDataLogLevel >= L.INFO)
            EngraveHelper.getSaveEngraveDataFilePath(
                index,
                LPDataConstant.EXT_DT
            ) else null //抖动图的日志输出路径
        transition.covertBitmap2DitheringJni(
            operateBitmap,
            dataPath,//数据输出
            logPath,//日志输出
            LibHawkKeys.grayThreshold,
            LibHawkKeys.alphaThreshold,
            true
        )

        //抖动图, 需要严格使用图片的宽高否则雕刻出来的数据会异常
        transferDataEntity.width = operateBitmap.width
        transferDataEntity.height = operateBitmap.height

        span {
            append("转抖动") {
                foregroundColor = accentColor
            }
            append("[$index]->")
            append(transferConfigEntity.name)
            append(" [${operateBitmap.width} x ${operateBitmap.height}] dpi:${transferConfigEntity.dpi}") {
                foregroundColor = accentColor
            }
            append(" ${bitmapByteCount.toSizeString()}->${dataPath.fileSizeString()}")
            append(" 耗时:${LTime.time()}") {
                foregroundColor = accentColor
            }
        }.apply { vmApp<DataShareModel>().shareTextOnceData.postValue(this) }.writePerfLog()

        doBack {
            //1:保存一份原始可视化数据
            if (HawkEngraveKeys.engraveDataLogLevel >= L.DEBUG) {
                EngraveHelper.saveEngraveData(
                    index,
                    operateBitmap,
                    LPDataConstant.EXT_PREVIEW
                )
            }

            //3:保存一份数据的预览图
            if (HawkEngraveKeys.engraveDataLogLevel >= L.WARN && !ignoreBitmapLog(operateBitmap)) {
                val previewBitmap =
                    logPath.toEngraveDitheringBitmapJni(operateBitmap.width, operateBitmap.height)
                EngraveHelper.saveEngraveData(
                    index,
                    previewBitmap,
                    LPDataConstant.EXT_DATA_PREVIEW,
                    true
                )
            }
            operateBitmap.recycle()
        }

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
        val index = transferDataEntity.index

        val pathList = provider.getEngravePathData()
        val bitmap = provider.getEngraveBitmapData()

        //1:保存一份原始可视化数据
        if (HawkEngraveKeys.engraveDataLogLevel >= L.DEBUG) {
            EngraveHelper.saveEngraveData(
                index,
                bitmap,
                LPDataConstant.EXT_PREVIEW
            )
        }

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

                span {
                    append("转GCode") {
                        foregroundColor = accentColor
                    }
                    append("[$index]->")
                    append(transferConfigEntity.name)
                    append(" ${transferDataEntity.lines}行 ")
                    append(" [${bitmap.width} x ${bitmap.height}] dpi:${transferConfigEntity.dpi}") {
                        foregroundColor = accentColor
                    }
                    append(" opencv:${params.useOpenCvHandleGCode.toDC()}")
                    append(" ${bitmap.byteCount.toSizeString()}->${fileSize.toSizeString()}")
                    append(" 耗时:${LTime.time()}") {
                        foregroundColor = accentColor
                    }
                }.apply { vmApp<DataShareModel>().shareTextOnceData.postValue(this) }.writePerfLog()
            }
        } else {
            //path转GCode
            val gcodeText = provider.getEngraveGCode()
            val adjust = !HawkEngraveKeys.enableGCodeTransform && !gcodeText.isNullOrBlank()

            val gCodeFile = if (adjust) {
                //使用GCode原始数据调整
                transition.adjustGCode(gcodeText!!, provider.getEngraveGCodeMatrix(), params)
            } else {
                transition.covertPathStroke2GCode(pathList, params)
            }

            val fileSize = gCodeFile.length()
            saveGCodeEngraveData(transferDataEntity, gCodeFile)

            span {
                append("${if (adjust) "调整" else "转"}GCode") {
                    foregroundColor = accentColor
                }
                append("[$index]->")
                append(transferConfigEntity.name)
                append(" ${transferDataEntity.lines}行 ")
                append(" dpi:${transferConfigEntity.dpi}")
                append(" [path]->${fileSize.toSizeString()}")
                append(" 耗时:${LTime.time()}") {
                    foregroundColor = accentColor
                }
            }.apply { vmApp<DataShareModel>().shareTextOnceData.postValue(this) }.writePerfLog()
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

        span {
            append("toRaw") {
                foregroundColor = accentColor
            }
            append("[${transferDataEntity.index}]->")
            append(transferConfigEntity.name)
            append(" dpi:${transferConfigEntity.dpi}")
            append(" ${data.size().toSizeString()}")
            append(" 耗时:${LTime.time()}")
        }.apply { vmApp<DataShareModel>().shareTextOnceData.postValue(this) }.writePerfLog()
        return transferDataEntity
    }

    //endregion ---核心---

    //region ---辅助方法---

    /**是否需要忽略图片日志*/
    private fun ignoreBitmapLog(bitmap: Bitmap) =
        bitmap.width >= HawkEngraveKeys.engraveBitmapLogSize ||
                bitmap.height >= HawkEngraveKeys.engraveBitmapLogSize

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
        val originX = mmValueUnit.convertPixelToValue(dataLeft)
        val originY = mmValueUnit.convertPixelToValue(dataTop)
        val originWidth = mmValueUnit.convertPixelToValue(dataWidth)
        val originHeight = mmValueUnit.convertPixelToValue(dataHeight)
        transferDataEntity.originX = originX
        transferDataEntity.originY = originY
        transferDataEntity.originWidth = originWidth
        transferDataEntity.originHeight = originHeight

        //雕刻数据坐标
        val engraveDataType = transferDataEntity.engraveDataType
        if (engraveDataType == DataCmd.ENGRAVE_TYPE_GCODE) {
            //mm单位
            transferDataEntity.x = (originX * 10).floor().toInt()
            transferDataEntity.y = (originY * 10).floor().toInt()
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

            //如果是抖动图, 这里的宽高需要使用图片的原始宽高, 否则会导致图片数据异常
            transferDataEntity.width = rect.width()
            transferDataEntity.height = rect.height()
        }

        buildString {
            append("初始化传输实体[${configEntity.taskId}]:${transferDataEntity.index}->")
            append("坐标[${provider.getEngraveDataName()}]:")
            append(" x:${transferDataEntity.x} y:${transferDataEntity.y}")
            append(" w:${transferDataEntity.width} h:${transferDataEntity.height}")
            if (engraveDataType != DataCmd.ENGRAVE_TYPE_GCODE) {
                //GCode只有1k, 所以不需要日志
                append(" ${transferDataEntity.dpi}")
            }
            append(" :${bounds}")
        }.writeToLog()
    }

    //endregion ---辅助方法---

    //region ---文件输出信息---

    private fun saveGCodeEngraveData(transferDataEntity: TransferDataEntity, gCodeFile: File) {
        transferDataEntity.lines = gCodeFile.lines()
        val gCodeText = gCodeFile.readText()
        gCodeFile.deleteSafe()

        val index = transferDataEntity.index
        transferDataEntity.dataPath = gCodeText.toByteArray().writeTransferDataPath("$index")

        doBack {
            //2:保存一份GCode文本数据/原始数据
            if (HawkEngraveKeys.engraveDataLogLevel >= L.INFO) {
                EngraveHelper.saveEngraveData(
                    index,
                    gCodeText,
                    LPDataConstant.EXT_GCODE
                )
            }

            if (HawkEngraveKeys.engraveDataLogLevel >= L.WARN) {
                //val gCodeDrawable = GCodeHelper.parseGCode(gCodeText)
                val gCodeDrawable =
                    gCodeText.toGCodePath()?.toDrawable(HawkEngraveKeys.projectOutSize.toFloat())

                //3:保存一份GCode的图片数据/预览数据, 数据的预览图片
                val previewBitmap = gCodeDrawable?.toBitmap()
                EngraveHelper.saveEngraveData(
                    index,
                    previewBitmap,
                    LPDataConstant.EXT_DATA_PREVIEW,
                    true
                )
            }
        }
    }

    private fun testSaveBitmap(index: Int, bitmap: Bitmap) {
        if (BuildConfig.DEBUG) {
            EngraveHelper.saveEngraveData(
                "${index}-test",
                bitmap,
                LPDataConstant.EXT_PREVIEW
            )
        }
    }

    /**[saveTaskAerialView]*/
    fun saveTaskAerialView(taskId: String?) {
        val transferDataList = EngraveFlowDataHelper.getTransferDataList(taskId)
        saveTaskAerialView(taskId, transferDataList)
    }

    /**保存一份任务雕刻鸟瞰图*/
    fun saveTaskAerialView(taskId: String?, transferDataList: List<TransferDataEntity>) {
        if (transferDataList.size() < 1) {
            //数据个数大于1, 生成坐标的鸟瞰图
            return
        }
        doBack {
            var left: Int? = null
            var top: Int? = null
            var right: Int? = null
            var bottom: Int? = null
            val rectList = mutableListOf<Rect>()
            val isTransferList = mutableListOf<Boolean>()
            val isEngraveList = mutableListOf<Boolean>()
            val engraveFinishReasonList = mutableListOf<Int>()
            for (transferData in transferDataList) {
                val l = transferData.x
                val t = transferData.y
                val r = l + transferData.width
                val b = t + transferData.height

                left = min(left ?: l, l)
                top = min(top ?: t, t)
                right = max(right ?: r, r)
                bottom = max(bottom ?: b, b)

                rectList.add(Rect(l, t, r, b))

                //是否已经传输完成
                val transferDataEntity =
                    EngraveFlowDataHelper.getTransferData(taskId, transferData.index)
                isTransferList.add(transferDataEntity?.isTransfer == true)

                //是否已经雕刻完成
                val engraveDataEntity =
                    EngraveFlowDataHelper.getEngraveDataEntity(taskId, transferData.index)
                isEngraveList.add(engraveDataEntity?.progress == 100)
                engraveFinishReasonList.add(engraveDataEntity?.finishReason ?: -1)
            }
            //鸟瞰图
            val paint = createPaint()
            val paintWidth = paint.strokeWidth
            createOverrideBitmapCanvas(
                right!! - left!! + paintWidth,
                bottom!! - top!! + paintWidth,
                HawkEngraveKeys.projectOutSize.toFloat(),
                null, {
                    preTranslate(-left.toFloat(), -top.toFloat())
                    postTranslate(paintWidth / 2, paintWidth / 2)
                }
            ) {
                rectList.forEachIndexed { index, rect ->
                    paint.color = if (isEngraveList[index]) {
                        when (engraveFinishReasonList[index]) {
                            EngraveDataEntity.FINISH_REASON_INDEX -> Color.YELLOW
                            EngraveDataEntity.FINISH_REASON_SKIP -> Color.GREEN
                            else -> DeviceHelper.ENGRAVE_COLOR
                        }
                    } else if (isTransferList[index]) {
                        DeviceHelper.PREVIEW_COLOR
                    } else {
                        Color.BLACK
                    }
                    drawRect(rect, paint)
                }
            }?.let { bitmap ->
                //保存鸟瞰图
                EngraveHelper.saveEngraveData(
                    taskId,
                    bitmap,
                    LPDataConstant.EXT_DATA_PREVIEW,
                    true
                )
            }
        }
    }

    //endregion ---文件输出信息---

}