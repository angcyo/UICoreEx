package com.angcyo.canvas2.laser.pecker.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Paint
import android.graphics.RectF
import androidx.annotation.Keep
import androidx.annotation.WorkerThread
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.core.Strategy
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.GridCountItem
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.LabelSizeItem
import com.angcyo.canvas2.laser.pecker.dialog.dslitem.LayerSegmentItem
import com.angcyo.canvas2.laser.pecker.element.LPTextElement
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.EngraveLaserSegmentItem
import com.angcyo.canvas2.laser.pecker.util.LPRendererHelper
import com.angcyo.core.vmApp
import com.angcyo.dialog.BaseRecyclerDialogConfig
import com.angcyo.dialog.configBottomDialog
import com.angcyo.gcode.GCodeWriteHandler
import com.angcyo.item.style.itemCurrentIndex
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.bean.LPElementBean
import com.angcyo.laserpacker.device.DeviceHelper
import com.angcyo.laserpacker.device.EngraveHelper
import com.angcyo.laserpacker.device.HawkEngraveKeys
import com.angcyo.laserpacker.toPaintStyleInt
import com.angcyo.library.annotation.DSL
import com.angcyo.library.annotation.MM
import com.angcyo.library.annotation.Pixel
import com.angcyo.library.component.HawkPropertyValue
import com.angcyo.library.ex._string
import com.angcyo.library.ex.uuid
import com.angcyo.library.unit.IValueUnit
import com.angcyo.library.unit.toMm
import com.angcyo.library.unit.toPixel
import java.io.StringWriter
import kotlin.math.max

/**
 * 添加一个功率深度参数对照表
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/04/04
 */
class ParameterComparisonTableDialogConfig : BaseRecyclerDialogConfig() {

    @Keep
    companion object {
        /**功率深度阈值, 小于值才需要绘制格子*/
        internal var powerDepthThreshold: Float by HawkPropertyValue<Any, Float>(2400f)

        /**竖向网格的数量, 有多少行*/
        internal var verticalGridCount: Int by HawkPropertyValue<Any, Int>(10)

        /**横向网格的数量, 有多少列*/
        internal var horizontalGridCount: Int by HawkPropertyValue<Any, Int>(10)

        /**网格格子的边距*/
        @MM
        internal var gridItemMargin: Float by HawkPropertyValue<Any, Float>(2f)

        /**数字文本大小*/
        @MM
        internal var textFontSize: Float by HawkPropertyValue<Any, Float>(8f)
    }

    var renderDelegate: CanvasRenderDelegate? = null

    init {
        dialogTitle = _string(R.string.add_parameter_comparison_table)

        positiveButton { dialog, dialogViewHolder ->
            dialog.dismiss()
            addParameterComparisonTable()
        }

        onRenderAdapterAction = {

            //图层选择
            LayerSegmentItem()() {
                itemCurrentIndex =
                    EngraveHelper.engraveLayerList.indexOfFirst { it.layerMode == gridDataMode }
                observeItemChange {
                    gridDataMode = currentLayerInfo().layerMode
                }
            }

            //激光类型选择
            EngraveLaserSegmentItem()() {
                observeItemChange {
                    val type = currentLaserTypeInfo().type
                    gridPrintType = type
                }
            }

            //格子数量选择
            GridCountItem()() {
                itemColumns = horizontalGridCount
                itemRows = verticalGridCount
                itemPowerDepthThreshold = powerDepthThreshold

                onItemChangeAction = {
                    horizontalGridCount = itemColumns
                    verticalGridCount = itemRows
                    powerDepthThreshold = itemPowerDepthThreshold
                }
            }

            //字体大小, 边距
            LabelSizeItem()() {
                itemTextFontSize = textFontSize
                itemGridItemMargin = gridItemMargin

                onItemChangeAction = {
                    textFontSize = itemTextFontSize
                    gridItemMargin = itemGridItemMargin
                }
            }
        }
    }

    /**不同类型元素之间的距离*/
    @MM
    var elementMargin = 2f

    /**强行指定格子的数据类型*/
    var gridDataMode = LPDataConstant.DATA_MODE_BLACK_WHITE

    /**指定激光类型
     * [LaserPeckerHelper.LASER_TYPE_BLUE]
     * [LaserPeckerHelper.LASER_TYPE_WHITE]
     * */
    var gridPrintType = DeviceHelper.getProductLaserType()

    /**标签 Power/Depth 文本字体大小*/
    @MM
    val labelTextFontSize: Float
        get() = textFontSize * 2

    /**添加 功率 深度, 雕刻参数对照表. 耗时操作, 建议在子线程中执行*/
    @WorkerThread
    private fun addParameterComparisonTable() {
        val delegate = renderDelegate ?: return

        HawkEngraveKeys.enableItemEngraveParams = true //必须
        HawkEngraveKeys.enableSingleItemTransfer = true //必须

        @Pixel val bounds =
            vmApp<LaserPeckerModel>().productInfoData.value?.previewBounds ?: RectF(
                0f,
                0f,
                160f.toPixel(),
                160f.toPixel()
            )

        val elementMargin = elementMargin.toPixel()
        val gridMargin = gridItemMargin.toPixel()

        val numberTextItem = LPTextElement(LPElementBean().apply {
            text = "100"
            fontSize = textFontSize
            charSpacing = 0.2f
            name = text

            //参数, 使用最后一次的默认
            //com.angcyo.engrave.EngraveFlowDataHelper.generateEngraveConfig
        })

        //功率/深度文本的宽高
        val powerTextItem = LPTextElement(LPElementBean().apply {
            mtype = LPDataConstant.DATA_TYPE_TEXT
            fontSize = labelTextFontSize
            text = "Power(%)"
            name = text

            printPrecision = numberTextItem.elementBean.printPrecision
            printCount = numberTextItem.elementBean.printCount
        })
        val depthTextItem = LPTextElement(LPElementBean().apply {
            mtype = LPDataConstant.DATA_TYPE_TEXT
            fontSize = powerTextItem.elementBean.fontSize
            text = "Depth(%)"
            name = text
            angle = -90f

            printPrecision = numberTextItem.elementBean.printPrecision
            printCount = numberTextItem.elementBean.printCount
        })

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

        val powerTextHeight = powerTextItem.getTextHeight()
        val depthTextWidth = depthTextItem.getTextWidth()
        val depthTextHeight = depthTextItem.getTextHeight()
        val leftTextWidth = depthTextHeight + numberTextItem.getTextWidth() + elementMargin
        val topTextHeight = powerTextHeight + numberTextItem.getTextHeight() + elementMargin

        //格子开始的地方
        val gridLeft = bounds.left + elementMargin + leftTextWidth
        val gridTop = bounds.top + elementMargin + topTextHeight

        //格子总共占用的宽高
        val gridWidthSum = bounds.right - gridLeft
        val gridHeightSum = bounds.bottom - gridTop

        //每个格子的宽高, 不包含margin
        val gridWidth = gridWidthSum / horizontalGridCount
        val gridHeight = gridHeightSum / verticalGridCount

        //最终结果
        val beanList = mutableListOf<LPElementBean>()

        //存放网格item
        val gridItemList = mutableListOf<LPElementBean>()

        //---数值
        val numberTextBeanList = mutableListOf<LPElementBean>()

        //横竖线
        @Pixel
        val powerList = mutableListOf<Float>() //功率分割线, 竖线
        val depthList = mutableListOf<Float>() //深度分割线, 横线

        val max = 100
        val horizontalStep = max / horizontalGridCount
        val verticalStep = max / verticalGridCount

        //--格子
        @Pixel
        var x = gridLeft
        var y = gridTop
        for (power in 0 until horizontalGridCount) {
            //功率
            x = gridLeft + power * gridWidth

            if (power > 0) {
                powerList.add(x)
            }
            val powerNumberItem = LPTextElement(createGridItemBean().apply {
                text = "${(power + 1) * horizontalStep}"
                name = "power[${text}]"
            })
            powerNumberItem.elementBean.left =
                (x + gridWidth / 2 - powerNumberItem.getTextWidth() / 2).toMm()
            powerNumberItem.elementBean.top =
                (bounds.top + powerTextHeight + elementMargin).toMm()
            numberTextBeanList.add(powerNumberItem.elementBean)

            for (depth in 0 until verticalGridCount) {
                //深度
                y = gridTop + depth * gridHeight

                if (power == 0) {
                    if (depth > 0) {
                        depthList.add(y)
                    }
                    val depthNumberItem = LPTextElement(createGridItemBean().apply {
                        text = "${(depth + 1) * horizontalStep}"
                        name = "depth[${text}]"
                    })
                    depthNumberItem.elementBean.left =
                        (gridLeft - elementMargin - depthNumberItem.getTextWidth()).toMm()
                    depthNumberItem.elementBean.top =
                        (y + gridHeight / 2 - depthNumberItem.getTextHeight() / 2).toMm()
                    numberTextBeanList.add(depthNumberItem.elementBean)
                }

                val powerValue = (power + 1) * horizontalStep
                val depthValue = (depth + 1) * verticalStep
                if (powerValue * depthValue <= powerDepthThreshold) {
                    gridItemList.add(LPElementBean().apply {
                        mtype = LPDataConstant.DATA_TYPE_RECT
                        paintStyle = Paint.Style.FILL.toPaintStyleInt()
                        width = max(2f, (gridWidth - gridMargin * 2)).toMm()
                        height = max(2f, (gridHeight - gridMargin * 2)).toMm()
                        left = (x + gridMargin).toMm()
                        top = (y + gridMargin).toMm()
                        name = "grid[${powerValue}:${depthValue}]"

                        //参数
                        dataMode = gridDataMode
                        printPrecision = numberTextItem.elementBean.printPrecision
                        printCount = numberTextItem.elementBean.printCount
                        printPower = powerValue
                        printDepth = depthValue
                    })
                }
            }
        }

        //---横竖线
        val gCodeHandler = GCodeWriteHandler()
        gCodeHandler.unit = IValueUnit.MM_UNIT
        gCodeHandler.isAutoCnc = false
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

        gCodeHandler.onPathEnd()
        gCodeWriter.flush()
        gCodeWriter.close()

        val gcode = gCodeWriter.toString()
        beanList.add(createGridItemBean().apply {
            mtype = LPDataConstant.DATA_TYPE_GCODE
            paintStyle = Paint.Style.STROKE.toPaintStyleInt()
            data = gcode
            left = gridLeft.toMm()
            top = gridTop.toMm()
            name = "gridLine"
        })

        //---数值
        beanList.addAll(numberTextBeanList)

        //---文本
        powerTextItem.elementBean.left =
            (gridLeft + gridWidthSum / 2 - powerTextItem.getTextWidth() / 2).toMm()
        powerTextItem.elementBean.top = (bounds.top).toMm()
        beanList.add(powerTextItem.elementBean)

        //左边的深度文本, 旋转了-90度, 所以需要特殊处理
        depthTextItem.elementBean.left =
            (gridLeft - numberTextItem.getTextWidth() - elementMargin * 2 - depthTextHeight).toMm()
        depthTextItem.elementBean.top = (gridTop + gridHeightSum / 2 + depthTextWidth / 2).toMm()

        beanList.add(depthTextItem.elementBean)

        //---格子
        beanList.addAll(gridItemList)

        //组合在一起
        val groupId = uuid()
        for (bean in beanList) {
            bean.groupId = groupId

            //一致打印参数
            bean.printType = gridPrintType.toInt()
        }

        LPRendererHelper.renderElementList(delegate, beanList, true, Strategy.normal)
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