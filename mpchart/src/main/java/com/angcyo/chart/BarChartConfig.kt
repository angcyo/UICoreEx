package com.angcyo.chart

import android.graphics.Color
import android.graphics.drawable.Drawable
import com.angcyo.library.L
import com.angcyo.library.ex._color
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet

/**
 * [BarChart]配置项
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/04/07
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
open class BarChartConfig : BaseChartConfig() {

    val dataSetList = mutableListOf<BarDataSet>()

    private var lastEntryList = mutableListOf<BarEntry>()

    /**线上绘制值*/
    var barDrawValues: Boolean = false

    /**触摸时, 是否高亮*/
    var barHighlightEnabled: Boolean = false

    /**触摸拖动时, 高亮*/
    var barHighlightPerDragEnabled: Boolean = true

    /**...*/
    var barHighLightPerTapEnabled: Boolean = true
    var barHighlightColor = Color.rgb(255, 187, 115)
    var barDataSetColor = Color.rgb(255, 187, 115)
    var barValueTextColor = _color(R.color.text_general_color)

    /**激活绘制圆*/
    var barDrawCircleEnable: Boolean = true

    /**圆内的hole*/
    var barDrawCircleHole: Boolean = true
    var barDrawIcons: Boolean = true

    /**填充绘制*/
    var barDrawFilled: Boolean = false

    /**线的显示样式*/
    var barMode = LineDataSet.Mode.LINEAR

    /**数据集是否可见*/
    var barVisible = true

    //<editor-fold desc="Entry数据">

    /**添加一根线*/
    fun addBarDataSet(
        entryList: List<BarEntry>,
        label: String? = null,
        action: BarDataSet.() -> Unit = {}
    ) {
        BarDataSet(entryList, label).apply {
            setDrawIcons(barDrawIcons)
            isHighlightEnabled = barHighlightEnabled
            //高亮使用蚂蚁线
            //enableDashedHighlightLine()

            valueTextColor = barValueTextColor

            highLightColor = barHighlightColor
            color = barDataSetColor
            //setFillFormatter { dataSet, dataProvider ->  }
//            setDrawCircleHole(barDrawCircleHole)
//            setDrawValues(barDrawValues)
//            setDrawCircles(barDrawCircleEnable)
//
//            setDrawFilled(barDrawFilled)
//            fillAlpha
//            fillColor
//            fillDrawable
//
//            mode = barMode

            // customize legend entry
            formLineWidth
            formLineDashEffect
            formSize

            isVisible = barVisible

            action()
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
                chart.data = BarData(dataSetList as List<IBarDataSet>)
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