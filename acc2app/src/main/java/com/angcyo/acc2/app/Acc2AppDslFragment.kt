package com.angcyo.acc2.app

import android.os.Bundle
import com.angcyo.core.fragment.BaseDslFragment

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/07/27
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
open class Acc2AppDslFragment : BaseDslFragment() {

    override fun onFragmentShow(bundle: Bundle?) {
        super.onFragmentShow(bundle)
        //MobclickAgent.onPageStart(fragmentTitle.orString(this.className()))
    }

    override fun onFragmentHide() {
        super.onFragmentHide()
        //MobclickAgent.onPageEnd(fragmentTitle.orString(this.className()))
    }
}