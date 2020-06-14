package com.angcyo.amap3d

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.amap.api.maps.model.LatLng
import com.angcyo.amap3d.core.toLatLng
import com.angcyo.library.ex.orString
import java.io.File

/**
 * @Description:
 * @Author: shujie <1583534549@qq.com>
 * @CreateDate: 2019/6/27 17:23
 * @UpdateUser:
 * @UpdateDate: 2019/6/27 17:23
 * @UpdateRemark:
 * @Version: 1.0
 * Copyright (c) 2019 Shenzhen O&M Cloud Co., Ltd. All rights reserved.
 */
object MapNaviUtils {
    const val PN_GAODE_MAP = "com.autonavi.minimap" // 高德地图包名
    const val PN_BAIDU_MAP = "com.baidu.BaiduMap" // 百度地图包名
    const val DOWNLOAD_GAODE_MAP = "http://www.autonavi.com/" // 高德地图下载地址
    const val DOWNLOAD_BAIDU_MAP = "http://map.baidu.com/zt/client/index/" // 百度地图下载地址

    /**
     * 检查应用是否安装
     *
     * @return
     */
    val isGdMapInstalled: Boolean get() = isInstallPackage(PN_GAODE_MAP)

    val isBaiduMapInstalled: Boolean get() = isInstallPackage(PN_BAIDU_MAP)

    private fun isInstallPackage(packageName: String): Boolean {
        return File("/data/data/$packageName").exists()
    }

    /**
     * 打开高德地图导航功能
     *
     * @param context
     * @param slat    起点纬度
     * @param slon    起点经度
     * @param sname   起点名称 可不填（0,0，null）
     * @param dlat    终点纬度
     * @param dlon    终点经度
     * @param dname   终点名称 必填
     */
    fun openGaoDeNavi(
        context: Context,
        endLatLng: LatLng,
        endName: String? = null,
        startLatLng: LatLng? = null,
        startName: String? = null
    ) {
        val builder = StringBuilder("amapuri://route/plan?sourceApplication=maxuslife")

        if (startLatLng == null) {
            if (AMapHelper.lastMapLocation != null) {
                builder.append("&sname=我的位置")
                    .append("&slat=").append(AMapHelper.lastMapLocation?.latitude)
                    .append("&slon=").append(AMapHelper.lastMapLocation?.longitude)
            }
        } else {
            builder.append("&sname=").append(startName.orString(""))
                .append("&slat=").append(startLatLng.latitude)
                .append("&slon=").append(startLatLng.longitude)
        }

        builder.append("&dlat=").append(endLatLng.latitude)
            .append("&dlon=").append(endLatLng.longitude)
            .append("&dname=").append(endName.orString(""))
            .append("&dev=0")
            .append("&t=0")
        val uriString = builder.toString()
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setPackage(PN_GAODE_MAP)
        intent.data = Uri.parse(uriString)
        context.startActivity(intent)
    }

    /**
     * 打开百度地图导航功能(默认坐标点是高德地图，需要转换)
     *
     * @param context
     * @param slat    起点纬度
     * @param slon    起点经度
     * @param sname   起点名称 可不填（0,0，null）
     * @param dlat    终点纬度
     * @param dlon    终点经度
     * @param dname   终点名称 必填
     */
    fun openBaiDuNavi(
        context: Context,
        endLatLng: LatLng,
        endName: String? = null,
        startLatLng: LatLng? = null,
        startName: String? = null
    ) {
        val endLatLng2 = gcj02_To_Bd09(endLatLng.latitude, endLatLng.longitude)
        val builder = StringBuilder("baidumap://map/direction?mode=driving&")

        var startLatLngTemp = startLatLng

        if (startLatLng == null) {
            if (AMapHelper.lastMapLocation != null) {
                startLatLngTemp = AMapHelper.lastMapLocation!!.toLatLng()
            }
        }

        val startLatLng2 = if (startLatLngTemp != null) {
            gcj02_To_Bd09(startLatLngTemp.latitude, startLatLngTemp.longitude)
        } else {
            null
        }

        if (startLatLng2 != null) {
            builder.append("origin=latlng:")
                .append(startLatLng2.latitude)
                .append(",")
                .append(startLatLng2.longitude)
                .append("|name:")
                .append(startName.orString(""))
        }

        builder.append("&destination=latlng:")
            .append(endLatLng2.latitude)
            .append(",")
            .append(endLatLng2.longitude)
            .append("|name:")
            .append(endName.orString(""))
        val uriString = builder.toString()
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setPackage(PN_BAIDU_MAP)
        intent.data = Uri.parse(uriString)
        context.startActivity(intent)
    }

    /**
     * 距离友好显示
     */
    fun friendlyDistance(distance: Float, isEnglish: Boolean = false): String {
        val distanceStr: String
        distanceStr = if (distance > 1000) {
            val km = Math.round(distance / 1000)
            km.toString() + if (isEnglish) "km" else "千米"
        } else if (distance > 0) {
            val m = Math.round(distance)
            m.toString() + if (isEnglish) "m" else "米"
        } else {
            0.toString() + if (isEnglish) "m" else "米"
        }
        return distanceStr
    }

    /**
     * 火星坐标系 (GCJ-02) 与百度坐标系 (BD-09) 的转换算法 将 GCJ-02 坐标转换成 BD-09 坐标
     *
     * @param gcj_lat
     * @param gcj_lon
     */
    fun gcj02_To_Bd09(gcj_lat: Double, gcj_lon: Double): LatLng {
        val z =
            Math.sqrt(gcj_lon * gcj_lon + gcj_lat * gcj_lat) + 0.00002 * Math.sin(gcj_lat * Math.PI)
        val theta =
            Math.atan2(gcj_lat, gcj_lon) + 0.000003 * Math.cos(gcj_lon * Math.PI)
        val bd_lon = z * Math.cos(theta) + 0.0065
        val bd_lat = z * Math.sin(theta) + 0.006
        return LatLng(bd_lat, bd_lon)
    }

    /**
     * * 火星坐标系 (GCJ-02) 与百度坐标系 (BD-09) 的转换算法 * * 将 BD-09 坐标转换成GCJ-02 坐标
     * @param bd_lat
     * @param bd_lon
     * @return
     */
    fun bd09_To_Gcj02(bd_lat: Double, bd_lon: Double): LatLng {
        val x = bd_lon - 0.0065
        val y = bd_lat - 0.006
        val z =
            Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * Math.PI)
        val theta =
            Math.atan2(y, x) - 0.000003 * Math.cos(x * Math.PI)
        val gg_lon = z * Math.cos(theta)
        val gg_lat = z * Math.sin(theta)
        return LatLng(gg_lat, gg_lon)
    }
}