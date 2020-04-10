package com.angcyo.chart

import android.graphics.drawable.Drawable
import com.angcyo.library.L
import com.github.mikephil.charting.charts.CandleStickChart
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.data.CandleData
import com.github.mikephil.charting.data.CandleDataSet
import com.github.mikephil.charting.data.CandleEntry

/**
 * 蜡烛 图表配置
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/04/10
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
open class CandleStickChartConfig : BaseChartConfig<CandleEntry, CandleDataSet>() {

    override fun addDataSet(label: String?, action: CandleDataSet.() -> Unit) {
        if (entryList.isEmpty()) {
            L.w("Entry为空, 请检查是否先调用了[addEntry].")
        }
        CandleDataSet(entryList, label).apply {
            configDataSet(this)
            action()
            addDataSet(this)
        }
    }

    override fun addEntry(action: CandleEntry.() -> Unit) {
        entryList.add(CandleEntry(0f, 0f, 0f, 0f, 0f).apply(action))
    }

    fun addEntry(
        x: Float = 0f,
        shadowH: Float = 0f /*蜡烛 底线顶部值*/,
        shadowL: Float = 0f/*蜡烛 底线底部值*/,
        open: Float = 0f,
        close: Float = 0f,
        icon: Drawable? = null,
        data: Any? = null,
        action: CandleEntry.() -> Unit = {}
    ) {
        addEntry {
            this.x = x
            y = (shadowH + shadowL) / 2f
            high = shadowH
            low = shadowL
            this.open = open
            this.close = close
            this.icon = icon
            this.data = data
            action()
        }
    }

    var candleStickDataConfig: (CandleData) -> Unit = {}

    override fun onSetChartData(chart: Chart<*>, dataSetList: List<CandleDataSet>) {
        chart.data = CandleData(dataSetList).apply {
            candleStickDataConfig(this)
        }
    }
}

fun dslCandleStickChart(
    chart: Chart<*>?,
    action: CandleStickChartConfig.() -> Unit = {}
): CandleStickChart? {
    chart?.apply {
        CandleStickChartConfig().also {
            it.action()
            it.doIt(chart)
        }
    }
    return chart as? CandleStickChart?
}