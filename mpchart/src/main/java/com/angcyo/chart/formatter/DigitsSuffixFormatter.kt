package com.angcyo.chart.formatter

import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.DecimalFormat

/**
 * 浮点数 数值格式化, 带后缀的[ValueFormatter]
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/04/09
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
open class DigitsSuffixFormatter(
    val digits: Int = 1 /*小数点后几位*/,
    var suffix: String = "" /*额外的后缀*/
) : ValueFormatter() {

    val decimalFormat: DecimalFormat

    init {
        val b = StringBuffer()
        for (i in 0 until digits) {
            if (i == 0) b.append(".")
            b.append("0")
        }

        decimalFormat = DecimalFormat("###,###,###,##0$b")
    }

    override fun getFormattedValue(value: Float): String {
        return decimalFormat.format(value.toDouble()) + suffix
    }

    override fun getAxisLabel(value: Float, axis: AxisBase): String {
        return when {
            axis is XAxis -> decimalFormat.format(value.toDouble())
            value != 0f -> decimalFormat.format(value.toDouble()) + suffix
            else -> decimalFormat.format(value.toDouble())
        }
    }
}