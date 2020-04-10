package com.angcyo.chart

import com.angcyo.library.L
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.charts.ScatterChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.ScatterData
import com.github.mikephil.charting.data.ScatterDataSet
import com.github.mikephil.charting.utils.ColorTemplate

/**
 * 分散/散列 图表配置
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/04/10
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
open class ScatterChartConfig : BaseChartConfig<Entry, ScatterDataSet>() {

    /**散列图形 形状*/
    var scatterShape: ScatterChart.ScatterShape = ScatterChart.ScatterShape.CIRCLE
    var scatterShapeHoleColor = ColorTemplate.COLOR_NONE
    var scatterShapeHoleRadius = 0f
    var scatterShapeSize = 25f

    override fun addDataSet(label: String?, action: ScatterDataSet.() -> Unit) {
        if (entryList.isEmpty()) {
            L.w("Entry为空, 请检查是否先调用了[addEntry].")
        }
        ScatterDataSet(entryList, label).apply {
            configDataSet(this, action)

            setScatterShape(scatterShape)
            scatterShapeHoleColor = this@ScatterChartConfig.scatterShapeHoleColor
            scatterShapeHoleRadius = this@ScatterChartConfig.scatterShapeHoleRadius
            scatterShapeSize = this@ScatterChartConfig.scatterShapeSize

            addDataSet(this)
        }
    }

    override fun addEntry(action: Entry.() -> Unit) {
        entryList.add(Entry().apply(action))
    }

    var scatterDataConfig: (ScatterData) -> Unit = {}

    override fun onSetChartData(chart: Chart<*>, dataSetList: List<ScatterDataSet>) {
        chart.data = ScatterData(dataSetList).apply {
            scatterDataConfig(this)
        }
    }
}

fun dslScatterChart(chart: Chart<*>?, action: ScatterChartConfig.() -> Unit = {}): ScatterChart? {
    chart?.apply {
        ScatterChartConfig().also {
            it.action()
            it.doIt(chart)
        }
    }
    return chart as? ScatterChart?
}