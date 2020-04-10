package com.angcyo.chart.combined

import com.github.mikephil.charting.data.DataSet

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/04/10
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class CombinedDataSet : DataSet<CombinedEntry>(emptyList(), null) {
    override fun copy(): DataSet<CombinedEntry> {
        return CombinedDataSet()
    }
}