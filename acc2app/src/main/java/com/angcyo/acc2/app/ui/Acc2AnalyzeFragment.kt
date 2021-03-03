package com.angcyo.acc2.app.ui

import android.os.Bundle
import com.angcyo.acc2.app.Acc2AppDslFragment
import com.angcyo.acc2.app.dslitem.AppAnalyzeItem

/**
 * 分析界面
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/07/30
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class Acc2AnalyzeFragment : Acc2AppDslFragment() {
    init {
        fragmentTitle = "应用分析"
    }

    override fun initBaseView(savedInstanceState: Bundle?) {
        super.initBaseView(savedInstanceState)

        renderDslAdapter {
            AppAnalyzeItem()()
        }
    }
}