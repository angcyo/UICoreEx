package com.angcyo.chart

import android.graphics.Color
import android.graphics.drawable.Drawable
import com.angcyo.library.L
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry

/**
 * 雷达 图表配置
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/04/10
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
open class RadarChartConfig : BaseChartConfig<RadarEntry, RadarDataSet>() {

    init {
        chartDrawValues = false
        chartLeftAxisDrawLabels = false
        chartRightAxisDrawLabels = false
        chartXAxisDrawLabels = true
        chartHighlightEnabled = true

        chartDrawVerticalHighlightIndicator = false
        chartDrawHorizontalHighlightIndicator = false
    }

    /**绘制填充*/
    var radarDrawFill = false

    /**填充颜色*/
    var radarFillColor = Color.rgb(140, 234, 255)
    var radarFillAlpha = 25

    /**高亮时, 绘制圆.需要先激活[chartHighlightEnabled]*/
    var radarDrawHighlightCircleEnabled = true

    override fun addDataSet(label: String?, action: RadarDataSet.() -> Unit) {
        if (entryList.isEmpty()) {
            L.w("Entry为空, 请检查是否先调用了[addEntry].")
        }
        RadarDataSet(entryList, label).apply {
            configDataSet(this, action)

            fillColor = radarFillColor
            fillAlpha = radarFillAlpha
            setDrawFilled(radarDrawFill)
            lineWidth = chartDataSetWidth

            isDrawHighlightCircleEnabled = radarDrawHighlightCircleEnabled

            addDataSet(this)
        }
    }

    override fun addEntry(action: RadarEntry.() -> Unit) {
        entryList.add(RadarEntry(0f).apply(action))
    }

    override fun addEntry(x: Float, y: Float, icon: Drawable?, data: Any?) {
        super.addEntry(x, y, icon, data)
    }

    fun addEntry(
        value: Float = 0f,
        icon: Drawable? = null,
        data: Any? = null,
        action: RadarEntry.() -> Unit = {}
    ) {
        addEntry {
            this.y = value
            this.icon = icon
            this.data = data
            action()
        }
    }

    var radarDataConfig: (RadarData) -> Unit = {}

    override fun onSetChartData(chart: Chart<*>, dataSetList: List<RadarDataSet>) {
        chart.data = RadarData(dataSetList).apply {
            radarDataConfig(this)
        }
    }
}

fun dslRadarChart(chart: Chart<*>?, action: RadarChartConfig.() -> Unit = {}): RadarChart? {
    chart?.apply {
        RadarChartConfig().also {
            it.action()
            it.doIt(chart)
        }
    }
    return chart as? RadarChart?
}