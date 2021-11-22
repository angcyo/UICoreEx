package com.angcyo.amap3d

import com.amap.api.maps.AMapUtils
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/22
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
object LatLngUntil {
}

//https://a.amap.com/lbs/static/unzip/Android_Map_Doc/index.html
// latitude - 地点的纬度，在-90 与90 之间的double 型数值。
// longitude - 地点的经度，在-180 与180 之间的double 型数值。
//深圳坐标
//114.085947,22.547
//经度        纬度

/**
 * latitude - 地点的纬度，在-90 与90 之间的double 型数值。
 * longitude - 地点的经度，在-180 与180 之间的double 型数值。
 * isCheck - 是否需要检查经纬度的合法性，建议填写true 如果设为true，传入的经纬度不合理，会打印错误日志进行提示，然后转换为接近的合理的经纬度。
 *
 * https://a.amap.com/lbs/static/unzip/Android_Map_Doc/index.html
 * */
fun latlng(latitude: Double? /*纬度*/, longitude: Double? /*经度*/): LatLng =
    LatLng(latitude ?: 0.0, longitude ?: 0.0, true)

/**计算2个点之间的距离, 单位米
返回两点间的距离，单位米。*/
fun LatLng.distance(endLatLng: LatLng): Float = AMapUtils.calculateLineDistance(this, endLatLng)

/**判断多边形是否包含指定的点位*/
fun List<LatLng>.containLatLng(latLng: LatLng): Boolean {
    val bounds = LatLngBounds.builder().apply {
        forEach {
            include(it)
        }
    }.build()
    return bounds.contains(latLng)
}