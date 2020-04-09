package com.angcyo.chart

import android.graphics.drawable.Drawable
import com.angcyo.library.L
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet

/**
 * [BarChart]配置项
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/04/07
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
open class BarChartConfig : BaseChartConfig<BarEntry, BarDataSet>() {

    //<editor-fold desc="Entry数据">

    /**添加一根Bar, 统一对每个DataSet设置样式, 需要单独配置, 请单独覆盖*/
    fun addBarDataSet(
        entryList: List<BarEntry>,
        label: String? = null,
        action: BarDataSet.() -> Unit = {}
    ) {
        BarDataSet(entryList, label).apply {
            configDataSet(this, action)
            dataSetList.add(this)
        }
    }

    fun addBarDataSet(label: String? = null, action: BarDataSet.() -> Unit = {}) {
        if (lastEntryList.isEmpty()) {
            L.w("Entry为空, 请检查是否先调用了[addBarEntry].")
        }
        addBarDataSet(lastEntryList, label, action)
        lastEntryList = mutableListOf()
    }

    fun addBarEntry(action: BarEntry.() -> Unit) {
        lastEntryList.add(BarEntry(0f, 0f).apply(action))
    }

    fun addBarEntry(x: Float = 0f, y: Float = 0f, icon: Drawable? = null, data: Any? = null) {
        addBarEntry {
            this.x = x
            this.y = y
            this.icon = icon
            this.data = data
        }
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

    override fun doIt(chart: Chart<*>) {
        super.doIt(chart)

        if (chart is BarChart) {
            //configLimit(chart)

            if (lastEntryList.isNotEmpty()) {
                addBarDataSet()
            }

            if (dataSetList.isEmpty()) {
                chart.data = null
            } else {
                chart.data = BarData(dataSetList as List<IBarDataSet>).apply {
                    /*[barWidth] 可以理解为, chartDataSetWidth * 1个单位值 */
                    barWidth = chartDataSetWidth

                    if (!barGroupFromX.isNaN()) {
                        groupBars(barGroupFromX, barGroupSpace, barGroupBarSpace)
                    }

                    barDataConfig(this)
                }
            }
        }
    }
}

fun dslBarChart(chart: Chart<*>?, action: BarChartConfig.() -> Unit = {}): Chart<*>? {
    chart?.apply {
        BarChartConfig().also {
            it.action()
            it.doIt(chart)
        }
    }
    return chart
}