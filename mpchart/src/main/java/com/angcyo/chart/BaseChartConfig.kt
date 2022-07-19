package com.angcyo.chart

import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.view.Gravity
import com.angcyo.chart.formatter.PercentFormatter
import com.angcyo.library.L
import com.angcyo.library.ex._color
import com.angcyo.library.ex.isDebugType
import com.angcyo.library.ex.size
import com.angcyo.library.ex.undefined_color
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.*
import com.github.mikephil.charting.charts.Chart.PAINT_INFO
import com.github.mikephil.charting.components.IMarker
import com.github.mikephil.charting.components.Legend.*
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.LimitLine.LimitLabelPosition
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.IDataSet
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.utils.Utils

/**
 * [Chart] 公共配置
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/04/07
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
abstract class BaseChartConfig<EntryType : Entry, DataSetType : IDataSet<EntryType>> {

    companion object {
        /**默认文本大小dp*/
        val DEFAULT_TEXT_SIZE = 8f

        /**默认文本颜色*/
        val DEFAULT_TEXT_COLOR = _color(R.color.text_general_color)
    }

    //<editor-fold desc="数据">

    val dataSetList = mutableListOf<DataSetType>()

    var entryList = mutableListOf<EntryType>()

    /**添加数据集合,[DataSet]包含[Entry]*/
    abstract fun addDataSet(label: String? = null, action: DataSetType.() -> Unit = {})

    /**添加数据点*/
    abstract fun addEntry(action: EntryType.() -> Unit)

    /**最终设置图表数据*/
    abstract fun onSetChartData(chart: Chart<*>, dataSetList: List<DataSetType>)

    /**添加数据集合,[DataSet]包含[Entry]*/
    open fun addDataSet(dataSet: DataSetType) {
        dataSetList.add(dataSet)
        entryList = mutableListOf()
    }

    /**添加数据点*/
    open fun addEntry(entry: EntryType) {
        entryList.add(entry)
    }

    open fun addEntry(
        x: Float = 0f,
        y: Float = 0f,
        icon: Drawable? = null,
        data: Any? = null,
        action: EntryType.() -> Unit = {}
    ) {
        addEntry {
            this.x = x
            this.y = y
            this.icon = icon
            this.data = data
            action()
        }
    }

    //</editor-fold desc="数据">

    //<editor-fold desc="基础配置">

    /**Log开关*/
    var chartEnableLog: Boolean = isDebugType()

    /**是否激活手势*/
    var chartTouchEnabled: Boolean = true
    var chartDrawMarkers: Boolean = true

    /**高亮时, 显示的marker*/
    var chartMarker: IMarker? = null

    /**绘制边框[BarLineChartBase]*/
    var chartDrawBorders: Boolean = false

    /**表格整体偏移. dp*/
    var chartExtraOffsetLeft = 0f
    var chartExtraOffsetTop = 0f
    var chartExtraOffsetRight = 0f
    var chartExtraOffsetBottom = 0f

    /**[BarLineChartBase]*/
    var chartDragXEnabled: Boolean = false
    var chartDragYEnabled: Boolean = false

    /**[BarLineChartBase]*/
    var chartDragEnabled: Boolean = false
        set(value) {
            field = value
            chartDragXEnabled = value
            chartDragYEnabled = value
        }

    /**[BarLineChartBase]*/
    var chartScaleXEnabled: Boolean = false
        set(value) {
            field = value
            if (value) {
                chartDragXEnabled = true
            }
        }

    var chartScaleYEnabled: Boolean = false
        set(value) {
            field = value
            if (value) {
                chartDragYEnabled = true
            }
        }

    /**[BarLineChartBase]*/
    var chartScaleEnabled: Boolean = false
        set(value) {
            field = value
            chartScaleXEnabled = value
            chartScaleYEnabled = value
        }

    /**[BarLineChartBase], 激活捏合放大缩小*/
    var chartPinchZoomEnabled: Boolean = false

    /**[BarLineChartBase]*/
    var chartDrawGridBackground: Boolean = false

    /**dp*/
    var chartMaxHighlightDistance = 500f

    /**触摸拖动时, 高亮[BarLineChartBase]*/
    var lineHighlightPerDragEnabled: Boolean = true

    /**自动将线缩放到最小值 最大值范围内*/
    var lineAutoScaleMinMaxEnabled: Boolean = false

    /**...[BarLineChartBase]*/
    var chartHighLightPerTapEnabled: Boolean = true

    /**[BarLineChartBase] [BarChart] [LineChart] [ScatterChart] [BubbleChart] [CombinedChart] [CandleStickChart]
     *  当[entry]总数小于[chartMaxVisibleCount]时, 才绘制Value[chartDrawValues] / icon
     *
     *  [com.github.mikephil.charting.renderer.DataRenderer.isDrawingValuesAllowed]
     * */
    var chartMaxVisibleCount = 100

    /**[BarChart], 是否绘制 顶部/底部 不在Value区域的部分*/
    var barDrawBarShadow = false

    /**Value绘制在Bar顶部挨着的上面[BarChart], 关闭之后将在Bar顶部inside绘制*/
    var barDrawValueAboveBar = true

    /**[BarChart],x轴左右预留Bar的一半宽度空隙*/
    var barFitBars = false

    /**[BarChart], 整个Bar高亮, 还是部分高亮*/
    var barHighlightFullEnabled = false

    /**[PieChart], 使用百分比显示Value*/
    var pieUsePercentValues = false
        set(value) {
            field = value
            chartValueFormatter = if (value) {
                PercentFormatter()
            } else {
                Utils.getDefaultValueFormatter()
            }
        }

    var pieCenterTextColor = DEFAULT_TEXT_COLOR
    var pieCenterText: CharSequence? = null
    var pieDrawCenterText = true

    /**dp*/
    var pieCenterTextSize = 12f

    /**dp*/
    var pieCenterTextOffsetX = 0f
    var pieCenterTextOffsetY = 0f

    /**绘制Hole*/
    var pieDrawHoleEnable = true

    /**默认切片是扇形, 可以通过此开关绘制成圆形. 圆形效果在点击时没有高凸效果.*/
    var pieDrawRoundedSlices = false

    /**切片绘制在hole下*/
    var pieDrawSlicesUnderHole = false
    var pieHoleColor = Color.WHITE
    var pieHoleRadius = 58f

    /**Hole外透明圆圈*/
    var pieTransparentCircleColor = Color.WHITE
    var pieTransparentCircleAlpha = 110
    var pieTransparentCircleRadius = 61f

    /**图表当前旋转的角度, 0°是3点钟方向*/
    var pieRotationAngle = 270f

    /**饼状图是否可以收拾旋转*/
    var pieRotationEnable = true

    /**饼状图绘制Value时, 可以在Value下面绘制Entry的Label*/
    var pieEntryLabelColor = DEFAULT_TEXT_COLOR
    var pieEntryLabelTextSize = DEFAULT_TEXT_SIZE
    var pieDrawEntryLabels = true

    /**饼状图圆最大的角度 [90-360]*/
    var pieMaxAngle = 360f

    /**dp 直线的宽度*/
    var radarWebLineWidth = 1f
    var radarWebSkipLineCount = 0

    /**直线的颜色*/
    var radarWebColor = Color.LTGRAY

    /**0-255*/
    var radarWebAlpha = 150

    /**dp 网的宽度*/
    var radarWebLineWidthInner = 1f

    /**dp 网的颜色*/
    var radarWebColorInner = Color.LTGRAY

    /**设置可见Entry的数量, 配置[moveViewToX], 可以实现 实时波形图, 电波图, 脑波图.
     * 此属性设置之前请先设置[axisMinimum] [axisMaximum]*/
    var chartVisibleXRangeMinimum = Float.NaN
    var chartVisibleXRangeMaximum = Float.NaN
    var chartVisibleYRangeMaximum = Float.NaN
    var chartVisibleYRangeMinimum = Float.NaN

    /**设置X轴方向上, 缩放的最小级别. 参考手势缩放
     * [chartVisibleXRangeMaximum]也会修改此属性对应的效果*/
    var chartScaleMinimumX = 1f

    /**设置Y轴方向上, 缩放的最小级别*/
    var chartScaleMinimumY = 1f

    /**选中Value回调*/
    var chartValueSelected: (entry: Entry, highlight: Highlight) -> Unit = { entry, highlight ->
        L.i("entry:$entry highlight:$highlight")
    }

    /**未选中Value回调*/
    var chartNothingSelected: () -> Unit = {
        L.i("未选择!")
    }

    open fun configBase(chart: Chart<*>) {
        chart.apply {
            isLogEnabled = chartEnableLog
            setTouchEnabled(chartTouchEnabled)

            isHighlightPerTapEnabled = chartHighLightPerTapEnabled

            setExtraOffsets(
                chartExtraOffsetLeft,
                chartExtraOffsetTop,
                chartExtraOffsetRight,
                chartExtraOffsetBottom
            )

            maxHighlightDistance = chartMaxHighlightDistance

            //dragDecelerationFrictionCoef

            //marker
            setDrawMarkers(chartDrawMarkers)
            marker = chartMarker

            if (this is BarLineChartBase<*>) {
                val lineChart = this

                lineChart.apply {
                    //setViewPortOffsets()
                    setDrawGridBackground(chartDrawGridBackground)

                    isDragXEnabled = chartDragXEnabled
                    isDragYEnabled = chartDragYEnabled

                    isScaleXEnabled = chartScaleXEnabled
                    isScaleYEnabled = chartScaleYEnabled

                    setPinchZoom(chartPinchZoomEnabled)

                    setDrawBorders(chartDrawBorders)
                    //setBorderColor()
                    //setBorderWidth()

                    isAutoScaleMinMaxEnabled = lineAutoScaleMinMaxEnabled

                    isHighlightPerDragEnabled = lineHighlightPerDragEnabled
                    setMaxVisibleValueCount(chartMaxVisibleCount)

                    setScaleMinima(chartScaleMinimumX, chartScaleMinimumY)

                    if (!chartVisibleXRangeMinimum.isNaN()) {
                        setVisibleXRangeMinimum(chartVisibleXRangeMinimum)
                    }
                    if (!chartVisibleXRangeMaximum.isNaN()) {
                        setVisibleXRangeMaximum(chartVisibleXRangeMaximum)
                    }
                    if (!chartVisibleYRangeMinimum.isNaN()) {
                        setVisibleYRangeMinimum(
                            chartVisibleYRangeMinimum,
                            YAxis.AxisDependency.LEFT
                        )
                        setVisibleYRangeMinimum(
                            chartVisibleYRangeMinimum,
                            YAxis.AxisDependency.RIGHT
                        )
                    }
                    if (!chartVisibleYRangeMaximum.isNaN()) {
                        setVisibleYRangeMaximum(
                            chartVisibleYRangeMaximum,
                            YAxis.AxisDependency.LEFT
                        )
                        setVisibleYRangeMaximum(
                            chartVisibleYRangeMaximum,
                            YAxis.AxisDependency.RIGHT
                        )
                    }
                }
            }

            if (this is BarChart) {
                setDrawBarShadow(barDrawBarShadow)
                setDrawValueAboveBar(barDrawValueAboveBar)
                setFitBars(barFitBars)

                isHighlightFullBarEnabled = barHighlightFullEnabled
            }

            if (this is CombinedChart) {
                setDrawValueAboveBar(barDrawValueAboveBar)
                setDrawBarShadow(barDrawBarShadow)
                isHighlightFullBarEnabled = barHighlightFullEnabled
            }

            if (this is PieChart) {
                setUsePercentValues(pieUsePercentValues)
                setCenterTextSize(pieCenterTextSize)
                setCenterTextColor(pieCenterTextColor)
                centerText = pieCenterText
                setCenterTextOffset(pieCenterTextOffsetX, pieCenterTextOffsetY)

                setDrawCenterText(pieDrawCenterText)
                isDrawHoleEnabled = pieDrawHoleEnable
                setHoleColor(pieHoleColor)
                holeRadius = pieHoleRadius
                setDrawRoundedSlices(pieDrawRoundedSlices)
                setDrawSlicesUnderHole(pieDrawSlicesUnderHole)

                setTransparentCircleColor(pieTransparentCircleColor)
                transparentCircleRadius = pieTransparentCircleRadius
                setTransparentCircleAlpha(pieTransparentCircleAlpha)

                rotationAngle = pieRotationAngle
                isRotationEnabled = pieRotationEnable

                setEntryLabelColor(pieEntryLabelColor)
                setEntryLabelTextSize(pieEntryLabelTextSize)
                setDrawEntryLabels(pieDrawEntryLabels)
                //setEntryLabelTypeface()

                maxAngle = pieMaxAngle
            }

            if (this is ScatterChart) {
                //this
            }

            if (this is BubbleChart) {
                //
            }

            if (this is RadarChart) {
                webLineWidth = radarWebLineWidth
                skipWebLineCount = radarWebSkipLineCount
                webColor = radarWebColor
                webAlpha = radarWebAlpha

                webLineWidthInner = radarWebLineWidthInner
                webColorInner = radarWebColorInner
            }

            //listener
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onNothingSelected() {
                    chartNothingSelected()
                }

                override fun onValueSelected(entry: Entry, highlight: Highlight) {
                    chartValueSelected(entry, highlight)
                }
            })
        }
    }

    //</editor-fold desc="基础配置">

    //<editor-fold desc="Description配置">

    /**右下角说明信息*/

    var chartDesText: String? = null
        set(value) {
            field = value
            chartDesEnabled = !value.isNullOrEmpty()
        }

    var chartDesEnabled: Boolean = false
    var chartDesTextColor: Int = DEFAULT_TEXT_COLOR
    var chartDesTextAlign = Paint.Align.LEFT

    /**px 当设置了[Position]时, [Offset]将不起作用*/
    var chartDesPositionX = Float.NaN
    var chartDesPositionY = Float.NaN

    /**dp*/
    var chartDesPositionXOffset = 2f

    var chartDesPositionYOffset = 2f
    var chartDesPositionGravity = Gravity.RIGHT or Gravity.BOTTOM

    open fun configDescription(chart: Chart<*>) {
        chart.description.apply {
            text = chartDesText
            isEnabled = chartDesEnabled
            textColor = chartDesTextColor
            textAlign = chartDesTextAlign
            if (chartDesPositionX.isNaN() && chartDesPositionY.isNaN()) {
                //default
            } else if (!chartDesPositionX.isNaN() && !chartDesPositionY.isNaN()) {
                setPosition(chartDesPositionX, chartDesPositionY)
            } else if (chartDesPositionX.isNaN()) {
                setPosition(0f, chartDesPositionY)
            } else if (chartDesPositionY.isNaN()) {
                setPosition(chartDesPositionX, 0f)
            }
            xOffset = chartDesPositionXOffset
            yOffset = chartDesPositionYOffset

            gravity = chartDesPositionGravity
        }
    }

    //</editor-fold desc="Description配置">

    //<editor-fold desc="Legend图例配置">

    /**图例, 有多个[DataSet]时自动开启*/
    var chartLegendEnable: Boolean? = null

    /**图例的样式*/
    var chartLegendForm = LegendForm.SQUARE

    /**图例shape的大小*/
    var chartLegendFormSize = 8f

    var chartLegendHorizontalAlignment = LegendHorizontalAlignment.LEFT
    var chartLegendVerticalAlignment = LegendVerticalAlignment.BOTTOM

    /**图例排列*/
    var chartLegendOrientation = LegendOrientation.HORIZONTAL

    /**图例方向*/
    var chartLegendDirection = LegendDirection.LEFT_TO_RIGHT

    var chartLegendDrawInside = false

    /**图例之间的空隙*/
    var chartLegendXEntrySpace = 6f
    var chartLegendYEntrySpace = 0f

    /**图例文本大小[6-24]dp*/
    var chartLegendTextSize = 10f

    /**图例文本颜色*/
    var chartLegendTextColor = DEFAULT_TEXT_COLOR

    /**图例偏移, dp*/
    var chartLegendOffsetX = 5f
    var chartLegendOffsetY = 3f

    open fun configLegend(chart: Chart<*>) {
        chart.legend?.apply {
            isEnabled = chartLegendEnable ?: (dataSetList.size() > 1)
            form = chartLegendForm
            formSize = chartLegendFormSize
            horizontalAlignment = chartLegendHorizontalAlignment
            verticalAlignment = chartLegendVerticalAlignment
            orientation = chartLegendOrientation
            direction = chartLegendDirection
            setDrawInside(chartLegendDrawInside)

            xEntrySpace = chartLegendXEntrySpace
            yEntrySpace = chartLegendYEntrySpace
            textSize = chartLegendTextSize
            textColor = chartLegendTextColor

            xOffset = chartLegendOffsetX
            yOffset = chartLegendOffsetY
        }
    }

    //</editor-fold desc="Legend图例配置">

    //<editor-fold desc="DataSet配置">

    /**Value文本大小*/
    var chartValueTextSize = DEFAULT_TEXT_SIZE

    var chartValueTextColor = DEFAULT_TEXT_COLOR

    /**数据集是否可见*/
    var chartDataSetVisible = true

    /**线上绘制值*/
    var chartDrawValues: Boolean = false

    var chartDrawIcons: Boolean = true

    /**宽度, 不同类型的图表, 宽度自行初始化宽度*/
    var chartDataSetWidth = 0.85f

    /**触摸时, 是否高亮. 横竖十字架线*/
    var chartHighlightEnabled: Boolean = false

    /**点击后, 高亮线的颜色*/
    var chartHighlightColor = Color.rgb(255, 187, 115)

    /**数据集的颜色*/
    var chartDataSetColor = Color.rgb(255, 187, 115)

    /**数据集的颜色集合, 循环从里面去颜色绘制*/
    var chartDataSetColors = listOf<Int>()

    /**高亮时, 是否绘制纵向的指示线*/
    var chartDrawVerticalHighlightIndicator = true

    /**高亮时, 是否绘制横向的指示线*/
    var chartDrawHorizontalHighlightIndicator = true

    /**[0-20]dp, 饼状 切片之间的间隙*/
    var pieSliceSpace = 3f

    /**选中之后, 切片需要额外偏移的距离. dp*/
    var pieSelectionShift = 5f

    /**[DataSet]的Formatter*/
    var chartValueFormatter: ValueFormatter? = Utils.getDefaultValueFormatter()

    /**[DataSet]依赖left or right 轴*/
    var chartAxisDependency = YAxis.AxisDependency.LEFT

    /**[dp] 高亮时, 气泡周围绘制边框的宽度 [chartHighlightColor]*/
    var bubbleHighlightCircleWidth = 1.5f

    /**激活绘制圆, 线上的圆圈*/
    var lineDrawCircleEnable: Boolean = true

    /**圆内的hole*/
    var lineDrawCircleHole: Boolean = true

    /**线的显示样式*/
    var lineMode = LineDataSet.Mode.LINEAR

    var barBorderWidth = 0f
    var barBorderColor = Color.BLACK
    var barShadowColor = Color.rgb(215, 215, 215)

    /**Part1 相当于圆心, 偏移的距离*/
    var pieValueLinePart1OffsetPercentage = pieTransparentCircleRadius + 20f

    /**Part1 长度占比*/
    var pieValueLinePart1Length = 0.2f

    /**Part2 长度占比*/
    var pieValueLinePart2Length = 0.5f

    /**设置 outside 时, 会绘制在饼状图的外面, 并用线连接*/
    var pieValuePositionX = PieDataSet.ValuePosition.INSIDE_SLICE

    /**当Y值, outside 时, 会绘制线*/
    var pieValuePositionY = PieDataSet.ValuePosition.INSIDE_SLICE

    /**线的颜色*/
    var pieValueLineColor = DEFAULT_TEXT_COLOR

    /**px, 线的宽度*/
    var pieValueLineWidth = 1f

    /**绘制填充*/
    var chartDrawFilled = false

    /**填充颜色*/
    var chartFillColor = Color.rgb(140, 234, 255)
    var chartFillAlpha = 25
    var chartFillDrawable: Drawable? = null

    /**高亮时, 绘制圆.需要先激活[chartHighlightEnabled]*/
    var radarDrawHighlightCircleEnabled = true

    /**蜡烛 底线的颜色*/
    var candleShadowColor = Color.DKGRAY

    /**dp*/
    var candleShadowWidth = 1f

    /**open-close>0时, 使用此颜色绘制*/
    var candleDecreasingColor = Color.RED
    var candleDecreasingPaintStyle = Paint.Style.FILL

    /**open-close<0时, 使用此颜色绘制*/
    var candleIncreasingColor = Color.rgb(122, 242, 84)
    var candleIncreasingPaintStyle = Paint.Style.STROKE

    /**open-close=0时, 使用此颜色绘制*/
    var candleNeutralColor = Color.BLUE

    /**散列图形 形状*/
    var scatterShape: ScatterChart.ScatterShape = ScatterChart.ScatterShape.CIRCLE
    var scatterShapeHoleColor = ColorTemplate.COLOR_NONE
    var scatterShapeHoleRadius = 0f
    var scatterShapeSize = 25f

    fun configDataSet(dataSet: IDataSet<*>) {
        dataSet.apply {
            isVisible = chartDataSetVisible
            isHighlightEnabled = chartHighlightEnabled

            setDrawIcons(chartDrawIcons)
            setDrawValues(chartDrawValues)

            valueTextColor = chartValueTextColor
            valueTextSize = chartValueTextSize

            valueFormatter = chartValueFormatter

            // customize legend entry
            formLineWidth
            formLineDashEffect
            formSize

            axisDependency = chartAxisDependency

            if (dataSet is BaseDataSet<*>) {
                if (chartDataSetColors.isEmpty()) {
                    dataSet.color = chartDataSetColor
                } else {
                    dataSet.colors = ArrayList(chartDataSetColors)
                }
            }

            if (dataSet is LineRadarDataSet<*>) {
                dataSet.lineWidth = chartDataSetWidth

                dataSet.highLightColor = chartHighlightColor

                dataSet.setDrawFilled(chartDrawFilled)
                dataSet.fillAlpha = chartFillAlpha
                dataSet.fillColor = chartFillColor
                dataSet.fillDrawable = chartFillDrawable
            }

            if (dataSet is LineScatterCandleRadarDataSet<*>) {
                dataSet.setDrawVerticalHighlightIndicator(chartDrawVerticalHighlightIndicator)
                dataSet.setDrawHorizontalHighlightIndicator(chartDrawHorizontalHighlightIndicator)
            }

            if (dataSet is LineDataSet) {
                //高亮使用蚂蚁线
                //enableDashedHighlightLine()
                //setFillFormatter { dataSet, dataProvider ->  }
                dataSet.setDrawCircleHole(lineDrawCircleHole)
                dataSet.setDrawCircles(lineDrawCircleEnable)

                dataSet.mode = lineMode
            }

            if (dataSet is BarDataSet) {
                dataSet.barBorderWidth = this@BaseChartConfig.barBorderWidth
                dataSet.barBorderColor = this@BaseChartConfig.barBorderColor
                dataSet.barShadowColor = this@BaseChartConfig.barShadowColor
            }

            if (dataSet is PieDataSet) {
                dataSet.sliceSpace = pieSliceSpace
                //dataSet.iconsOffset =
                dataSet.selectionShift = pieSelectionShift

                dataSet.isValueLineVariableLength = true
                dataSet.valueLinePart1OffsetPercentage = pieValueLinePart1OffsetPercentage
                dataSet.valueLinePart1Length = pieValueLinePart1Length
                dataSet.valueLinePart2Length = pieValueLinePart2Length
                dataSet.xValuePosition = pieValuePositionX
                dataSet.yValuePosition = pieValuePositionY
                dataSet.valueLineColor = pieValueLineColor
                dataSet.valueLineWidth = pieValueLineWidth
            }

            if (dataSet is BubbleDataSet) {
                dataSet.highlightCircleWidth = bubbleHighlightCircleWidth
            }

            if (dataSet is CandleDataSet) {
                dataSet.shadowColor = candleShadowColor
                dataSet.shadowWidth = candleShadowWidth
                dataSet.decreasingColor = candleDecreasingColor
                dataSet.decreasingPaintStyle = candleDecreasingPaintStyle
                dataSet.increasingColor = candleIncreasingColor
                dataSet.increasingPaintStyle = candleIncreasingPaintStyle
                dataSet.neutralColor = candleNeutralColor
            }

            if (dataSet is RadarDataSet) {
                dataSet.isDrawHighlightCircleEnabled = radarDrawHighlightCircleEnabled
            }

            if (dataSet is ScatterDataSet) {
                dataSet.setScatterShape(scatterShape)
                dataSet.scatterShapeHoleColor = this@BaseChartConfig.scatterShapeHoleColor
                dataSet.scatterShapeHoleRadius = this@BaseChartConfig.scatterShapeHoleRadius
                dataSet.scatterShapeSize = this@BaseChartConfig.scatterShapeSize
            }

        }
    }

    //</editor-fold desc="DataSet配置">

    //<editor-fold desc="NoData配置">

    var chartNoDataText: String = "暂无数据"
    var chartNoDataTextColor: Int = _color(R.color.colorAccent)

    open fun configNoData(chart: Chart<*>) {
        chart.setNoDataText(chartNoDataText)
        if (chartNoDataTextColor == undefined_color) {
            chartNoDataTextColor = chart.getPaint(PAINT_INFO).color
        }
        chart.setNoDataTextColor(chartNoDataTextColor)
        //chart.setNoDataTextTypeface(tf)
    }

    //</editor-fold desc="NoData配置">

    //<editor-fold desc="Axis配置">

    /**Axis 横轴. 左右纵轴配置*/

    /**是否激活轴*/
    var chartXAxisEnable = true
    var chartLeftAxisEnable = true
    var chartRightAxisEnable = false

    /**轴线的颜色*/
    var chartXAxisLineColor = Color.GRAY
    var chartLeftAxisLineColor = Color.GRAY
    var chartRightAxisLineColor = Color.GRAY

    var chartXAxisGridLineColor = Color.GRAY
    var chartLeftAxisGridLineColor = Color.GRAY
    var chartRightAxisGridLineColor = Color.GRAY

    var chartXAxisLabelTextColor = DEFAULT_TEXT_COLOR
    var chartLeftAxisLabelTextColor = DEFAULT_TEXT_COLOR
    var chartRightAxisLabelTextColor = DEFAULT_TEXT_COLOR

    /**[6-24]dp*/
    var chartXAxisLabelTextSize = 10f
    var chartLeftAxisLabelTextSize = 10f
    var chartRightAxisLabelTextSize = 10f

    /**格式化值*/
    var chartXAxisValueFormatter: ValueFormatter? = null
    var chartLeftAxisValueFormatter: ValueFormatter? = null
    var chartRightAxisValueFormatter: ValueFormatter? = null

    /**是否绘制轴上的label*/
    var chartXAxisDrawLabels = true
    var chartLeftAxisDrawLabels = true
    var chartRightAxisDrawLabels = true

    /**是否绘制轴中间的网格线*/
    var chartXAxisDrawGridLines = false
    var chartLeftAxisDrawGridLines = true
    var chartRightAxisDrawGridLines = true

    /**轴绘制的位置*/
    var chartXAxisPosition = XAxis.XAxisPosition.BOTTOM
    var chartLeftAxisPosition = YAxis.YAxisLabelPosition.OUTSIDE_CHART
    var chartRightAxisPosition = YAxis.YAxisLabelPosition.OUTSIDE_CHART

    /**X轴 Label旋转的角度*/
    var chartXAxisLabelRotationAngle = 0f

    /**X轴的取值范围, [min-max]. 默认自动计算*/
    var chartXAxisMinimum = Float.NaN
    var chartXAxisMaximum = Float.NaN

    var chartLeftAxisMinimum = Float.NaN
    var chartLeftAxisMaximum = Float.NaN

    var chartRightAxisMinimum = Float.NaN
    var chartRightAxisMaximum = Float.NaN

    /**将Label绘制在轴的居中位置, 分组Bar中效果明显.
     * 居中位置绘制Label,默认Label会偏向外边
     * */
    var chartXAxisCenterLabels = false
    var chartLeftAxisCenterLabels = false
    var chartRightAxisCenterLabels = false

    var chartLeftZeroLineColor = Color.GRAY

    /**dp*/
    var chartLeftZeroLineWidth = 1f
    var chartLeftDrawZeroLine = false

    var chartRightZeroLineColor = Color.GRAY
    var chartRightZeroLineWidth = 1f
    var chartRightDrawZeroLine = false

    /**手势放大缩小时的粒度*/
    var chartXAxisGranularity = Float.NaN
    var chartLeftAxisGranularity = Float.NaN
    var chartRightAxisGranularity = Float.NaN

    /**[dp]*/
    var chartXAxisOffsetX = 2f
    var chartLeftAxisOffsetX = 2f
    var chartRightAxisOffsetX = 2f
    var chartXAxisOffsetY = 2f
    var chartLeftAxisOffsetY = 2f
    var chartRightAxisOffsetY = 2f

    /**轴上需要绘制Label的数量[2-25]*/
    var chartXAxisLabelCount = 6
    var chartLeftAxisLabelCount = 6
    var chartRightAxisLabelCount = 6

    /**强制指定Label的数量, 而不是均匀分布*/
    var chartXAxisForceLabels = false
    var chartLeftAxisForceLabels = false
    var chartRightAxisForceLabels = false

    open fun configAxis(chart: Chart<*>) {

        //X 轴
        if (chart !is PieChart) {
            chart.xAxis?.apply {
                isEnabled = chartXAxisEnable
                //enableGridDashedLine()
                position = chartXAxisPosition
                valueFormatter = chartXAxisValueFormatter
                axisLineColor = chartXAxisLineColor
                setDrawLabels(chartXAxisDrawLabels)
                //setDrawLimitLinesBehindData()
                setDrawGridLines(chartXAxisDrawGridLines)
                gridColor = chartXAxisGridLineColor
                textColor = chartXAxisLabelTextColor
                textSize = chartXAxisLabelTextSize
                labelRotationAngle = chartXAxisLabelRotationAngle

                xOffset = chartXAxisOffsetX
                yOffset = chartXAxisOffsetY

                if (chartXAxisMinimum.isNaN()) {
                    resetAxisMinimum()
                } else {
                    axisMinimum = chartXAxisMinimum
                }
                if (chartXAxisMaximum.isNaN()) {
                    resetAxisMaximum()
                } else {
                    axisMaximum = chartXAxisMaximum
                }

                if (!chartXAxisGranularity.isNaN()) {
                    granularity = chartXAxisGranularity
                }
                setCenterAxisLabels(chartXAxisCenterLabels)

                setDrawLimitLinesBehindData(chartXAxisDrawLimitLineBehindData)
                setDrawGridLinesBehindData(chartXAxisDrawGridLinesBehindData)

                removeAllLimitLines()
                xLimitList.forEach {
                    addLimitLine(it)
                }

                setLabelCount(chartXAxisLabelCount, chartXAxisForceLabels)
            }
        }

        var targetYAxis: YAxis? = null

        if (chart is BarLineChartBase<*>) {
            //Left 轴
            targetYAxis = chart.axisLeft

            //Right 轴
            chart.axisRight.apply {
                isEnabled = chartRightAxisEnable
                setPosition(chartRightAxisPosition)
                valueFormatter = chartRightAxisValueFormatter
                axisLineColor = chartRightAxisLineColor
                setDrawLabels(chartRightAxisDrawLabels)
                setDrawGridLines(chartRightAxisDrawGridLines)
                gridColor = chartRightAxisGridLineColor
                textColor = chartRightAxisLabelTextColor
                textSize = chartRightAxisLabelTextSize
                if (chartRightAxisMinimum.isNaN()) {
                    resetAxisMinimum()
                } else {
                    axisMinimum = chartRightAxisMinimum
                }
                if (chartRightAxisMaximum.isNaN()) {
                    resetAxisMaximum()
                } else {
                    axisMaximum = chartRightAxisMaximum
                }

                if (!chartRightAxisGranularity.isNaN()) {
                    granularity = chartRightAxisGranularity
                }
                setCenterAxisLabels(chartRightAxisCenterLabels)

                zeroLineColor = chartRightZeroLineColor
                zeroLineWidth = chartRightZeroLineWidth
                setDrawZeroLine(chartRightDrawZeroLine)

                xOffset = chartRightAxisOffsetX
                yOffset = chartRightAxisOffsetY

                setDrawLimitLinesBehindData(chartRightAxisDrawLimitLineBehindData)
                setDrawGridLinesBehindData(chartRightAxisDrawGridLinesBehindData)

                removeAllLimitLines()
                rightLimitList.forEach {
                    addLimitLine(it)
                }

                setLabelCount(chartRightAxisLabelCount, chartRightAxisForceLabels)
            }
        }

        //Y轴, 使用 Left轴 的配置
        if (chart is RadarChart) {
            targetYAxis = chart.yAxis
        }

        targetYAxis?.apply {
            isEnabled = chartLeftAxisEnable
            setPosition(chartLeftAxisPosition)
            valueFormatter = chartLeftAxisValueFormatter
            axisLineColor = chartLeftAxisLineColor
            setDrawLabels(chartLeftAxisDrawLabels)
            setDrawGridLines(chartLeftAxisDrawGridLines)
            gridColor = chartLeftAxisGridLineColor
            textColor = chartLeftAxisLabelTextColor
            textSize = chartLeftAxisLabelTextSize
            if (chartLeftAxisMinimum.isNaN()) {
                resetAxisMinimum()
            } else {
                axisMinimum = chartLeftAxisMinimum
            }
            if (chartLeftAxisMaximum.isNaN()) {
                resetAxisMaximum()
            } else {
                axisMaximum = chartLeftAxisMaximum
            }

            setCenterAxisLabels(chartLeftAxisCenterLabels)
            //spaceMin =
            //spaceTop =
            //spaceBottom =
            if (!chartLeftAxisGranularity.isNaN()) {
                granularity = chartLeftAxisGranularity
            }

            zeroLineColor = chartLeftZeroLineColor
            zeroLineWidth = chartLeftZeroLineWidth
            setDrawZeroLine(chartLeftDrawZeroLine)

            xOffset = chartLeftAxisOffsetX
            yOffset = chartLeftAxisOffsetY

            setDrawLimitLinesBehindData(chartLeftAxisDrawLimitLineBehindData)
            setDrawGridLinesBehindData(chartLeftAxisDrawGridLinesBehindData)

            removeAllLimitLines()
            leftLimitList.forEach {
                addLimitLine(it)
            }

            setLabelCount(chartLeftAxisLabelCount, chartLeftAxisForceLabels)
        }
    }

    //</editor-fold desc="Axis配置">

    //<editor-fold desc="Limit限制线">

    var chartXAxisLimitTextColor = DEFAULT_TEXT_COLOR
    var chartXAxisLimitLineColor = Color.rgb(237, 91, 91)
    var chartXAxisLimitLineWidth = chartDataSetWidth
    var chartXAxisLimitLabelPosition = LimitLabelPosition.RIGHT_TOP

    var chartLeftAxisLimitTextColor = DEFAULT_TEXT_COLOR
    var chartLeftAxisLimitLineColor = Color.rgb(237, 91, 91)
    var chartLeftAxisLimitLineWidth = chartDataSetWidth
    var chartLeftAxisLimitLabelPosition = LimitLabelPosition.RIGHT_TOP

    var chartRightAxisLimitTextColor = DEFAULT_TEXT_COLOR
    var chartRightAxisLimitLineColor = Color.rgb(237, 91, 91)
    var chartRightAxisLimitLineWidth = chartDataSetWidth
    var chartRightAxisLimitLabelPosition = LimitLabelPosition.RIGHT_TOP

    /**网格线绘制在data的下面*/
    var chartXAxisDrawGridLinesBehindData = true
    var chartLeftAxisDrawGridLinesBehindData = true
    var chartRightAxisDrawGridLinesBehindData = true

    /**Limit线绘制在data的下面*/
    var chartXAxisDrawLimitLineBehindData = false
    var chartLeftAxisDrawLimitLineBehindData = false
    var chartRightAxisDrawLimitLineBehindData = false

    private val xLimitList = mutableListOf<LimitLine>()
    private val leftLimitList = mutableListOf<LimitLine>()
    private val rightLimitList = mutableListOf<LimitLine>()

    fun addXAxisLimitLine(limit: Float, label: String? = null, action: LimitLine.() -> Unit = {}) {
        xLimitList.add(LimitLine(limit, label).apply {
            textColor = chartXAxisLimitTextColor
            lineColor = chartXAxisLimitLineColor
            isEnabled = true
            //label
            lineWidth = chartXAxisLimitLineWidth
            labelPosition = chartXAxisLimitLabelPosition
            action()
        })
    }

    fun addLeftAxisLimitLine(
        limit: Float,
        label: String? = null,
        action: LimitLine.() -> Unit = {}
    ) {
        leftLimitList.add(LimitLine(limit, label).apply {
            textColor = chartLeftAxisLimitTextColor
            lineColor = chartLeftAxisLimitLineColor
            isEnabled = true
            //label
            lineWidth = chartLeftAxisLimitLineWidth
            labelPosition = chartLeftAxisLimitLabelPosition
            action()
        })
    }

    fun addRightAxisLimitLine(
        limit: Float,
        label: String? = null,
        action: LimitLine.() -> Unit = {}
    ) {
        rightLimitList.add(LimitLine(limit, label).apply {
            textColor = chartRightAxisLimitTextColor
            lineColor = chartRightAxisLimitLineColor
            isEnabled = true
            //label
            lineWidth = chartRightAxisLimitLineWidth
            labelPosition = chartRightAxisLimitLabelPosition
            action()
        })
    }

    //</editor-fold desc="Limit限制线">

    //<editor-fold desc="执行">

    /**动画时长*/
    var chartAnimateDurationX = 0
    var chartAnimateDurationY = 0
    var chartAnimateEasingX = Easing.Linear
    var chartAnimateEasingY = Easing.Linear

    open fun doIt(chart: Chart<*>) {
        //chart.clear() //need?

        configNoData(chart)
        configDescription(chart)
        configLegend(chart)
        configAxis(chart)
        configBase(chart)

        if (entryList.isNotEmpty()) {
            addDataSet()
        }

        if (dataSetList.isEmpty()) {
            chart.data = null
        } else {
            onSetChartData(chart, dataSetList)
        }

        //刷新界面
        if (chartAnimateDurationX > 0 && chartAnimateDurationY > 0) {
            chart.animateXY(
                chartAnimateDurationX,
                chartAnimateDurationY,
                chartAnimateEasingX,
                chartAnimateEasingY
            )
        } else if (chartAnimateDurationX > 0) {
            chart.animateX(chartAnimateDurationX, chartAnimateEasingX)
        } else if (chartAnimateDurationY > 0) {
            chart.animateY(chartAnimateDurationY, chartAnimateEasingY)
        } else {
            chart.postInvalidateOnAnimation()
        }

        if (chart is BarLineChartBase<*>) {
            //chart.moveViewToX()
        }
    }

    //</editor-fold desc="执行">
}