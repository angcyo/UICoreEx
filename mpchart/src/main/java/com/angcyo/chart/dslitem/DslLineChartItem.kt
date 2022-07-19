package com.angcyo.chart.dslitem

import android.graphics.Color
import com.angcyo.chart.LineChartConfig
import com.angcyo.chart.R
import com.angcyo.chart.dslLineChart
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.style.ITextItem
import com.angcyo.item.style.TextItemConfig
import com.angcyo.library.ex._dimen
import com.angcyo.widget.DslViewHolder
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.LineDataSet

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/19
 */
open class DslLineChartItem : DslAdapterItem(), ITextItem {

    /**数据配置*/
    var itemLineChartConfigAction: ((LineChartConfig) -> Unit)? = null

    /**图表配置*/
    var itemLineChartAction: ((LineChart) -> Unit)? = null

    override var textItemConfig: TextItemConfig = TextItemConfig()

    init {
        itemLayoutId = R.layout.dsl_line_chart_item
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
        dslLineChart(itemHolder.v(R.id.lib_line_chart)) {
            lineMode = LineDataSet.Mode.LINEAR
            chartXAxisLabelTextColor = Color.RED
            chartDesEnabled = true
            chartDesText = ""

            chartExtraOffsetLeft = _dimen(R.dimen.lib_dpi).toFloat()
            chartExtraOffsetBottom = _dimen(R.dimen.lib_dpi).toFloat()

            /*chartDesText = "(h)"

            for (i in 0..3) {
                addEntry(1f + i * Random.nextInt(0, 3), 1f + i * Random.nextInt(0, 3))
                addEntry(2f + i * Random.nextInt(0, 3), 2f + i * Random.nextInt(0, 3))
                addEntry(3f + i * Random.nextInt(0, 3), 3f + i * Random.nextInt(0, 3))
                addDataSet("L$i")
            }

            addXAxisLimitLine(2.5f) {
                lineColor = Color.RED
            }

            addXAxisLimitLine(3.5f, "Limit") {
                lineColor = Color.GREEN
            }*/

            onConfigLineChart(this)
        }?.apply {
            description.setPosition(
                viewPortHandler.contentLeft(),
                viewPortHandler.contentTop()
            )

            //动画
            /*animateXY(600, 600)*/
            itemLineChartAction?.invoke(this)
        }
    }

    open fun onConfigLineChart(config: LineChartConfig) {
        itemLineChartConfigAction?.invoke(config)
    }
}