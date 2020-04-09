package com.angcyo.chart

import com.angcyo.library.L
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

/**
 * [LineChart]线性图表配置
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/04/07
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
open class LineChartConfig : BaseChartConfig<Entry, LineDataSet>() {

    /**激活绘制圆*/
    var lineDrawCircleEnable: Boolean = true

    /**圆内的hole*/
    var lineDrawCircleHole: Boolean = true

    /**填充绘制*/
    var lineDrawFilled: Boolean = false

    /**线的显示样式*/
    var lineMode = LineDataSet.Mode.LINEAR

    init {
        /**线的宽度, 0-10f dp*/
        //chartDataSetWidth = 1f
    }

    //<editor-fold desc="Entry数据">

    /**先调用[addEntry], 后调用此方法*/
    override fun addDataSet(label: String?, action: LineDataSet.() -> Unit) {
        if (entryList.isEmpty()) {
            L.w("Entry为空, 请检查是否先调用了[addEntry].")
        }
        LineDataSet(entryList, label).apply {
            configDataSet(this, action)

            //高亮使用蚂蚁线
            //enableDashedHighlightLine()
            //setFillFormatter { dataSet, dataProvider ->  }
            setDrawCircleHole(lineDrawCircleHole)
            setDrawCircles(lineDrawCircleEnable)

            setDrawFilled(lineDrawFilled)
            fillAlpha
            fillColor
            fillDrawable

            mode = lineMode

            addDataSet(this)
        }
    }

    /**添加最近一条线上的点*/
    override fun addEntry(action: Entry.() -> Unit) {
        entryList.add(Entry().apply(action))
    }

    //</editor-fold desc="Entry数据">

    var lineDataConfig: (LineData) -> Unit = {}

    override fun onSetChartData(chart: Chart<*>, dataSetList: List<LineDataSet>) {
        chart.data = LineData(dataSetList).apply(lineDataConfig)
    }
}

fun dslLineChart(chart: Chart<*>?, action: LineChartConfig.() -> Unit = {}): LineChart? {
    chart?.apply {
        LineChartConfig().also {
            it.action()
            it.doIt(chart)
        }
    }
    return chart as? LineChart?
}