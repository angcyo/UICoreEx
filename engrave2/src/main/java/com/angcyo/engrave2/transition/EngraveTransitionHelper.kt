package com.angcyo.engrave2.transition

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import com.angcyo.bitmap.handle.BitmapHandle
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.bean._loopGcodeDataCmdRange
import com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd
import com.angcyo.core.component.file.writeErrorLog
import com.angcyo.core.component.file.writePerfLog
import com.angcyo.core.component.file.writeToLog
import com.angcyo.core.component.model.DataShareModel
import com.angcyo.core.vmApp
import com.angcyo.engrave2.BuildConfig
import com.angcyo.engrave2.EngraveFlowDataHelper
import com.angcyo.engrave2.R
import com.angcyo.engrave2.data.TransitionParam
import com.angcyo.engrave2.model.TransferModel
import com.angcyo.gcode.CollectPoint
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
import com.angcyo.library.component.byteWriter
import com.angcyo.library.component.hawk.LibHawkKeys
import com.angcyo.library.component.pool.acquireTempRectF
import com.angcyo.library.component.pool.release
import com.angcyo.library.ex._color
import com.angcyo.library.ex.addBgColor
import com.angcyo.library.ex.ceil
import com.angcyo.library.ex.clamp
import com.angcyo.library.ex.createOverrideBitmapCanvas
import com.angcyo.library.ex.createPaint
import com.angcyo.library.ex.deleteSafe
import com.angcyo.library.ex.file
import com.angcyo.library.ex.fileSizeString
import com.angcyo.library.ex.floor
import com.angcyo.library.ex.isDebug
import com.angcyo.library.ex.lines
import com.angcyo.library.ex.readText
import com.angcyo.library.ex.save
import com.angcyo.library.ex.size
import com.angcyo.library.ex.sleep
import com.angcyo.library.ex.toBitmap
import com.angcyo.library.ex.toDC
import com.angcyo.library.ex.toDrawable
import com.angcyo.library.ex.toSizeString
import com.angcyo.library.ex.withBitmapPaint
import com.angcyo.library.ex.withMinValue
import com.angcyo.library.libCacheFile
import com.angcyo.library.unit.IValueUnit
import com.angcyo.library.unit.toMm
import com.angcyo.objectbox.laser.pecker.entity.EngraveDataEntity
import com.angcyo.objectbox.laser.pecker.entity.TransferConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity
import com.angcyo.opencv.OpenCV
import com.angcyo.rust.handle.RustBitmapHandle
import com.angcyo.widget.span.span
import java.io.File
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 雕刻数据转换助手工具类
 *
 * [IEngraveDataProvider] -> [TransferDataEntity]
 *
 * [com.angcyo.canvas2.laser.pecker.engrave.LPTransferHelper]
 * [com.angcyo.canvas2.laser.pecker.engrave.LPTransferHelper.transitionRenderer]
 *
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

    /**根据切片数量, 获取对应的切片色值阈值列表
     * [colorsList] 图片中的颜色阈值列表
     * [com.angcyo.uicore.demo.ExampleUnitTest.testSliceList]
     * */
    fun getSliceThresholdList(colorsList: List<Int>, sliceCount: Int): List<Int> {
        //移除白色
        val colors = colorsList.toMutableList().apply { removeAll { it == 255 } }

        val thresholdList = mutableListOf<Int>()

        //最黑色颜色
        val minColor = colors.minOrNull() ?: 0
        //最白色颜色
        val maxColor = colors.maxOrNull() ?: 255

        //预估每一片的理论色阶值
        val sliceList = mutableListOf<Int>()
        if (sliceCount <= 1) {
            sliceList.add(maxColor)
        } else {
            for (i in 0 until sliceCount) {
                val value =
                    maxColor - ((maxColor - minColor) * 1f / (sliceCount - 1) * i).roundToInt()
                sliceList.add(value)
            }
        }
        sliceList.sortDescending() //从大到小排序

        for (i in 0 until sliceCount) {
            val ref = sliceList[i]
            val value = colors.minByOrNull {
                if (it <= ref) {
                    (it - ref).absoluteValue
                } else {
                    Int.MAX_VALUE
                }
            } ?: 0
            thresholdList.add(value)
        }

        return thresholdList
    }

    //region ---核心---

    /**将[provider]转换成图片雕刻数据, 一般灰度图走这个数据
     * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_BITMAP]*/
    fun transitionToBitmap(
        provider: IEngraveDataProvider?,
        transferConfigEntity: TransferConfigEntity
    ): TransferDataEntity? {
        val bitmap = provider?.getEngraveBitmapData() ?: return null
        LTime.tick()
        val transferDataEntity = createTransferDataEntity(
            provider,
            transferConfigEntity,
            DataCmd.ENGRAVE_TYPE_BITMAP,
            LaserPeckerHelper.LAYER_PICTURE
        )
        val layerDpi = transferConfigEntity.getLayerConfigDpi(
            LaserPeckerHelper.LAYER_PICTURE,
            HawkEngraveKeys.getLastLayerDpi(LaserPeckerHelper.LAYER_PICTURE)
        )
        val dpiBitmap =
            LaserPeckerHelper.bitmapScale(bitmap, LaserPeckerHelper.LAYER_PICTURE, layerDpi)

        //转换数据
        val index = transferDataEntity.index
        val dataPath = EngraveHelper.getTransferDataPath("$index")
        transferDataEntity.dataPath = dataPath
        transition.covertBitmap2BytesJni(
            dpiBitmap,
            transferDataEntity.dataPath,
            transferConfigEntity.dataDir
        )

        //灰度图, 需要严格使用图片的宽高否则雕刻出来的数据会异常
        transferDataEntity.width = dpiBitmap.width
        transferDataEntity.height = dpiBitmap.height

        span {
            append("转图片") {
                foregroundColor = accentColor
            }
            append("[$index]->")
            append(transferConfigEntity.name)
            append(" [${dpiBitmap.width} x ${dpiBitmap.height}] dpi:${layerDpi}") {
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
                    BitmapHandle.parseColorBytesToBitmap(
                        dataPath,
                        this,
                        it,
                        dpiBitmap.width,
                        dpiBitmap.height,
                        transferConfigEntity.dataDir
                    )
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
            DataCmd.ENGRAVE_TYPE_BITMAP_PATH,
            LaserPeckerHelper.LAYER_FILL
        )
        val index = transferDataEntity.index
        //testSaveBitmap(index, bitmap)

        val layerDpi = transferConfigEntity.getLayerConfigDpi(
            LaserPeckerHelper.LAYER_FILL,
            HawkEngraveKeys.getLastLayerDpi(LaserPeckerHelper.LAYER_FILL)
        )
        val dpiBitmap =
            LaserPeckerHelper.bitmapScale(bitmap, LaserPeckerHelper.LAYER_FILL, layerDpi)
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
                LibHawkKeys.alphaThreshold,
                transferConfigEntity.dataDir
            ).toInt()

        if (dataPath.file().length() <= 0 && retryCount <= HawkEngraveKeys.engraveRetryCount) {
            //数据大小为0, 重试1次
            "[$index]数据大小为0,bitmap:[${bitmap.width},${bitmap.height}],重试${retryCount}次".writeErrorLog()
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
                    BitmapHandle.parseBitmapPathToBitmap(
                        logPath,
                        this,
                        it,
                        transferConfigEntity.dataDir
                    )
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
            append(" [${dpiBitmap.width} x ${dpiBitmap.height}] dpi:${layerDpi}") {
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
            DataCmd.ENGRAVE_TYPE_BITMAP_DITHERING,
            LaserPeckerHelper.LAYER_PICTURE
        )
        val index = transferDataEntity.index

        //testSaveBitmap(index, bitmap)

        val layerDpi = transferConfigEntity.getLayerConfigDpi(
            LaserPeckerHelper.LAYER_PICTURE,
            HawkEngraveKeys.getLastLayerDpi(LaserPeckerHelper.LAYER_PICTURE)
        )
        //抖动处理图片
        val dpiBitmap =
            LaserPeckerHelper.bitmapScale(bitmap, LaserPeckerHelper.LAYER_PICTURE, layerDpi)
        var bitmapByteCount = 0
        val operateBitmap: Bitmap

        if (params.isBitmapInvert) {
            //图片已经反色, 则添加反色背景后直接进行抖动处理
            val bgColor = Color.WHITE//图片已经反色, 则透明背景变成白色, 白色不雕刻
            val dpiBitmap2 = dpiBitmap.addBgColor(bgColor)
            dpiBitmap.recycle()
            bitmapByteCount = dpiBitmap2.byteCount
            operateBitmap = if (params.useNewDithering) {
                RustBitmapHandle.bitmapDither(
                    dpiBitmap2,
                    0f,
                    0f,
                    false,
                    LibHawkKeys.alphaThreshold,
                    ditherMode = HawkEngraveKeys.ditherMode
                )
            } else {
                OpenCV.bitmapToDithering(
                    app(),
                    dpiBitmap2,
                    false,
                    0.0,
                    0.0,
                    LibHawkKeys.alphaThreshold
                )
            }!! //用灰度图进行抖动处理
            dpiBitmap2.recycle()
        } else {
            //未反色的图片
            val bgColor = if (params.invert) Color.BLACK else Color.WHITE //白色不雕刻
            val dpiBitmap2 = dpiBitmap.addBgColor(bgColor)
            dpiBitmap.recycle()
            bitmapByteCount = dpiBitmap2.byteCount
            operateBitmap = if (params.useNewDithering) {
                RustBitmapHandle.bitmapDither(
                    dpiBitmap2,
                    params.contrast,
                    params.brightness,
                    params.invert,
                    ditherMode = HawkEngraveKeys.ditherMode
                )
            } else {
                OpenCV.bitmapToDithering(
                    app(), dpiBitmap2,
                    params.invert,
                    params.contrast.toDouble(),
                    params.brightness.toDouble(),
                )
            }!! //用未反色的图进行抖动处理
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
            true,
            transferConfigEntity.dataDir
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
            append(" [${operateBitmap.width} x ${operateBitmap.height}] dpi:${layerDpi}") {
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
                val previewBitmap = logPath.toEngraveDitheringBitmapJni(
                    operateBitmap.width,
                    operateBitmap.height,
                    transferConfigEntity.dataDir
                )
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
            DataCmd.ENGRAVE_TYPE_GCODE,
            LaserPeckerHelper.LAYER_LINE
        )
        //2023-6-14 GCode偏移量
        transferDataEntity.offsetLeft = params.gcodeOffsetLeft.toMm()
        transferDataEntity.offsetTop = params.gcodeOffsetTop.toMm()

        val index = transferDataEntity.index
        val layerDpi = transferConfigEntity.getLayerConfigDpi(
            LaserPeckerHelper.LAYER_LINE,
            HawkEngraveKeys.getLastLayerDpi(LaserPeckerHelper.LAYER_LINE)
        )

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
            } else if (params.enableSlice) {
                //使用图片切片

                val sliceGcodeFile = libCacheFile("slice_output.gcode")
                sliceGcodeFile.deleteSafe()

                val colors = BitmapHandle.getChannelValueList(bitmap).map { it.toUByte().toInt() }
                val thresholdList = getSliceThresholdList(colors, params.sliceCount)

                DeviceHelper.tempEngraveLogPathList.clear()
                //切片高度(需要下降的高度)
                val sliceHeight = (params.sliceHeight * 10).roundToInt() / 10f //乘以10再除以10,保留1位小数.
                var allSliceHeight = 0f //总共下降的高度, 在最后面需要回升
                val controlHeight = buildString {
                    appendLine()
                    if (params.isAutoCnc) {
                        append("S0")
                        appendLine()
                        append("G0X0Y0")
                    } else {
                        append("M05S0")
                    }
                    appendLine()
                    append("M3021")
                    append("S$sliceHeight")
                    appendLine()
                }

                //最后一次的阈值
                var lastThreshold = -1
                var lastGCode: String? = null

                val lastIndex = thresholdList.size - 1

                if (HawkEngraveKeys.loopGcodeDataCmd || _loopGcodeDataCmdRange) {
                    //使用循环数据指令
                    thresholdList.toSet().forEachIndexed { index, threshold ->
                        val loopCount =
                            (thresholdList.count { it == threshold } - 1).withMinValue(0)

                        vmApp<TransferModel>().updateTransferMessage("获取切片[${index}/${thresholdList.size}]:$threshold")

                        BitmapHandle.toSliceHandle(bitmap, threshold, false)
                            ?.let { sliceBitmap ->
                                val cacheBitmapFile = libCacheFile("slice_${threshold}.png")
                                sliceBitmap.save(cacheBitmapFile)
                                L.i("切片[${index}/${thresholdList.size}]:$threshold ${loopCount}次->$controlHeight:${cacheBitmapFile.absolutePath}".writePerfLog())
                                DeviceHelper.tempEngraveLogPathList.add(cacheBitmapFile.absolutePath)

                                vmApp<TransferModel>().updateTransferMessage("切片[${index}/${thresholdList.size}]:$threshold ${loopCount}次")

                                params.bitmapToGCodeType = 1
                                params.bitmapToGCodeIsLast = false //levelIndex == lastIndex
                                val bounds = provider.getEngraveDataBounds(RectF())
                                val gCodeFile = if (params.useOpenCvHandleGCode) {
                                    transition.covertBitmap2GCode(
                                        sliceBitmap,
                                        null,
                                        bounds,
                                        params
                                    )
                                } else {
                                    transition.covertBitmapPixel2GCode(
                                        sliceBitmap,
                                        bounds,
                                        params
                                    )
                                }
                                gCodeFile.readText()?.let {
                                    //以下数据需要开始循环的次数
                                    sliceGcodeFile.appendText("M3040S$loopCount\n")
                                    sliceGcodeFile.appendText(it)
                                    //一层之后, 则需要下降支架高度
                                    sliceGcodeFile.appendText(controlHeight)
                                    sliceGcodeFile.appendText("M3041\n") //数据结束循环标识
                                    allSliceHeight += sliceHeight * (loopCount + 1)
                                }

                                val cacheGCodeFile =
                                    libCacheFile("slice_${threshold}.gcode")
                                if (gCodeFile.renameTo(cacheGCodeFile)) {
                                    DeviceHelper.tempEngraveLogPathList.add(cacheGCodeFile.absolutePath)
                                }
                                sliceBitmap.recycle()
                                if (params.sliceCount > HawkEngraveKeys.sliceCountDelay) {
                                    sleep(HawkEngraveKeys.sliceDelay)
                                }
                            }
                    }
                } else {
                    thresholdList.forEachIndexed { levelIndex, threshold ->
                        //单切片, 防止一下子内存占用过高
                        if (threshold != 255) { //不是白色
                            if (lastThreshold == threshold && lastGCode != null && levelIndex != lastIndex) {
                                //使用缓存, 并下降高度
                                sliceGcodeFile.appendText(controlHeight)
                                sliceGcodeFile.appendText(lastGCode!!)
                                allSliceHeight += sliceHeight
                            } else {
                                vmApp<TransferModel>().updateTransferMessage("获取切片[${levelIndex}/${thresholdList.size}]:$threshold")

                                BitmapHandle.toSliceHandle(bitmap, threshold, false)
                                    ?.let { sliceBitmap ->
                                        val cacheBitmapFile = libCacheFile("slice_${threshold}.png")
                                        sliceBitmap.save(cacheBitmapFile)
                                        L.i("切片[$levelIndex/${thresholdList.size}]:$threshold->$controlHeight:${cacheBitmapFile.absolutePath}".writePerfLog())
                                        DeviceHelper.tempEngraveLogPathList.add(cacheBitmapFile.absolutePath)

                                        vmApp<TransferModel>().updateTransferMessage("切片[$levelIndex/${thresholdList.size}]:$threshold")

                                        params.bitmapToGCodeType = 1
                                        params.bitmapToGCodeIsLast = false //levelIndex == lastIndex
                                        val bounds = provider.getEngraveDataBounds(RectF())
                                        val gCodeFile = if (params.useOpenCvHandleGCode) {
                                            transition.covertBitmap2GCode(
                                                sliceBitmap,
                                                null,
                                                bounds,
                                                params
                                            )
                                        } else {
                                            transition.covertBitmapPixel2GCode(
                                                sliceBitmap,
                                                bounds,
                                                params
                                            )
                                        }
                                        gCodeFile.readText()?.let {
                                            lastGCode = it
                                            if (levelIndex != 0) {
                                                //不是第一层, 则需要下降支架高度
                                                sliceGcodeFile.appendText(controlHeight)
                                                allSliceHeight += sliceHeight
                                            }
                                            sliceGcodeFile.appendText(it)
                                        }

                                        val cacheGCodeFile =
                                            libCacheFile("slice_${threshold}.gcode")
                                        if (gCodeFile.renameTo(cacheGCodeFile)) {
                                            DeviceHelper.tempEngraveLogPathList.add(cacheGCodeFile.absolutePath)
                                        }
                                        sliceBitmap.recycle()
                                        if (params.sliceCount > HawkEngraveKeys.sliceCountDelay) {
                                            sleep(HawkEngraveKeys.sliceDelay)
                                        }
                                    }
                            }
                        }
                        lastThreshold = threshold
                    }
                }

                if (HawkEngraveKeys.autoPickUp && allSliceHeight > 0) {
                    //回升支架高度
                    sliceGcodeFile.appendText("M3020S${allSliceHeight}\n")
                }
                sliceGcodeFile.appendText("M2\n") //GCode结束

                val fileSize = sliceGcodeFile.length()
                saveGCodeEngraveData(transferDataEntity, sliceGcodeFile)

                span {
                    append("转GCode切片") {
                        foregroundColor = accentColor
                    }
                    append("[$index]->")
                    append(transferConfigEntity.name)
                    append(" ${transferDataEntity.lines}行")
                    append(" [${bitmap.width} x ${bitmap.height}] dpi:${layerDpi}") {
                        foregroundColor = accentColor
                    }
                    append(" opencv:${params.useOpenCvHandleGCode.toDC()}")
                    append(" ${bitmap.byteCount.toSizeString()}->${fileSize.toSizeString()}")
                    append(" ${colors}->${params.sliceCount}:$sliceHeight")
                    append(" 耗时:${LTime.time()}") {
                        foregroundColor = accentColor
                    }
                }.apply { vmApp<DataShareModel>().shareTextOnceData.postValue(this) }.writePerfLog()

            } else {
                //使用图片转GCode

                /*val renderUnit = IValueUnit.MM_RENDER_UNIT
                val offsetX =
                    renderUnit.convertValueToPixel(transferDataEntity.x.toFloat()).ceilInt()
                val offsetY =
                    renderUnit.convertValueToPixel(transferDataEntity.y.toFloat()).ceilInt()*/
                val bounds = provider.getEngraveDataBounds(RectF())
                val bitmapFile = bitmap.save(libCacheFile())

                var fileSize = 0L
                var pointList: List<CollectPoint>? = null

                val gCodeFile = if (params.useOpenCvHandleGCode) {
                    transferConfigEntity.gcodeUsePathData = params.gcodeUsePathData
                    if (transferConfigEntity.gcodeUsePathData) {
                        //2023-11-30, 0x30数据支持
                        pointList = transition.covertBitmap2GCodePoint(
                            bitmap,
                            bitmapFile.absolutePath,
                            bounds,
                            params
                        )
                    }
                    transition.covertBitmap2GCode(bitmap, bitmapFile.absolutePath, bounds, params)
                } else {
                    transition.covertBitmapPixel2GCode(bitmap, bounds, params)
                }
                fileSize = gCodeFile.length()
                saveGCodeEngraveData(transferDataEntity, gCodeFile)

                if (!pointList.isNullOrEmpty()) {
                    fileSize = transitionToPoint(
                        transferDataEntity,
                        pointList,
                        false
                    )
                }

                bitmapFile.deleteSafe()

                span {
                    val type = if (transferConfigEntity.gcodeUsePathData) "PathData" else "GCode"
                    append("转${type}") {
                        foregroundColor = accentColor
                    }
                    append("[$index]->")
                    append(transferConfigEntity.name)
                    append(" ${transferDataEntity.lines}行")
                    append(" [${bitmap.width} x ${bitmap.height}] dpi:${layerDpi}") {
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

            val fileWrap = if (adjust && !params.enableGCodeCutData) {
                //使用GCode原始数据调整
                transition.adjustGCode(gcodeText!!, provider.getEngraveGCodeMatrix(), params)
            } else {
                transferConfigEntity.gcodeUsePathData = params.gcodeUsePathData
                transition.covertPathStroke2GCode(pathList, params)
            }

            val gCodeFile = fileWrap.targetFile

            var fileSize = gCodeFile.length()
            saveGCodeEngraveData(transferDataEntity, gCodeFile)

            if (transferConfigEntity.gcodeUsePathData) {
                //2023-10-23
                fileSize = transitionToPoint(transferDataEntity, fileWrap.collectPointList, true)
            }

            span {
                val type = if (transferConfigEntity.gcodeUsePathData) "PathData" else "GCode"
                append("${if (adjust) "调整" else "转"}${type}") {
                    foregroundColor = accentColor
                }
                append("[$index]->")
                append(transferConfigEntity.name)
                append(" ${transferDataEntity.lines}行 ")
                append(" dpi:${layerDpi}")
                append(" [path]->${fileSize.toSizeString()}")
                append(" 耗时:${LTime.time()}") {
                    foregroundColor = accentColor
                }
            }.apply { vmApp<DataShareModel>().shareTextOnceData.postValue(this) }.writePerfLog()
        }
        return transferDataEntity
    }

    /**转换成0x30路径数据
     * [isAbsolutePoint] 当前的点是绝对位置(画布左上角), 还是相对位置(图片左上角)
     * @return 返回字节大小*/
    private fun transitionToPoint(
        transferDataEntity: TransferDataEntity,
        collectPointList: List<CollectPoint>?,
        isAbsolutePoint: Boolean
    ): Long {
        var fileSize = 0L
        collectPointList?.apply {
            transferDataEntity.engraveDataType = DataCmd.ENGRAVE_TYPE_PATH

            val targetList = mutableListOf<CollectPoint>()
            forEach { line ->
                if (line.pointList.isNotEmpty()) {
                    //每N个数据, 生成一组新的集合
                    var pointList = mutableListOf<PointF>()
                    line.pointList.forEachIndexed { index, point ->
                        pointList.add(point)
                        if ((index + 1) % HawkEngraveKeys.pathDataPointLimit == 0) {
                            targetList.add(CollectPoint(pointList))
                            pointList = mutableListOf()
                        }
                    }
                    if (pointList.isNotEmpty()) {
                        targetList.add(CollectPoint(pointList))
                    }
                }
            }

            transferDataEntity.lines = targetList.size()

            val logBuilder =
                if (HawkEngraveKeys.engraveDataLogLevel >= L.INFO) StringBuilder() else null
            val bytes = byteWriter {
                val precision = 10 * 10 //精度
                val sizePrecision = precision / 10 //因为[transferDataEntity]里面已经是10倍的精度了
                targetList.forEach { line ->
                    write(0x7f)
                    write(0xfe)
                    val len = line.pointList.size()
                    write(len, 2)
                    logBuilder?.append("${len}:")
                    line.pointList.forEach { section ->
                        val x =
                            (section.x * precision - if (isAbsolutePoint) transferDataEntity.x * sizePrecision else 0)
                                .roundToInt().clamp(0, transferDataEntity.width * sizePrecision)
                        val y =
                            (section.y * precision - if (isAbsolutePoint) transferDataEntity.y * sizePrecision else 0)
                                .roundToInt().clamp(0, transferDataEntity.height * sizePrecision)

                        write(x, 2)
                        write(y, 2)
                        logBuilder?.append("(${x},${y})")
                    }
                    write(0x7f)
                    write(0xfa)
                    logBuilder?.appendLine()
                }
            }
            fileSize = bytes.size().toLong()
            transferDataEntity.dataPath = bytes.writeTransferDataPath("${transferDataEntity.index}")

            logBuilder?.let { log ->
                EngraveHelper.saveEngraveData(
                    transferDataEntity.index,
                    log,
                    LPDataConstant.EXT_PATH
                )
            }
        }
        return fileSize
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
            engraveDataType,
            LaserPeckerHelper.LAYER_LINE
        )

        //写入数据到路径, 用于发送到数据
        transferDataEntity.dataPath = data?.writeTransferDataPath("${transferDataEntity.index}")

        span {
            append("toRaw") {
                foregroundColor = accentColor
            }
            append("[${transferDataEntity.index}]->")
            append(transferConfigEntity.name)
            append(" dpi:${transferConfigEntity.layerJson}")
            append(" ${data.size().toSizeString()}")
            append(" 耗时:${LTime.time()}")
        }.apply { vmApp<DataShareModel>().shareTextOnceData.postValue(this) }.writePerfLog()
        return transferDataEntity
    }

    //endregion ---核心---

    //region ---辅助方法---

    /**是否需要忽略图片日志*/
    private fun ignoreBitmapLog(bitmap: Bitmap): Boolean {
        return if (isDebug()) {
            false
        } else {
            bitmap.width >= HawkEngraveKeys.engraveBitmapLogSize ||
                    bitmap.height >= HawkEngraveKeys.engraveBitmapLogSize
        }
    }

    /**创建一个传输的数据[TransferDataEntity], 并进行一些初始化
     * [engraveDataType] 雕刻数据的类型, 需要提前赋值, 后续需要此条件*/
    private fun createTransferDataEntity(
        provider: IEngraveDataProvider,
        transferConfigEntity: TransferConfigEntity,
        engraveDataType: Int,
        layerId: String
    ) = TransferDataEntity().apply {
        this.engraveDataType = engraveDataType
        initTransferDataIndex(this, provider, transferConfigEntity, layerId)
    }

    /**初始化传输数据的索引, 在构建[TransferDataEntity]之后, 尽快调用 */
    @Private
    private fun initTransferDataIndex(
        transferDataEntity: TransferDataEntity,
        provider: IEngraveDataProvider,
        configEntity: TransferConfigEntity,
        layerId: String
    ) {
        val layerDpi =
            configEntity.getLayerConfigDpi(layerId, HawkEngraveKeys.getLastLayerDpi(layerId))
        transferDataEntity.taskId = configEntity.taskId
        transferDataEntity.name = configEntity.name
        transferDataEntity.dataDir = configEntity.dataDir

        transferDataEntity.dpi = layerDpi
        transferDataEntity.index = provider.getEngraveDataIndex()

        @Pixel
        val bounds = acquireTempRectF()
        provider.getEngraveDataBounds(bounds)

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
            //mm单位, 乘以10保证精度
            transferDataEntity.x = (originX * 10).floor().toInt()
            transferDataEntity.y = (originY * 10).floor().toInt()
            transferDataEntity.width = (originWidth * 10).ceil().toInt()
            transferDataEntity.height = (originHeight * 10).ceil().toInt()
        } else {
            //px单位
            val rect = EngravePreviewCmd.adjustRectRange(bounds, layerId, layerDpi).resultRect!!
            transferDataEntity.x = rect.left
            transferDataEntity.y = rect.top

            //如果是抖动图, 这里的宽高需要使用图片的原始宽高, 否则会导致图片数据异常
            transferDataEntity.width = rect.width()
            transferDataEntity.height = rect.height()
        }

        buildString {
            append("初始化传输实体[${configEntity.taskId}]:${transferDataEntity.index}->${layerId} ")
            append("坐标[${provider.getEngraveDataName()}]:")
            append(" x:${transferDataEntity.x} y:${transferDataEntity.y}")
            append(" w:${transferDataEntity.width} h:${transferDataEntity.height}")
            /*if (engraveDataType != DataCmd.ENGRAVE_TYPE_GCODE) {
                //GCode只有1k, 所以不需要日志
            }*/
            append(" dpi:${transferDataEntity.dpi}")
            append(" :${bounds}")
        }.writeToLog()

        //last
        bounds.release()
    }

    //endregion ---辅助方法---

    //region ---文件输出信息---

    private fun saveGCodeEngraveData(transferDataEntity: TransferDataEntity, gCodeFile: File) {
        val fileLength = gCodeFile.length()
        transferDataEntity.lines = gCodeFile.lines()
        val gCodeText = gCodeFile.readText()
        gCodeFile.deleteSafe()

        val index = transferDataEntity.index
        transferDataEntity.dataPath = gCodeText?.toByteArray()?.writeTransferDataPath("$index")

        doBack {
            //2:保存一份GCode文本数据/原始数据
            if (HawkEngraveKeys.engraveDataLogLevel >= L.INFO) {
                EngraveHelper.saveEngraveData(
                    index,
                    gCodeText,
                    LPDataConstant.EXT_GCODE
                )
            }

            if (HawkEngraveKeys.engraveDataLogLevel >= L.WARN && fileLength <= HawkEngraveKeys.previewFileByteCount) {
                //val gCodeDrawable = GCodeHelper.parseGCode(gCodeText)
                val gCodeDrawable =
                    gCodeText?.toGCodePath()?.toDrawable(HawkEngraveKeys.projectOutSize.toFloat())

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
            val engraveStartTimeList = mutableListOf<Long>()
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
                engraveStartTimeList.add(engraveDataEntity?.startTime ?: -1)
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
                        //雕刻完成
                        when (engraveFinishReasonList[index]) {
                            EngraveDataEntity.FINISH_REASON_INDEX -> Color.YELLOW
                            EngraveDataEntity.FINISH_REASON_SKIP -> Color.GREEN
                            else -> DeviceHelper.ENGRAVE_COLOR
                        }
                    } else if (engraveStartTimeList[index] > 0) {
                        //开始过雕刻, 蓝色
                        DeviceHelper.PREVIEW_COLOR
                    } else if (isTransferList[index]) {
                        //传输完成, 黑色
                        Color.BLACK
                    } else {
                        //只是创建了数据, 白色/灰色
                        Color.GRAY
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