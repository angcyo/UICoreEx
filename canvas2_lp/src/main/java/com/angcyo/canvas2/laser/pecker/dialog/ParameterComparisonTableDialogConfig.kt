package com.angcyo.canvas2.laser.pecker.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import androidx.annotation.Keep
import androidx.annotation.WorkerThread
import com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker._deviceConfigBean
import com.angcyo.bluetooth.fsc.laserpacker._showLaserFrequencyConfig
import com.angcyo.bluetooth.fsc.laserpacker.bean._isAutoCnc
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.element.limitElementMaxSizeMatrix
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.renderer.CanvasGroupRenderer
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.AppointPowerDepthItem
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.GridCountItem
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.LabelSizeItem
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.LayerSegmentItem
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.PCTImageItem
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.PCTLabelItem
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.PrintCountItem
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.RowsColumnsRangeItem
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.TablePreviewItem
import com.angcyo.canvas2.laser.pecker.element.LPTextElement
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.EngraveLaserFrequencyItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.EngraveLaserSegmentItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.EngraveOptionWheelItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.EngravePropertyItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.transfer.TransferDataPxItem
import com.angcyo.canvas2.laser.pecker.util.LPRendererHelper
import com.angcyo.core.vmApp
import com.angcyo.dialog.BaseRecyclerDialogConfig
import com.angcyo.dialog.configBottomDialog
import com.angcyo.dialog2.dslitem.getSelectedWheelIntData
import com.angcyo.dialog2.dslitem.itemSelectedIndex
import com.angcyo.dialog2.dslitem.itemWheelBean
import com.angcyo.dialog2.dslitem.itemWheelList
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.eachItem
import com.angcyo.gcode.GCodeWriteHandler
import com.angcyo.item.style.itemCurrentIndex
import com.angcyo.item.style.itemLabelText
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.bean.LPElementBean
import com.angcyo.laserpacker.device.DeviceHelper
import com.angcyo.laserpacker.device.EngraveHelper
import com.angcyo.laserpacker.device.LayerHelper
import com.angcyo.laserpacker.device.ensurePrintPrecision
import com.angcyo.laserpacker.device.filterLayerDpi
import com.angcyo.laserpacker.device.toDataMode
import com.angcyo.laserpacker.device.toLaserWave
import com.angcyo.laserpacker.device.toLayerInfo
import com.angcyo.laserpacker.toPaintStyleInt
import com.angcyo.library.annotation.DSL
import com.angcyo.library.annotation.MM
import com.angcyo.library.annotation.Pixel
import com.angcyo.library.canvas.core.Reason
import com.angcyo.library.component.Strategy
import com.angcyo.library.component.hawk.HawkPropertyValue
import com.angcyo.library.ex._string
import com.angcyo.library.ex.appendSpaceIfNotEmpty
import com.angcyo.library.ex.dpi
import com.angcyo.library.ex.have
import com.angcyo.library.ex.size
import com.angcyo.library.ex.toBase64Data
import com.angcyo.library.ex.uuid
import com.angcyo.library.ex.withMinValue
import com.angcyo.library.unit.IValueUnit
import com.angcyo.library.unit.toMm
import com.angcyo.library.unit.toPixel
import com.angcyo.library.utils.BuildHelper
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity
import java.io.StringWriter
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * 添加一个功率深度参数对照表
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/04/04
 */
class ParameterComparisonTableDialogConfig : BaseRecyclerDialogConfig(),
    IParameterComparisonTableProvider {

    @Keep
    companion object {

        @Pixel
        internal val tableBounds: RectF
            get() = vmApp<LaserPeckerModel>().productInfoData.value?.previewBounds ?: RectF(
                0f, 0f, 160f.toPixel(), 160f.toPixel()
            )

        /**功率深度阈值, 小于值才需要绘制格子*/
        internal var powerDepthThreshold: Float by HawkPropertyValue<Any, Float>(100 * 100f)

        /**最小阈值*/
        internal var minPowerDepthThreshold: Float by HawkPropertyValue<Any, Float>(0f)

        /**网格格子的大小*/
        @MM
        internal var gridItemSize: Float by HawkPropertyValue<Any, Float>(14f)

        /**竖向网格的数量, 有多少行*/
        internal var verticalGridCount: Int by HawkPropertyValue<Any, Int>(5)

        /**横向网格的数量, 有多少列*/
        internal var horizontalGridCount: Int by HawkPropertyValue<Any, Int>(5)

        internal var minPower: Int by HawkPropertyValue<Any, Int>(10)
        internal var maxPower: Int by HawkPropertyValue<Any, Int>(100)

        internal var minDepth: Int by HawkPropertyValue<Any, Int>(10)
        internal var maxDepth: Int by HawkPropertyValue<Any, Int>(100)

        /**网格格子的边距*/
        @MM
        internal var gridItemMargin: Float by HawkPropertyValue<Any, Float>(2f)

        /**数字文本大小*/
        @MM
        internal var pctTextFontSize: Float by HawkPropertyValue<Any, Float>(8f)

        /**标签文本字体缩放比例,相对于[pctTextFontSize]*/
        internal var pctLabelTextFontScale: Float by HawkPropertyValue<Any, Float>(1.5f)

        /**标题文本字体缩放比例,相对于[pctTextFontSize]*/
        internal var pctTitleTextFontScale: Float by HawkPropertyValue<Any, Float>(1.2f)

        /**字间距*/
        @MM
        internal var ptcCharSpace: Float by HawkPropertyValue<Any, Float>(0.5f)

        /**强行指定格子的数据类型, 图层id*/
        internal var gridLayerId: String by HawkPropertyValue<Any, String>(LaserPeckerHelper.LAYER_FILL)

        /**加速级别*/
        internal var gridPrintPrecision: Int by HawkPropertyValue<Any, Int>(1)

        /**额外追加的行列范围
         * [行:列 行:列 行:列] */
        internal var rowsColumnsRange: String by HawkPropertyValue<Any, String>("")

        /**雕刻次数配置
         * [行.列.次数 行.列.次数] */
        internal var pctPrintCount: String by HawkPropertyValue<Any, String>("")

        /**直接指定要生成的功率和深度
         * [功率 功率 功率 功率.深度 深度 深度] */
        internal var appointPowerDepth: String by HawkPropertyValue<Any, String>("")

        /**参数表的Label*/
        internal var labelText: String? by HawkPropertyValue<Any, String?>("%1")

        /**需要隐藏的元素*/
        const val HIDE_POWER = 0b1 //1: 隐藏顶部功率
        const val HIDE_DEPTH = HIDE_POWER shl 1 //2: 隐藏左边深度
        const val HIDE_GRID = HIDE_DEPTH shl 1 //4: 隐藏网格
        const val HIDE_LABEL = HIDE_GRID shl 1 //8: 隐藏左上角标签
        const val HIDE_POWER_LABEL = HIDE_LABEL shl 1 //16: 隐藏功率数字
        const val HIDE_DEPTH_LABEL = HIDE_POWER_LABEL shl 1 //32: 隐藏深度数字

        var hideFunInt: Int by HawkPropertyValue<Any, Int>(0)

        /**按键大小*/
        internal var keyboardNumSize = 45 * dpi

        /**选中的图片*/
        internal var selectImage: Bitmap? = null

        /**添加乘法口诀表*/
        fun addMultiplicationTable(delegate: CanvasRenderDelegate?) {
            delegate ?: return
            HawkEngraveKeys.enableTransferIndexCheck = false //可选, 关闭索引检查, 每次都重新传输 = true //必须
            HawkEngraveKeys.enableSingleItemTransfer = true //必须
            val bounds = tableBounds

            @Pixel val padding = 5f.toPixel()
            val horizontalGap = 3f.toPixel()
            val verticalGap = 2f.toPixel()

            val count = 9
            val textWidthSum = bounds.width() - padding * 2 - (count - 1) * horizontalGap
            val textHeightSum = bounds.height() - padding * 2 - (count - 1) * verticalGap

            /* @Pixel
             val textWidth = textWidthSum / count
             val textHeight = textHeightSum / count*/

            val textItem = LPTextElement(LPElementBean().apply {
                mtype = LPDataConstant.DATA_TYPE_TEXT
                text = "9 × 9=81"
                fontSize = 4f
                charSpacing = 0.2f

                /*width = textWidth.toMm()
                height = textHeight.toMm()*/
            })

            @Pixel val textWidth = textItem.getTextWidth()
            val textHeight = textItem.getTextHeight()

            val textLeft = bounds.left + padding
            val textTop = bounds.centerY() - (textHeight * count + verticalGap * (count - 1)) / 2

            //LPElementBean
            fun createItemBean(): LPElementBean = LPElementBean().apply {
                mtype = LPDataConstant.DATA_TYPE_TEXT
                fontSize = textItem.elementBean.fontSize
                charSpacing = textItem.elementBean.charSpacing
                width = textItem.elementBean.width
                height = textItem.elementBean.height
            }

            val beanList = mutableListOf<LPElementBean>()
            for (x in 1..count) { //1~9 列
                val left = textLeft + (x - 1) * (textWidth + horizontalGap)
                for (y in x..count) { //x~9 行
                    val top = textTop + (y - 1) * (textHeight + verticalGap)
                    beanList.add(createItemBean().apply {
                        text = "$x × $y=${x * y}"
                        this.left = left.toMm()
                        this.top = top.toMm()
                    })
                }
            }

            //组合在一起
            val groupId = uuid()
            for (bean in beanList) {
                bean.groupId = groupId
            }

            LPRendererHelper.renderElementList(delegate, beanList, true, Strategy.normal)
        }

        /**添加视力表*/
        fun addVisualChart(delegate: CanvasRenderDelegate?) {
            delegate ?: return
            HawkEngraveKeys.enableTransferIndexCheck = false //可选, 关闭索引检查, 每次都重新传输
            HawkEngraveKeys.enableSingleItemTransfer = true //必须
            val bounds = tableBounds

            @Pixel val padding = 5f.toPixel()
            val horizontalGap = 3f.toPixel()
            val verticalGap = 4f.toPixel()

            var textLeft = bounds.left + padding
            var textTop = bounds.top + padding

            //每一行, 有多少个E, 从上到下
            val lineSizeList = listOf(1, 2, 2, 3, 3, 4, 4, 5, 6, 7, 8, 8, 8, 8)

            @MM var maxFontSize = 20f //最上面一行的字体大小
            val scaleFactor = 0.8f //每一行字体大小是上一行的多少倍
            val lineCount = lineSizeList.size() //总共的行数
            val minFontSize = maxFontSize * scaleFactor.pow(lineCount - 1)

            //随机旋转的角度
            val angleList = mutableListOf<Float>()
            fun randomAngle(): Float {
                if (angleList.size() >= 4) {
                    val last = angleList.last()
                    angleList.clear()
                    angleList.add(last)
                }
                while (true) {
                    val angle = when (Random.nextInt(0, 4)) { //[0~4)
                        1 -> 90f
                        2 -> 180f
                        3 -> 270f
                        else -> 0f
                    }
                    if (!angleList.contains(angle)) {
                        angleList.add(angle)
                        break
                    }
                }
                return angleList.last()
            }

            val textItem = LPTextElement(LPElementBean().apply {
                mtype = LPDataConstant.DATA_TYPE_TEXT
                text = "E"
                fontSize = minFontSize

                /*width = textWidth.toMm()
                height = textHeight.toMm()*/
            })

            //计算最后一行需要占用的宽度
            val lastLineSize = lineSizeList.last()

            @Pixel val textAllWidth =
                lastLineSize * textItem.getTextWidth() + (lastLineSize - 1) * horizontalGap
            textLeft = bounds.centerX() - textAllWidth / 2

            //LPElementBean
            fun createItemBean(): LPElementBean = LPElementBean().apply {
                mtype = LPDataConstant.DATA_TYPE_TEXT
                fontSize = textItem.elementBean.fontSize
                text = textItem.elementBean.text
                angle = randomAngle()
                width = textItem.elementBean.width
                height = textItem.elementBean.height
            }

            val beanList = mutableListOf<LPElementBean>()
            for (line in 0 until lineCount) {
                val lineSize = lineSizeList[line]
                val lineTop = textTop
                var lineHeight = 0f
                for (size in 0 until lineSize) {
                    beanList.add(createItemBean().apply {
                        fontSize = maxFontSize

                        val eWidth = textAllWidth / lineSize
                        val centerX = textLeft + eWidth * size + eWidth / 2
                        val item = LPTextElement(this)
                        val itemWidth = item.getTextWidth()
                        lineHeight = item.getTextHeight()

                        left = (centerX - itemWidth / 2).toMm()
                        top = lineTop.toMm()

                        height = lineHeight.toMm()
                        width = itemWidth.toMm()
                    })
                }
                textTop = lineTop + lineHeight + verticalGap
                maxFontSize *= scaleFactor
            }

            //组合在一起
            val groupId = uuid()
            for (bean in beanList) {
                bean.groupId = groupId
            }

            LPRendererHelper.renderElementList(delegate, beanList, true, Strategy.normal)
        }

        /**解析pct参数表 */
        fun parseParameterComparisonTable(provider: IParameterComparisonTableProvider?): List<BaseRenderer> {
            provider ?: return emptyList()
            //最终结果
            val beanList = mutableListOf<LPElementBean>()
            //标签集合
            val labelBeanList = mutableListOf<LPElementBean>()
            //功率集合
            val powerBeanList = mutableListOf<LPElementBean>()
            //深度集合
            val depthBeanList = mutableListOf<LPElementBean>()
            //存放网格item
            val gridBeanList = mutableListOf<LPElementBean>()

            val elementMargin = provider.elementMargin.toPixel()
            val gridMargin = gridItemMargin.toPixel()

            val numberTextItem = LPTextElement(LPElementBean().apply {
                text = "100"
                fontSize = pctTextFontSize
                charSpacing = ptcCharSpace
                name = text

                //参数, 使用最后一次的默认
                //com.angcyo.engrave.EngraveFlowDataHelper.generateEngraveConfig
            })

            //功率/深度文本的宽高
            val powerTextItem = LPTextElement(LPElementBean().apply {
                mtype = LPDataConstant.DATA_TYPE_TEXT
                fontSize = provider.labelTextFontSize
                text = "Power(%)"
                name = text

                charSpacing = numberTextItem.elementBean.charSpacing
                printPrecision = numberTextItem.elementBean.printPrecision
                printCount = numberTextItem.elementBean.printCount
            })
            val depthTextItem = LPTextElement(LPElementBean().apply {
                mtype = LPDataConstant.DATA_TYPE_TEXT
                fontSize = powerTextItem.elementBean.fontSize
                text = "Depth(%)"
                name = text
                angle = -90f

                charSpacing = numberTextItem.elementBean.charSpacing
                printPrecision = numberTextItem.elementBean.printPrecision
                printCount = numberTextItem.elementBean.printCount
            })
            //左上角标签文本
            val labelTextItem = LPTextElement(LPElementBean().apply {
                mtype = LPDataConstant.DATA_TYPE_TEXT
                fontSize = provider.titleTextFontSize
                val defaultLabel = buildString {
                    val deviceStateModel = vmApp<DeviceStateModel>()
                    deviceStateModel.getDeviceConfig(provider.gridPrintType)?.name?.let {
                        append(it)
                        append(" ")
                    }
                    append(gridLayerId.toLayerInfo()?.label ?: "")
                    val layerInfo = deviceStateModel.getDeviceLaserModule(provider.gridPrintType)
                    appendSpaceIfNotEmpty()
                    append(layerInfo?.toLabel() ?: "${provider.gridPrintType.toLaserWave()}nm")
                    if (LayerHelper.showDpiConfig(gridLayerId)) {
                        val layerDpi = LayerHelper.getProductLastLayerDpi(gridLayerId)
                        appendSpaceIfNotEmpty()
                        append(LaserPeckerHelper.findPxInfo(gridLayerId, layerDpi).toText())
                    }
                }
                text = if (labelText.isNullOrBlank()) defaultLabel else labelText!!.replace(
                    "%1", defaultLabel
                )
                name = text

                charSpacing = numberTextItem.elementBean.charSpacing
                printPrecision = numberTextItem.elementBean.printPrecision
                printCount = numberTextItem.elementBean.printCount
            })

            val offsetTop = if (!hideFunInt.have(HIDE_LABEL)) {
                labelTextItem.getTextHeight() + elementMargin
            } else {
                0f
            }

            //LPElementBean
            fun createGridItemBean(): LPElementBean = LPElementBean().apply {
                mtype = LPDataConstant.DATA_TYPE_TEXT
                fontSize = numberTextItem.elementBean.fontSize
                charSpacing = numberTextItem.elementBean.charSpacing

                //参数
                printPrecision = numberTextItem.elementBean.printPrecision
                printCount = numberTextItem.elementBean.printCount
                printPower = numberTextItem.elementBean.printPower
                printDepth = numberTextItem.elementBean.printDepth
            }

            val powerTextHeight =
                if (hideFunInt.have(HIDE_POWER)) 0f else powerTextItem.getTextHeight()
            val depthTextWidth =
                if (hideFunInt.have(HIDE_DEPTH)) 0f else depthTextItem.getTextWidth()
            val depthTextHeight =
                if (hideFunInt.have(HIDE_DEPTH)) 0f else depthTextItem.getTextHeight()
            val topPowderHeight =
                if (hideFunInt.have(HIDE_POWER_LABEL)) 0f else numberTextItem.getTextHeight()
            val leftDepthWith =
                if (hideFunInt.have(HIDE_DEPTH_LABEL)) 0f else numberTextItem.getTextWidth()

            val leftTextWidth = depthTextHeight + leftDepthWith + elementMargin
            val topTextHeight = offsetTop + powerTextHeight + topPowderHeight + elementMargin

            //格子开始的地方
            val gridLeft = elementMargin + leftTextWidth
            val gridTop = elementMargin + topTextHeight

            //每个格子的宽高, 不包含margin
            val gridWidth = gridItemSize.toPixel()
            val gridHeight = gridItemSize.toPixel()

            //格子总共占用的宽高
            val gridWidthSum = gridWidth * horizontalGridCount
            val gridHeightSum = gridHeight * verticalGridCount

            //表格总宽度
            val pctWidth = leftTextWidth + elementMargin + gridWidthSum
            //表格总高度
            val pctHeight = topTextHeight + elementMargin + gridHeightSum

            //横竖线
            @Pixel val powerList = mutableListOf<Float>() //功率分割线, 竖线
            val depthList = mutableListOf<Float>() //深度分割线, 横线

            val horizontalStep =
                (maxPower - minPower) * 1f / (horizontalGridCount - 1).withMinValue(1)
            val verticalStep = (maxDepth - minDepth) * 1f / (verticalGridCount - 1).withMinValue(1)

            //--格子
            @Pixel var x = gridLeft
            var y = gridTop

            //需要创建的功率和深度的数值
            val powerValueList = mutableListOf<Int>()
            val depthValueList = mutableListOf<Int>()

            //默认功率和深度列表
            for (power in 0 until horizontalGridCount) {
                powerValueList.add((minPower + power * horizontalStep).roundToInt())
            }
            for (depth in 0 until verticalGridCount) {
                depthValueList.add((minDepth + depth * verticalStep).roundToInt())
            }

            if (appointPowerDepth.isNotBlank()) {
                //指定功率和深度
                val splitList = if (appointPowerDepth.contains(":")) appointPowerDepth.split(":")
                else appointPowerDepth.split(".") //功率 深度
                splitList.getOrNull(0)?.split(" ")?.filter { it.isNotBlank() }?.apply {
                    powerValueList.clear()
                    mapTo(powerValueList) {
                        it.toIntOrNull() ?: 0
                    }
                }
                splitList.getOrNull(1)?.split(" ")?.filter { it.isNotBlank() }?.apply {
                    depthValueList.clear()
                    mapTo(depthValueList) {
                        it.toIntOrNull() ?: 0
                    }
                }
            }

            powerValueList.forEachIndexed { powerIndex, powerValue ->
                if (powerIndex >= horizontalGridCount) {
                    return@forEachIndexed
                }
                //功率值文本
                x = gridLeft + powerIndex * gridWidth

                if (powerIndex > 0) {
                    powerList.add(x)
                }
                val powerNumberItem = LPTextElement(createGridItemBean().apply {
                    text = "$powerValue"
                    name = "power[${text}]"
                })
                powerNumberItem.elementBean.left =
                    (x + gridWidth / 2 - powerNumberItem.getTextWidth() / 2).toMm()
                powerNumberItem.elementBean.top =
                    (offsetTop + powerTextHeight + elementMargin).toMm()
                powerBeanList.add(powerNumberItem.elementBean)

                if (powerIndex == 0 && depthValueList.isEmpty()) {
                    //没有深度值时, 画一根线
                    depthList.add(gridTop)
                }
                depthValueList.forEachIndexed { depthIndex, depthValue ->
                    if (depthIndex >= verticalGridCount) {
                        return@forEachIndexed
                    }
                    //深度值文本
                    y = gridTop + depthIndex * gridHeight

                    if (powerIndex == 0) {
                        if (depthIndex > 0) {
                            depthList.add(y)
                        } else if (depthValueList.size() <= 1) {
                            //只有一个深度值时, 在尾部画一根线
                            depthList.add(gridTop + (depthIndex + 1) * gridHeight)
                        }
                        val depthNumberItem = LPTextElement(createGridItemBean().apply {
                            text = "$depthValue"
                            name = "depth[${text}]"
                        })
                        depthNumberItem.elementBean.left =
                            (gridLeft - elementMargin - depthNumberItem.getTextWidth()).toMm()
                        depthNumberItem.elementBean.top =
                            (y + gridHeight / 2 - depthNumberItem.getTextHeight() / 2).toMm()
                        depthBeanList.add(depthNumberItem.elementBean)
                    }

                    //格子数据
                    val refPDValue = powerValue * depthValue
                    if ((refPDValue > minPowerDepthThreshold && refPDValue <= powerDepthThreshold) ||
                        RowsColumnsRangeItem.isRowColumnInRange(depthIndex + 1, powerIndex + 1)
                    ) {
                        gridBeanList.add(LPElementBean().apply {
                            val isCut = gridLayerId == LaserPeckerHelper.LAYER_CUT
                            this.isCut = isCut
                            //_layerId
                            mtype = LPDataConstant.DATA_TYPE_RECT
                            paintStyle =
                                if (isCut) Paint.Style.STROKE.toPaintStyleInt() else Paint.Style.FILL.toPaintStyleInt()
                            val w = max(2f, (gridWidth - gridMargin * 2))
                            val h = max(2f, (gridHeight - gridMargin * 2))
                            width = w.toMm()
                            height = h.toMm()
                            left = (x + gridMargin).toMm()
                            top = (y + gridMargin).toMm()
                            name = "grid[${powerValue},${depthValue}]"

                            //2023-11-8 支持设置图片
                            val bitmap = selectImage
                            if (bitmap != null) {
                                mtype = LPDataConstant.DATA_TYPE_BITMAP
                                imageOriginal = bitmap.toBase64Data()
                                if (gridLayerId == LaserPeckerHelper.LAYER_FILL) {
                                    //图片图层
                                    imageFilter = LPDataConstant.DATA_MODE_BLACK_WHITE
                                } else {
                                    imageFilter = LPDataConstant.DATA_MODE_DITHERING
                                }
                                scaleX = w / bitmap.width.toPixel()
                                scaleY = h / bitmap.height.toPixel()
                            }

                            //参数
                            dataMode = if (HawkEngraveKeys.checkCpu32 &&
                                !BuildHelper.isCpu64 &&
                                gridLayerId == LaserPeckerHelper.LAYER_PICTURE
                            ) LPDataConstant.DATA_MODE_GREY /*32位手机 图片图层使用灰度雕刻*/ else gridLayerId.toDataMode()

                            printCount =
                                PrintCountItem.getPrintCount(depthIndex + 1, powerIndex + 1)
                            printPower = powerValue
                            printDepth = depthValue
                            printType = provider.gridPrintType.toInt()
                            //printPrecision = numberTextItem.elementBean.printPrecision //HawkEngraveKeys.lastPrecision
                            printPrecision = gridPrintPrecision.ensurePrintPrecision()
                            dpi = gridLayerId.filterLayerDpi(
                                LayerHelper.getProductLastLayerDpi(gridLayerId)
                            )
                        })
                    }
                }
            }

            //---横竖线
            val gCodeHandler = GCodeWriteHandler()
            gCodeHandler.unit = IValueUnit.MM_UNIT
            gCodeHandler.isAutoCnc = _isAutoCnc
            val gCodeWriter = StringWriter()
            gCodeHandler.writer = gCodeWriter
            gCodeHandler.onPathStart()

            //功率分割线, 竖线
            powerList.forEach {
                gCodeHandler.closeCnc()
                gCodeWriter.appendLine("G0X${it.toMm()}Y${gridTop.toMm()}")
                gCodeHandler.openCnc()
                gCodeWriter.appendLine("G1Y${(gridTop + gridHeightSum).toMm()}")
            }
            //深度分割线, 横线
            depthList.forEach {
                gCodeHandler.closeCnc()
                gCodeWriter.appendLine("G0X${gridLeft.toMm()}Y${it.toMm()}")
                gCodeHandler.openCnc()
                gCodeWriter.appendLine("G1X${(gridLeft + gridWidthSum).toMm()}")
            }

            gCodeHandler.onPathEnd(true)
            gCodeWriter.flush()
            gCodeWriter.close()

            if (!hideFunInt.have(HIDE_GRID)) {
                val gcode = gCodeWriter.toString()
                beanList.add(createGridItemBean().apply {
                    mtype = LPDataConstant.DATA_TYPE_GCODE
                    paintStyle = Paint.Style.STROKE.toPaintStyleInt()
                    data = gcode
                    left = gridLeft.toMm()
                    top = gridTop.toMm()
                    name = "gridLine"
                })
            }

            //Label
            labelTextItem.elementBean.left =
                (pctWidth / 2 - labelTextItem.getTextWidth() / 2).toMm()
            labelTextItem.elementBean.top = 0f
            if (!hideFunInt.have(HIDE_LABEL)) {
                labelBeanList.add(labelTextItem.elementBean)
            }

            //---文本
            powerTextItem.elementBean.left =
                (gridLeft + gridWidthSum / 2 - powerTextItem.getTextWidth() / 2).toMm()
            powerTextItem.elementBean.top = offsetTop.toMm()
            if (!hideFunInt.have(HIDE_POWER)) {
                labelBeanList.add(powerTextItem.elementBean)
            }

            //左边的深度文本, 旋转了-90度, 所以需要特殊处理
            depthTextItem.elementBean.left =
                (gridLeft - leftDepthWith - elementMargin * 2 - depthTextHeight).toMm()
            depthTextItem.elementBean.top =
                (gridTop + gridHeightSum / 2 + depthTextWidth / 2).toMm()
            if (!hideFunInt.have(HIDE_DEPTH)) {
                labelBeanList.add(depthTextItem.elementBean)
            }

            //归一
            beanList.addAll(labelBeanList)
            if (!hideFunInt.have(HIDE_POWER_LABEL)) {
                beanList.addAll(powerBeanList)
            }
            if (!hideFunInt.have(HIDE_DEPTH_LABEL)) {
                beanList.addAll(depthBeanList)
            }
            beanList.addAll(gridBeanList)

            //组合在一起
            val groupId = uuid()
            for (bean in beanList) {
                bean.groupId = groupId

                //一致打印参数
                bean.printType = provider.gridPrintType.toInt()
                bean.dpi =
                    LayerHelper.getProductLastLayerDpi(
                        bean._layerId ?: LaserPeckerHelper.LAYER_LINE
                    )
                if (bean.dataMode == null) {
                    bean.printPrecision = HawkEngraveKeys.lastPrecision
                    bean.printPower = HawkEngraveKeys.lastPower
                    bean.printDepth = HawkEngraveKeys.lastDepth
                    bean.useLaserFrequency = HawkEngraveKeys.lastLaserFrequency != null
                    bean.laserFrequency = HawkEngraveKeys.lastLaserFrequency
                    bean.printCount = 1
                }
            }
            return LPRendererHelper.renderElementList(null, beanList, true, Strategy.normal)
        }
    }

    var renderDelegate: CanvasRenderDelegate? = null

    val laserPeckerModel = vmApp<LaserPeckerModel>()
    val deviceStateModel = vmApp<DeviceStateModel>()

    init {
        dialogTitle = _string(R.string.add_parameter_comparison_table)

        val needPreview = true //!isInPadMode()
        if (needPreview) {
            dialogMaxHeight = "0.95sh"
        }

        positiveButton { dialog, dialogViewHolder ->
            dialog.dismiss()
            addParameterComparisonTable()
        }

        onRenderAdapterAction = {

            if (deviceStateModel.isPenMode() || laserPeckerModel.isZOpen()) {
                //画布模式下, 只能用GCode
                gridLayerId = LaserPeckerHelper.LAYER_LINE
            }

            if (needPreview) {
                TablePreviewItem()() {
                    iParameterComparisonTableProvider = this@ParameterComparisonTableDialogConfig
                }
            }

            //格子数量选择
            GridCountItem()() {
                itemColumns = horizontalGridCount
                itemRows = verticalGridCount

                onItemChangeAction = {
                    horizontalGridCount = itemColumns
                    verticalGridCount = itemRows
                }
            }

            //强制指定功率深度
            AppointPowerDepthItem()()

            //额外的行列范围
            RowsColumnsRangeItem()()

            //雕刻次数
            PrintCountItem()()

            //备注标签
            PCTLabelItem()()

            //字体大小, 边距
            LabelSizeItem()() {
                itemTextFontSize = pctTextFontSize
                itemGridItemMargin = gridItemMargin

                onItemChangeAction = {
                    pctTextFontSize = itemTextFontSize
                    gridItemMargin = itemGridItemMargin
                }
            }

            //图层选择
            if (!deviceStateModel.isPenMode()) {
                LayerSegmentItem()() {
                    itemIncludeCutLayer = true
                    itemCurrentIndex = LayerHelper.getEngraveLayerList(itemIncludeCutLayer)
                        .indexOfFirst { it.layerId == gridLayerId }
                    observeItemChange {
                        gridLayerId = currentLayerInfo().layerId
                        //updateTablePreview()
                        refreshDslAdapter()
                    }
                }
                if (gridLayerId == LaserPeckerHelper.LAYER_PICTURE) {
                    PCTImageItem()()
                } else {
                    selectImage = null
                }
            } else {
                selectImage = null
            }

            //激光类型选择 激光光源选择
            if (laserPeckerModel.productInfoData.value?.isCSeries() != true) {
                val typeList = LaserPeckerHelper.findProductSupportLaserTypeList()
                if (typeList.size() > 1) {
                    //激光类型
                    EngraveLaserSegmentItem()() {
                        observeItemChange {
                            val type = currentLaserTypeInfo().type
                            gridPrintType = type
                            HawkEngraveKeys.lastType = type.toInt()

                            updateTablePreview()
                            refreshDslAdapter()
                        }
                    }
                }
            }

            if (laserPeckerModel.isCSeries()) {
                //C1 加速级别选择 加速级别
                EngraveOptionWheelItem()() {
                    itemTag = EngraveConfigEntity::precision.name
                    itemLabelText = _string(R.string.engrave_precision)
                    itemWheelList = EngraveHelper.percentList(5)
                    itemSelectedIndex = EngraveHelper.findOptionIndex(
                        itemWheelList,
                        gridPrintPrecision
                    )
                    observeItemChange {
                        gridPrintPrecision =
                            getSelectedWheelIntData(def = gridPrintPrecision).ensurePrintPrecision()
                    }
                }
            }

            //---雕刻参数---

            //分辨率dpi
            if (LayerHelper.showDpiConfig(gridLayerId)) {
                TransferDataPxItem()() {
                    itemPxList = LaserPeckerHelper.findProductLayerSupportPxList(gridLayerId)
                    selectorCurrentDpi(LayerHelper.getProductLastLayerDpi(gridLayerId))
                    itemHidden = itemPxList.isNullOrEmpty() //自动隐藏
                    observeItemChange {
                        //保存最后一次选择的dpi
                        val dpi =
                            itemPxList?.get(itemCurrentIndex)?.dpi ?: LaserPeckerHelper.DPI_254
                        HawkEngraveKeys.updateLayerDpi(gridLayerId, dpi)

                        updateTablePreview()
                    }
                }
            }

            //LP5 白光 出光频率
            val laserFrequencyList = _deviceConfigBean?.laserFrequencyList
            if (_showLaserFrequencyConfig &&
                laserPeckerModel.isL5() &&
                gridPrintType == LaserPeckerHelper.LASER_TYPE_WHITE &&
                !laserFrequencyList.isNullOrEmpty()
            ) {
                EngraveLaserFrequencyItem()() {
                    initLaserFrequencyIfNeed()
                    itemWheelList = laserFrequencyList
                    itemUpdateAction(EngraveLaserFrequencyItem.PAYLOAD_UPDATE_LASER_FREQUENCY)
                    observeItemChange {
                        HawkEngraveKeys.lastLaserFrequency = itemWheelBean()
                    }
                }
            }

            //其他数据的参数
            //功率/深度/次数
            EngravePropertyItem()() {
                itemShowTimes = false
                itemShowPopupTip = false
                itemLabelText = "其它元素参数"
            }
        }
    }


    /**添加 功率 深度, 雕刻参数对照表. 耗时操作, 建议在子线程中执行*/
    @WorkerThread
    private fun addParameterComparisonTable() {
        val delegate = renderDelegate ?: return
        HawkEngraveKeys.enableTransferIndexCheck = false //可选, 关闭索引检查, 每次都重新传输
        HawkEngraveKeys.enableItemEngraveParams = true //必须
        HawkEngraveKeys.enableSingleItemTransfer = true //必须

        val result = parseParameterComparisonTable(this)
        if (result.size() == 1) {
            result.first().apply {
                if (this is CanvasGroupRenderer) {
                    var groupRenderProperty = getGroupRenderProperty()
                    var bounds = groupRenderProperty.getRenderBounds()
                    val matrix =
                        bounds.limitElementMaxSizeMatrix(tableBounds.width(), tableBounds.height())
                    applyScaleMatrix(matrix, Reason.code, null)
                    groupRenderProperty = getGroupRenderProperty()
                    bounds = groupRenderProperty.getRenderBounds()
                    val cx = min(tableBounds.centerX(), tableBounds.centerY())
                    val cy = cx
                    matrix.setTranslate(cx - bounds.centerX(), cy - bounds.centerY())
                    applyTranslateMatrix(matrix, Reason.code, null)
                }
            }
        }

        //添加到渲染器
        delegate.renderManager.addElementRenderer(result, true, Reason.user, Strategy.normal)
    }

    override var elementMargin: Float = 2f
    override var gridPrintType: Byte = DeviceHelper.getProductLaserType()
}

fun DslAdapter?.updateTablePreview() {
    this?.eachItem { index, dslAdapterItem ->
        if (dslAdapterItem is TablePreviewItem) {
            dslAdapterItem.updatePreview()
        }
    }
}

@DSL
fun Context.addParameterComparisonTableDialog(config: ParameterComparisonTableDialogConfig.() -> Unit): Dialog {
    return ParameterComparisonTableDialogConfig().run {
        dialogContext = this@addParameterComparisonTableDialog
        configBottomDialog()
        config()
        show()
    }
}