package com.angcyo.amap3d

import com.angcyo.amap3d.core.MapLocation
import com.angcyo.core.lifecycle.LifecycleViewModel
import com.angcyo.viewmodel.vmDataNull

/**
 * 高德地图数据模型
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/24
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class AMapModel : LifecycleViewModel() {

    /**当前的定位数据*/
    val myLocationData = vmDataNull<MapLocation>()

}