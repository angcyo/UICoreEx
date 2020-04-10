package com.angcyo.chart

import android.graphics.drawable.Drawable
import com.angcyo.library.L
import com.github.mikephil.charting.charts.BubbleChart
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.data.BubbleData
import com.github.mikephil.charting.data.BubbleDataSet
import com.github.mikephil.charting.data.BubbleEntry

/**
 * 气泡 图表配置
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/04/10
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
open class BubbleChartConfig : BaseChartConfig<BubbleEntry, BubbleDataSet>() {

    init {
        chartHighlightEnabled = true
    }

    override fun addDataSet(label: String?, action: BubbleDataSet.() -> Unit) {
        if (entryList.isEmpty()) {
            L.w("Entry为空, 请检查是否先调用了[addEntry].")
        }
        BubbleDataSet(entryList, label).apply {
            configDataSet(this)
            action()
            addDataSet(this)
        }
    }

    override fun addEntry(action: BubbleEntry.() -> Unit) {
        entryList.add(BubbleEntry(0f, 0f, 0f).apply(action))
    }

    fun addEntry(
        x: Float = 0f,
        y: Float = 0f,
        size: Float = 0f /*气泡的值, 非半径*/,
        icon: Drawable? = null,
        data: Any? = null,
        action: BubbleEntry.() -> Unit = {}
    ) {
        addEntry {
            this.x = x
            this.y = y
            this.size = size
            this.icon = icon
            this.data = data
            action()
        }
    }

    var bubbleDataConfig: (BubbleData) -> Unit = {}

    override fun onSetChartData(chart: Chart<*>, dataSetList: List<BubbleDataSet>) {
        chart.data = BubbleData(dataSetList).apply {
            bubbleDataConfig(this)
        }
    }
}

fun dslBubbleChart(chart: Chart<*>?, action: BubbleChartConfig.() -> Unit = {}): BubbleChart? {
    chart?.apply {
        BubbleChartConfig().also {
            it.action()
            it.doIt(chart)
        }
    }
    return chart as? BubbleChart?
}