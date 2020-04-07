package com.angcyo.chart

import android.graphics.Color
import android.graphics.Paint
import com.angcyo.library.L
import com.angcyo.library.ex._color
import com.angcyo.library.ex.isDebugType
import com.angcyo.library.ex.undefined_color
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.charts.Chart.PAINT_INFO
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener

/**
 * [Chart] 公共配置
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/04/07
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
abstract class BaseChartConfig {

    //<editor-fold desc="基础配置">

    var chartEnableLog: Boolean = isDebugType()

    /**是否激活手势*/
    var chartTouchEnabled: Boolean = true
    var chartDrawMarkers: Boolean = true

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

    open fun configBase(chart: Chart<*>) {
        chart.apply {
            isLogEnabled = chartEnableLog
            setTouchEnabled(chartTouchEnabled)

            //marker
            setDrawMarkers(chartDrawMarkers)

            if (this is LineChart) {
                setDrawGridBackground(chartDrawGridBackground)

                isDragXEnabled = chartDragXEnabled
                isDragYEnabled = chartDragYEnabled

                isScaleXEnabled = chartScaleXEnabled
                isScaleYEnabled = chartScaleYEnabled

                setPinchZoom(chartPinchZoomEnabled)
            }

            //listener
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onNothingSelected() {
                    L.i("未选择!")
                }

                override fun onValueSelected(entry: Entry, highlight: Highlight) {
                    L.i("entry:$entry highlight:$highlight")
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

    open fun configAxis(chart: Chart<*>) {
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

        }

        if (chart is LineChart) {
            chart.axisLeft.apply {
                isEnabled = chartLeftAxisEnable
                setPosition(chartLeftAxisPosition)
                valueFormatter = chartLeftAxisValueFormatter
                axisLineColor = chartLeftAxisLineColor
                setDrawLabels(chartLeftAxisDrawLabels)
                setDrawGridLines(chartLeftAxisDrawGridLines)
                gridColor = chartLeftAxisGridLineColor
                textColor = chartLeftAxisLabelTextColor
                axisMaximum
                axisMinimum
                textSize
            }
            chart.axisRight.apply {
                isEnabled = chartRightAxisEnable
                setPosition(chartRightAxisPosition)
                valueFormatter = chartRightAxisValueFormatter
                axisLineColor = chartRightAxisLineColor
                setDrawLabels(chartRightAxisDrawLabels)
                setDrawGridLines(chartRightAxisDrawGridLines)
                gridColor = chartRightAxisGridLineColor
                textColor = chartRightAxisLabelTextColor
                axisMaximum
                axisMinimum
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

    //<editor-fold desc="Entry数据">


    //</editor-fold desc="Entry数据">

    //<editor-fold desc="执行">

    open fun doIt(chart: Chart<*>) {
        configBase(chart)
        configNoData(chart)
        configDescription(chart)
        configAxis(chart)

        //刷新界面
        chart.postInvalidateOnAnimation()
    }

    //</editor-fold desc="执行">
}