package com.angcyo.chart

import android.graphics.drawable.Drawable
import com.angcyo.library.L
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet

/**
 * [LineChart]配置项
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

    /**添加一根线*/
    fun addLineDataSet(
        entryList: List<Entry>,
        label: String? = null,
        action: LineDataSet.() -> Unit = {}
    ) {
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

            dataSetList.add(this)
        }
    }

    /**先调用[addLineEntry], 后调用此方法*/
    fun addLineDataSet(label: String? = null, action: LineDataSet.() -> Unit = {}) {
        if (lastEntryList.isEmpty()) {
            L.w("Entry为空, 请检查是否先调用了[addLineEntry].")
        }
        addLineDataSet(lastEntryList, label, action)
        lastEntryList = mutableListOf()
    }

    /**添加最近一条线上的点*/
    fun addLineEntry(action: Entry.() -> Unit) {
        lastEntryList.add(Entry().apply(action))
    }

    fun addLineEntry(x: Float = 0f, y: Float = 0f, icon: Drawable? = null, data: Any? = null) {
        addLineEntry {
            this.x = x
            this.y = y
            this.icon = icon
            this.data = data
        }
    }

    //</editor-fold desc="Entry数据">

    //<editor-fold desc="Limit限制线">

    private val limitList = mutableListOf<LimitLine>()

    fun addLimitLine(limit: Float, label: String? = null, action: LimitLine.() -> Unit = {}) {
        limitList.add(LimitLine(limit, label).apply {
            //textColor
            //lineColor
            //isEnabled
            //label
            lineWidth = chartDataSetWidth
            //labelPosition
            action()
        })
    }

    fun configLimit(chart: LineChart) {
        var yAxis = chart.axisLeft
        if (!yAxis.isEnabled) {
            yAxis = chart.axisRight
        }

        if (yAxis.isEnabled) {
            limitList.forEach { yAxis.addLimitLine(it) }
        }
    }

    //</editor-fold desc="Limit限制线">

    var lineDataConfig: (LineData) -> Unit = {}

    override fun doIt(chart: Chart<*>) {
        super.doIt(chart)

        if (chart is LineChart) {
            configLimit(chart)

            if (lastEntryList.isNotEmpty()) {
                addLineDataSet()
            }

            if (dataSetList.isEmpty()) {
                chart.data = null
            } else {
                chart.data = LineData(dataSetList as List<ILineDataSet>).apply(lineDataConfig)
            }
        }
    }
}

fun dslLineChart(chart: Chart<*>?, action: LineChartConfig.() -> Unit = {}): Chart<*>? {
    chart?.apply {
        LineChartConfig().also {
            it.action()
            it.doIt(chart)
        }
    }
    return chart
}