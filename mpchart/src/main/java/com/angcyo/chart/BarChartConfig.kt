package com.angcyo.chart

import com.angcyo.library.L
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry

/**
 * [BarChart]柱状图表配置
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/04/07
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
open class BarChartConfig : BaseChartConfig<BarEntry, BarDataSet>() {

    //<editor-fold desc="Entry数据">

    /**添加一根Bar, 统一对每个DataSet设置样式, 需要单独配置, 请单独覆盖*/
    override fun addDataSet(label: String?, action: BarDataSet.() -> Unit) {
        if (entryList.isEmpty()) {
            L.w("Entry为空, 请检查是否先调用了[addEntry].")
        }
        BarDataSet(entryList, label).apply {
            configDataSet(this, action)
            addDataSet(this)
        }
    }

    override fun addEntry(action: BarEntry.() -> Unit) {
        entryList.add(BarEntry(0f, 0f).apply(action))
    }

    //</editor-fold desc="Entry数据">

    var barDataConfig: (BarData) -> Unit = {
        it.barWidth
        //it.groupBars()
    }

    /**开启分组[groupBars], 受[dataSet]size的影响*/
    var barGroupFromX: Float = Float.NaN

    /**每一组, 之间的间隙. 1个单位宽度的倍数*/
    var barGroupSpace: Float = 0.5f

    /**每一组内, Bar之间的间隙. 1个单位宽度的倍数*/
    var barGroupBarSpace: Float = 0.1f

    override fun onSetChartData(chart: Chart<*>, dataSetList: List<BarDataSet>) {
        chart.data = BarData(dataSetList).apply {
            /*[barWidth] 可以理解为, chartDataSetWidth * 1个单位值 */
            barWidth = chartDataSetWidth

            if (!barGroupFromX.isNaN()) {
                groupBars(barGroupFromX, barGroupSpace, barGroupBarSpace)
            }

            barDataConfig(this)
        }
    }
}

fun dslBarChart(chart: Chart<*>?, action: BarChartConfig.() -> Unit = {}): BarChart? {
    chart?.apply {
        BarChartConfig().also {
            if (chart is HorizontalBarChart) {
                it.chartXAxisEnable = true
                it.chartLeftAxisEnable = false
                it.chartRightAxisEnable = true
                it.chartXAxisDrawGridLines = false
                it.chartLeftAxisDrawGridLines = false
                it.chartRightAxisDrawGridLines = false
            }
            it.action()
            it.doIt(chart)
        }
    }
    return chart as? BarChart?
}