package com.angcyo.chart.formatter

import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter

/**
 * 百分比格式化显示的[ValueFormatter]
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/04/09
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
open class PercentFormatter(
    val usePercentValues: Boolean = true,
    var percentSuffix: String = "%"
) : DigitsSuffixFormatter() {

    override fun getFormattedValue(value: Float): String {
        suffix = if (usePercentValues) {
            percentSuffix
        } else {
            ""
        }
        return super.getFormattedValue(value)
    }

    override fun getPieLabel(value: Float, pieEntry: PieEntry?): String {
        return super.getPieLabel(value, pieEntry)
    }
}