package com.angcyo.chart

import android.graphics.Color
import android.graphics.Paint
import com.angcyo.library.L
import com.angcyo.library.ex._color
import com.angcyo.library.ex.isDebugType
import com.angcyo.library.ex.undefined_color
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.BarLineChartBase
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.charts.Chart.PAINT_INFO
import com.github.mikephil.charting.components.IMarker
import com.github.mikephil.charting.components.Legend.*
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BaseDataSet
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineRadarDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.IDataSet
import com.github.mikephil.charting.listener.OnChartValueSelectedListener

/**
 * [Chart] 公共配置
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/04/07
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
abstract class BaseChartConfig<EntryType : Entry, DataSet : IDataSet<EntryType>> {

    //<editor-fold desc="数据">

    val dataSetList = mutableListOf<DataSet>()

    protected var lastEntryList = mutableListOf<EntryType>()

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

    /**[BarLineChartBase]*/
    var chartDragXEnabled: Boolean = true
    var chartDragYEnabled: Boolean = true

    /**[BarLineChartBase]*/
    var chartDragEnabled: Boolean = true
        set(value) {
            field = value
            chartDragXEnabled = value
            chartDragYEnabled = value
        }

    /**[BarLineChartBase]*/
    var chartScaleXEnabled: Boolean = true
    var chartScaleYEnabled: Boolean = true

    /**[BarLineChartBase]*/
    var chartScaleEnabled: Boolean = true
        set(value) {
            field = value
            chartScaleXEnabled = value
            chartScaleYEnabled = value
        }

    /**[BarLineChartBase], 激活捏合放大缩小*/
    var chartPinchZoomEnabled: Boolean = false

    /**[BarLineChartBase]*/
    var chartDrawGridBackground: Boolean = false

    /**触摸拖动时, 高亮[BarLineChartBase]*/
    var lineHighlightPerDragEnabled: Boolean = true

    /**...[BarLineChartBase]*/
    var lineHighLightPerTapEnabled: Boolean = true

    /**[BarLineChartBase] [BarChart]*/
    var chartMaxVisibleCount = 100

    /**[BarChart], 是否绘制 顶部/底部 不在Value区域的部分*/
    var barDrawBarShadow = false

    /**Value绘制在Bar顶部挨着的上面[BarChart], 关闭之后将在Bar顶部inside绘制*/
    var barDrawValueAboveBar = true

    /**[BarChart],x轴左右预留Bar的一半宽度空隙*/
    var barFitBars = false

    /**[BarChart], 整个Bar高亮, 还是部分高亮*/
    var barHighlightFullEnabled = false

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

            //setExtraOffsets()

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

                    isHighlightPerDragEnabled = lineHighlightPerDragEnabled
                    isHighlightPerTapEnabled = lineHighLightPerTapEnabled
                    setMaxVisibleValueCount(chartMaxVisibleCount)
                }
            }

            if (this is BarChart) {
                setDrawBarShadow(barDrawBarShadow)
                setDrawValueAboveBar(barDrawValueAboveBar)
                setFitBars(barFitBars)

                isHighlightFullBarEnabled = barHighlightFullEnabled
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

    var chartXAxisLabelTextColor = _color(R.color.text_general_color)
    var chartLeftAxisLabelTextColor = _color(R.color.text_general_color)
    var chartRightAxisLabelTextColor = _color(R.color.text_general_color)

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

    var chartXAxisMinimum = Float.NaN
    var chartXAxisMaximum = Float.NaN

    var chartLeftAxisMinimum = Float.NaN
    var chartLeftAxisMaximum = Float.NaN

    var chartRightAxisMinimum = Float.NaN
    var chartRightAxisMaximum = Float.NaN

    /**居中标签, 分组Bar中效果明显*/
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

    open fun configAxis(chart: Chart<*>) {

        //X 轴
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
        }

        if (chart is BarLineChartBase<*>) {
            //Left 轴
            chart.axisLeft.apply {
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
            }
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
            }
        }
    }

    //</editor-fold desc="Axis配置">

    //<editor-fold desc="Description配置">

    /**右下角说明信息*/

    var chartDesText: String? = null
    var chartDesEnabled: Boolean = false
    var chartDesTextColor: Int = _color(R.color.text_general_color)
    var chartDesTextAlign = Paint.Align.RIGHT

    open fun configDescription(chart: Chart<*>) {
        chart.description.apply {
            text = chartDesText
            isEnabled = chartDesEnabled
            textColor = chartDesTextColor
            textAlign = chartDesTextAlign
            //setPosition()
            //xOffset
            //yOffset
        }
    }

    //</editor-fold desc="Description配置">

    //<editor-fold desc="Legend图例配置">

    var chartLegendEnable = true

    /**图例的样式*/
    var chartLegendForm = LegendForm.SQUARE

    /**图例shape的大小*/
    var chartLegendFormSize = 8f

    var chartLegendHorizontalAlignment = LegendHorizontalAlignment.LEFT
    var chartLegendVerticalAlignment = LegendVerticalAlignment.BOTTOM
    var chartLegendOrientation = LegendOrientation.HORIZONTAL
    var chartLegendDirection = LegendDirection.LEFT_TO_RIGHT
    var chartLegendDrawInside = false

    /**图例之间的空隙*/
    var chartLegendXEntrySpace = 6f
    var chartLegendYEntrySpace = 0f

    /**图例文本大小[6-24]dp*/
    var chartLegendTextSize = 10f

    /**图例文本颜色*/
    var chartLegendTextColor = _color(R.color.text_general_color)

    /**图例偏移, dp*/
    var chartLegendOffsetX = 5f
    var chartLegendOffsetY = 3f

    open fun configLegend(chart: Chart<*>) {
        chart.legend?.apply {
            isEnabled = chartLegendEnable
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
    var chartValueTextSize = 7f

    var chartValueTextColor = _color(R.color.text_general_color)

    /**数据集是否可见*/
    var chartDataSetVisible = true

    /**线上绘制值*/
    var chartDrawValues: Boolean = false

    var chartDrawIcons: Boolean = true

    /**宽度, 不同类型的图表, 宽度自行初始化宽度*/
    var chartDataSetWidth = 0.85f

    /**触摸时, 是否高亮*/
    var chartHighlightEnabled: Boolean = false

    /**点击后, 高亮线的颜色*/
    var chartHighlightColor = Color.rgb(255, 187, 115)

    /**数据集的颜色*/
    var chartDataSetColor = Color.rgb(255, 187, 115)

    /**数据集的颜色集合, 循环从里面去颜色绘制*/
    var chartDataSetColors = listOf<Int>()

    fun configDataSet(dataSet: DataSet, action: DataSet.() -> Unit = {}) {
        dataSet.apply {
            isVisible = chartDataSetVisible
            isHighlightEnabled = chartHighlightEnabled

            setDrawIcons(chartDrawIcons)
            setDrawValues(chartDrawValues)

            valueTextColor = chartValueTextColor
            valueTextSize = chartValueTextSize

            // customize legend entry
            formLineWidth
            formLineDashEffect
            formSize

            if (dataSet is BaseDataSet<*>) {
                if (chartDataSetColors.isEmpty()) {
                    dataSet.color = chartDataSetColor
                } else {
                    dataSet.colors = chartDataSetColors
                }
            }

            if (dataSet is LineRadarDataSet<*>) {
                dataSet.lineWidth = chartDataSetWidth
                dataSet.highLightColor = chartHighlightColor
            }

            if (dataSet is BarDataSet) {
                dataSet.barBorderWidth = 0f
            }

            action()
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

//    //<editor-fold desc="功能配置">
//
//    open fun configDescription(chart: Chart<*>) {
//
//    }
//
//    //</editor-fold desc="功能配置">

    //<editor-fold desc="执行">

    /**动画时长*/
    var chartAnimateDurationX = 0
    var chartAnimateDurationY = 0
    var chartAnimateEasingX = Easing.Linear
    var chartAnimateEasingY = Easing.Linear

    open fun doIt(chart: Chart<*>) {
        //chart.clear() //need?

        configBase(chart)
        configNoData(chart)
        configDescription(chart)
        configAxis(chart)
        configLegend(chart)

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
    }

    //</editor-fold desc="执行">
}