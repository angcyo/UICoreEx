package com.angcyo.amap3d

import android.Manifest
import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.core.app.ActivityCompat
import com.amap.api.maps.MapsInitializer
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

    /**仅请求定位权限
     * [backLocation] 是否需要后台定位权限*/
    fun requestPermissions(
        context: Context,
        backLocation: Boolean = false,
        requestCode: Int = 909
    ) {
        if (context is Activity) {
            ActivityCompat.requestPermissions(
                context,
                permissions(backLocation).toTypedArray(), requestCode
            )
        }
    }

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
    }
}