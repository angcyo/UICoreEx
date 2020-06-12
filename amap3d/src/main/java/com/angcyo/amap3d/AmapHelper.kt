package com.angcyo.amap3d

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.ActivityCompat
import com.amap.api.maps.AMapException
import com.amap.api.maps.AMapUtils
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.NaviPara
import com.amap.api.maps.offlinemap.OfflineMapActivity
import com.angcyo.library.ex.havePermission
import com.angcyo.library.utils.FileUtils


/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/06/11
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
object AmapHelper {

    //如果设置了target > 28，需要增加这个权限，否则不会弹出"始终允许"这个选择框
    const val BACK_LOCATION_PERMISSION = "android.permission.ACCESS_BACKGROUND_LOCATION"

    val LOCATION_PERMISSION = listOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    val LOCATION_PERMISSION_BACK = listOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        BACK_LOCATION_PERMISSION
    )

    //需要的权限列表
    fun permissions(backLocation: Boolean = false): List<String> {
        return if (backLocation && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            LOCATION_PERMISSION_BACK
        } else {
            LOCATION_PERMISSION
        }
    }

    /**是否有定位权限
     * [backLocation] 是否需要后台定位权限*/
    fun havePermissions(context: Context, backLocation: Boolean = false) = context.havePermission(
        permissions(backLocation)
    )

    /**如果已经有权限, 则返回. 否则请求定位权限
     * [backLocation] 是否需要后台定位权限*/
    fun requestPermissions(
        context: Context,
        backLocation: Boolean = false,
        requestCode: Int = 909
    ): Boolean {
        return if (havePermissions(context, backLocation)) {
            true
        } else {
            if (context is Activity) {
                ActivityCompat.requestPermissions(
                    context,
                    permissions(backLocation).toTypedArray(),
                    requestCode
                )
            }
            false
        }
    }

    /**https://lbs.amap.com/api/android-sdk/guide/create-map/offline-map*/
    fun initOffline() {
        /*
         * 设置离线地图存储目录，在下载离线地图或初始化地图设置;
         * 使用过程中可自行设置, 若自行设置了离线地图存储的路径，
         * 则需要在离线地图下载和使用地图页面都进行路径设置
         * */
        //Demo中为了其他界面可以使用下载的离线地图，使用默认位置存储，屏蔽了自定义设置

        //需要在地图onCreate之前调用
        MapsInitializer.sdcardDir =
            FileUtils.appRootExternalFolder(folder = "amap_offline")?.absolutePath

//        //构造OfflineMapManager对象
//        val amapManager: OfflineMapManager = OfflineMapManager(this, this);
//        //按照citycode下载
//        amapManager.downloadByCityCode(citycode)
//        //按照cityname下载
//        amapManager.downloadByCityName(cityname)

        //amapManager.pause()
        //amapManager.stop()

        //amapManager.remove(city);
    }

    /**启动离线地图下载组件*/
    fun startOfflineMap(context: Context) {
        //在Activity页面调用start Activity 启动离线地图组件
        context.startActivity(Intent(context, OfflineMapActivity::class.java))
    }

    /**调用外部导航到指定位置*/
    fun navTo(context: Context, target: LatLng) {
        // 构造导航参数
        val naviPara = NaviPara()
        // 设置终点位置
        naviPara.targetPoint = target
        // 设置导航策略，这里是避免拥堵
        naviPara.naviStyle = AMapUtils.DRIVING_AVOID_CONGESTION
        try {
            // 调起高德地图导航
            AMapUtils.openAMapNavi(naviPara, context)
        } catch (e: AMapException) {
            // 如果没安装会进入异常，调起下载页面
            AMapUtils.getLatestAMapApp(context)
        }
    }
}