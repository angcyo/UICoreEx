package com.angcyo.github.dslitem

import com.leochuan.AutoPlaySnapHelper

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/18
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
interface ILoopAdapterItem {

    /**获取轮播图循环间隔时长, 毫秒*/
    fun getLoopInterval(): Int = AutoPlaySnapHelper.TIME_INTERVAL
}