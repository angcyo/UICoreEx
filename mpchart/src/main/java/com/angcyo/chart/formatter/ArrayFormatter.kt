package com.angcyo.chart.formatter

import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter

/**
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/04/10
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
open class ArrayFormatter(val values: List<String>) : ValueFormatter() {

    override fun getFormattedValue(value: Float): String {
        return values[(value.toInt() % values.size)]
    }

    override fun getPieLabel(value: Float, pieEntry: PieEntry?): String {
        return super.getPieLabel(value, pieEntry)
    }
}