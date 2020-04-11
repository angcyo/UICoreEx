package com.angcyo.chart

import android.graphics.drawable.Drawable
import com.angcyo.chart.combined.CombinedDataSet
import com.angcyo.chart.combined.CombinedEntry
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.interfaces.datasets.*

/**
 * 合并图表, Bar + Line + Combined + Candle + Bubble
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/04/10
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
open class CombinedChartConfig : BaseChartConfig<CombinedEntry, CombinedDataSet>() {

    //line 线性图表
    val lineDataSetList = mutableListOf<LineDataSet>()
    var lineEntryList = mutableListOf<Entry>()

    //bar 柱状图表
    val barDataSetList = mutableListOf<BarDataSet>()
    var barEntryList = mutableListOf<BarEntry>()

    //scatter 散列图表
    val scatterDataSetList = mutableListOf<ScatterDataSet>()
    var scatterEntryList = mutableListOf<Entry>()

    //candle 蜡烛图表
    val candleDataSetList = mutableListOf<CandleDataSet>()
    var candleEntryList = mutableListOf<CandleEntry>()

    //bubble 气泡图表
    val bubbleDataSetList = mutableListOf<BubbleDataSet>()
    var bubbleEntryList = mutableListOf<BubbleEntry>()

    fun addLineEntry(
        x: Float = 0f,
        y: Float = 0f,
        icon: Drawable? = null,
        data: Any? = null,
        action: Entry.() -> Unit = {}
    ) {
        lineEntryList.add(Entry().apply {
            this.x = x
            this.y = y
            this.icon = icon
            this.data = data
            action()
        })
    }

    fun addBarEntry(
        x: Float = 0f,
        y: Float = 0f,
        icon: Drawable? = null,
        data: Any? = null,
        action: Entry.() -> Unit = {}
    ) {
        barEntryList.add(BarEntry(0f, 0f).apply {
            this.x = x
            this.y = y
            this.icon = icon
            this.data = data
            action()
        })
    }

    fun addScatterEntry(
        x: Float = 0f,
        y: Float = 0f,
        icon: Drawable? = null,
        data: Any? = null,
        action: Entry.() -> Unit = {}
    ) {
        scatterEntryList.add(Entry().apply {
            this.x = x
            this.y = y
            this.icon = icon
            this.data = data
            action()
        })
    }

    fun addCandleEntry(
        x: Float = 0f,
        shadowH: Float = 0f /*蜡烛 底线顶部值*/,
        shadowL: Float = 0f/*蜡烛 底线底部值*/,
        open: Float = 0f,
        close: Float = 0f,
        icon: Drawable? = null,
        data: Any? = null,
        action: CandleEntry.() -> Unit = {}
    ) {
        candleEntryList.add(CandleEntry(0f, 0f, 0f, 0f, 0f).apply {
            this.x = x
            y = (shadowH + shadowL) / 2f
            high = shadowH
            low = shadowL
            this.open = open
            this.close = close
            this.icon = icon
            this.data = data
            action()
        })
    }

    fun addBubbleEntry(
        x: Float = 0f,
        y: Float = 0f,
        size: Float = 0f /*气泡的值, 非半径*/,
        icon: Drawable? = null,
        data: Any? = null,
        action: BubbleEntry.() -> Unit = {}
    ) {
        bubbleEntryList.add(BubbleEntry(0f, 0f, 0f).apply {
            this.x = x
            this.y = y
            this.size = size
            this.icon = icon
            this.data = data
            action()
        })
    }

    fun addLineDataSet(label: String? = null, action: LineDataSet.() -> Unit = {}) {
        lineDataSetList.add(LineDataSet(lineEntryList, label).apply {
            configDataSet(this)
            action()
        })
        lineEntryList = mutableListOf()
    }

    fun addBarDataSet(label: String? = null, action: BarDataSet.() -> Unit = {}) {
        barDataSetList.add(BarDataSet(barEntryList, label).apply {
            configDataSet(this)
            action()
        })
        barEntryList = mutableListOf()
    }

    fun addScatterDataSet(label: String? = null, action: ScatterDataSet.() -> Unit = {}) {
        scatterDataSetList.add(ScatterDataSet(scatterEntryList, label).apply {
            configDataSet(this)
            action()
        })
        scatterEntryList = mutableListOf()
    }

    fun addCandleDataSet(label: String? = null, action: CandleDataSet.() -> Unit = {}) {
        candleDataSetList.add(CandleDataSet(candleEntryList, label).apply {
            configDataSet(this)
            action()
        })
        candleEntryList = mutableListOf()
    }

    fun addBubbleDataSet(label: String? = null, action: BubbleDataSet.() -> Unit = {}) {
        bubbleDataSetList.add(BubbleDataSet(bubbleEntryList, label).apply {
            configDataSet(this)
            action()
        })
        bubbleEntryList = mutableListOf()
    }

    @Deprecated("不能使用此方法")
    override fun addDataSet(label: String?, action: CombinedDataSet.() -> Unit) {

    }

    @Deprecated("不能使用此方法")
    override fun addEntry(action: CombinedEntry.() -> Unit) {
        //entryList.add(CombinedEntry().apply(action))
    }

    @Deprecated("不能使用此方法")
    override fun addEntry(
        x: Float,
        y: Float,
        icon: Drawable?,
        data: Any?,
        action: CombinedEntry.() -> Unit
    ) {
        //super.addEntry(x, y, icon, data)
    }

    override fun onSetChartData(chart: Chart<*>, dataSetList: List<CombinedDataSet>) {
        //no op
    }

    var scatterDataConfig: (ScatterData) -> Unit = {}
    var lineDataConfig: (LineData) -> Unit = {}
    var barDataConfig: (BarData) -> Unit = {}
    var candleDataConfig: (CandleData) -> Unit = {}
    var bubbleDataConfig: (BubbleData) -> Unit = {}

    override fun doIt(chart: Chart<*>) {
        super.doIt(chart)

        if (lineEntryList.isNotEmpty()) {
            addLineDataSet()
        }
        if (barEntryList.isNotEmpty()) {
            addBarDataSet()
        }
        if (scatterEntryList.isNotEmpty()) {
            addScatterDataSet()
        }
        if (candleEntryList.isNotEmpty()) {
            addCandleDataSet()
        }
        if (bubbleEntryList.isNotEmpty()) {
            addBubbleDataSet()
        }

        val isEmpty = lineDataSetList.isEmpty() &&
                barDataSetList.isEmpty() &&
                scatterDataSetList.isEmpty() &&
                candleDataSetList.isEmpty() &&
                bubbleDataSetList.isEmpty()

        if (isEmpty) {
            chart.data = null
        } else {
            chart.data = CombinedData().apply {
                //line 线性图表
                if (lineDataSetList.isNotEmpty()) {
                    setData(LineData(lineDataSetList as List<ILineDataSet>).apply(lineDataConfig))
                }

                //bar 柱状图表
                if (barDataSetList.isNotEmpty()) {
                    setData(BarData(barDataSetList as List<IBarDataSet>).apply(barDataConfig))
                }

                //scatter 散列图表
                if (scatterDataSetList.isNotEmpty()) {
                    setData(
                        ScatterData(scatterDataSetList as List<IScatterDataSet>).apply(
                            scatterDataConfig
                        )
                    )
                }

                //candle 蜡烛图表
                if (candleDataSetList.isNotEmpty()) {
                    setData(
                        CandleData(candleDataSetList as List<ICandleDataSet>).apply(
                            candleDataConfig
                        )
                    )
                }

                //bubble 气泡图表
                if (bubbleDataSetList.isNotEmpty()) {
                    setData(
                        BubbleData(bubbleDataSetList as List<IBubbleDataSet>).apply(
                            bubbleDataConfig
                        )
                    )
                }

            }
        }
    }
}

fun dslCombinedChart(
    chart: Chart<*>?,
    action: CombinedChartConfig.() -> Unit = {}
): CombinedChart? {
    chart?.apply {
        CombinedChartConfig().also {
            it.action()
            it.doIt(chart)
        }
    }
    return chart as? CombinedChart?
}