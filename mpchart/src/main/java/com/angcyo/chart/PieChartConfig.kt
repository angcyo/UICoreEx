package com.angcyo.chart

import android.graphics.Color
import android.graphics.drawable.Drawable
import com.angcyo.library.L
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

/**
 * [PieChart]饼状图表配置
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/04/09
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
open class PieChartConfig : BaseChartConfig<PieEntry, PieDataSet>() {

    init {
        chartDrawValues = true
        chartHighlightEnabled = true
        pieUsePercentValues = true
        chartValueTextColor = Color.WHITE
        pieEntryLabelColor = Color.WHITE
    }

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

    /**饼状图只允许有一个[IPieDataSet], 过得的无效*/
    override fun addDataSet(label: String?, action: PieDataSet.() -> Unit) {
        if (entryList.isEmpty()) {
            L.w("Entry为空, 请检查是否先调用了[addEntry].")
        }
        PieDataSet(entryList, label).apply {
            configDataSet(this, action)

            isValueLineVariableLength = true
            valueLinePart1OffsetPercentage = pieValueLinePart1OffsetPercentage
            valueLinePart1Length = pieValueLinePart1Length
            valueLinePart2Length = pieValueLinePart2Length

            xValuePosition = pieValuePositionX
            yValuePosition = pieValuePositionY

            valueLineColor = pieValueLineColor
            valueLineWidth = pieValueLineWidth

            addDataSet(this)
        }
    }

    override fun addEntry(action: PieEntry.() -> Unit) {
        entryList.add(PieEntry(0f).apply(action))
    }

    /**饼状图, X值无效. 请使用Y值*/
    fun addEntry(
        value: Float = 0f,
        icon: Drawable? = null,
        data: Any? = null,
        action: PieEntry.() -> Unit
    ) {
        addEntry {
            this.y = value
            this.icon = icon
            this.data = data
            action()
        }
    }

    var pieDataConfig: (PieData) -> Unit = {}

    override fun onSetChartData(chart: Chart<*>, dataSetList: List<PieDataSet>) {
        chart.data = PieData(dataSetList.first()).apply {
            pieDataConfig(this)
        }
    }
}

fun dslPieChart(chart: Chart<*>?, action: PieChartConfig.() -> Unit = {}): PieChart? {
    chart?.apply {
        PieChartConfig().also {
            it.action()
            it.doIt(chart)
        }
    }
    return chart as? PieChart?
}