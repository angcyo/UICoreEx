package com.angcyo.amap3d

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.amap.api.maps.AMapException
import com.amap.api.maps.AMapUtils
import com.amap.api.maps.CoordinateConverter
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.NaviPara
import com.amap.api.maps.offlinemap.OfflineMapActivity
import com.amap.api.services.core.AMapException.CODE_AMAP_SUCCESS
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.district.DistrictSearch
import com.amap.api.services.district.DistrictSearchQuery
import com.amap.api.services.geocoder.*
import com.angcyo.amap3d.core.MapLocation
import com.angcyo.library.ex.havePermission
import com.angcyo.library.model.Page
import com.angcyo.library.utils.FileUtils
import java.util.concurrent.CopyOnWriteArraySet


/**
 * http://a.amap.com/lbs/static/unzip/Android_Map_Doc/index.html
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/06/11
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */

typealias LocationObserver = (MapLocation) -> Unit

object AMapHelper {

    /**记录地图定位改变后的最后一次数据*/
    var lastMapLocation: MapLocation? = null
        set(value) {
            field = value
            value?.apply {
                locationObserveList.forEach {
                    it.invoke(this)
                }
            }
        }

    /**定理位置改变监听, 由高德地图驱动.*/
    val locationObserveList = CopyOnWriteArraySet<LocationObserver>()

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

    /**观察地理位置变化*/
    fun observeLocation(observer: LocationObserver) {
        locationObserveList.add(observer)
    }

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

    /** 判断高德地图app是否已经安装 */
    fun haveNavApp(context: Context): Boolean {
        var packageInfo: PackageInfo? = null
        try {
            packageInfo = context.packageManager.getPackageInfo("com.autonavi.minimap", 0)
        } catch (e: PackageManager.NameNotFoundException) {
            packageInfo = null
            e.printStackTrace()
        }
        // 本手机没有安装高德地图app
        return packageInfo != null
    }

    /**将中文地址, 转换成经纬度
     * [locationName] - 查询地址。
     * [city] - 可选值：cityname（中文或中文全拼）、citycode、adcode。如传入null或空字符串则为“全国”，
     * 第一个参数表示地址，
     * 第二个参数表示查询城市，中文或者中文全拼，citycode、adcode，*/
    fun geocodeQuery(context: Context, locationName: String, city: String? = null) {
        val query = GeocodeQuery(locationName, city)
        val search = GeocodeSearch(context)
        search.setOnGeocodeSearchListener(object : GeocodeSearch.OnGeocodeSearchListener {

            /**经纬度 转 中文地址回调*/
            override fun onRegeocodeSearched(regeocodeResult: RegeocodeResult?, resultCode: Int) {
                //根据给定的经纬度和最大结果数返回逆地理编码的结果列表。
                //no op
            }

            /**中文地址 转 经纬度回调*/
            override fun onGeocodeSearched(geocodeResult: GeocodeResult?, resultCode: Int) {
                //根据给定的地理名称和查询城市，返回地理编码的结果列表。
                if (resultCode == CODE_AMAP_SUCCESS) {
                    //返回成功
                    if (geocodeResult?.geocodeAddressList.isNullOrEmpty()) {
                        //无结果返回
                    } else {

                    }
                } else {
                    //返回失败
                }
            }

        })
        // 设置同步地理编码请求
        search.getFromLocationNameAsyn(query)
    }

    /**
     * 将经纬度[point], 转换成 中文地址
     * 第一个参数表示一个Latlng，
     * 第二参数表示范围多少米，
     * 第三个参数表示是火系坐标系还是GPS原生坐标系
     * */
    fun regeocodeQuery(
        context: Context,
        point: LatLonPoint,
        radius: Float = 1000f,
        latLonType: String = GeocodeSearch.AMAP
    ) {
        val query = RegeocodeQuery(point, radius, latLonType)
        //query.poiType
        val search = GeocodeSearch(context)
        search.setOnGeocodeSearchListener(object : GeocodeSearch.OnGeocodeSearchListener {

            /**经纬度 转 中文地址回调*/
            override fun onRegeocodeSearched(regeocodeResult: RegeocodeResult?, resultCode: Int) {
                //根据给定的经纬度和最大结果数返回逆地理编码的结果列表。
                if (resultCode == CODE_AMAP_SUCCESS) {
                    //返回成功
                    if (regeocodeResult?.regeocodeAddress?.formatAddress != null) {
                        //有效地址
                    }
                } else {
                    //返回失败
                }
            }

            /**中文地址 转 经纬度回调*/
            override fun onGeocodeSearched(geocodeResult: GeocodeResult?, resultCode: Int) {
                //no op
            }

        })
        // 设置异步逆地理编码请求
        search.getFromLocationAsyn(query)
    }

    /**行政区划搜索*/
    fun searchDistrict(context: Context, keywords: String = "中国") {
        // 设置行政区划查询监听
        val search = DistrictSearch(context)
        search.setOnDistrictSearchListener {
            if (it?.aMapException?.errorCode == CODE_AMAP_SUCCESS) {
                //请求成功
                it.district?.firstOrNull()?.apply {
                    //字符串拆分规则： 经纬度，经度和纬度之间用","分隔，坐标点之间用";"分隔。例如：116.076498,40.115153;116.076603,40.115071;116.076333,40.115257;116.076498,40.115153。
                    districtBoundary()

                    center
                }
            } else {
                //失败
            }
        }
        // 查询中国的区划
        val query = DistrictSearchQuery()
        query.isShowBoundary = true//返回边界信息
        query.keywords = keywords
        query.pageNum = 0
        query.pageSize = Page.PAGE_SIZE

        search.query = query
        // 异步查询行政区
        search.searchDistrictAsyn()
    }

    /** 根据类型 转换 坐标 */
    fun convert(
        context: Context,
        sourceLatLng: LatLng,
        coord: CoordinateConverter.CoordType = CoordinateConverter.CoordType.BAIDU
    ): LatLng? {
        val converter = CoordinateConverter(context)
        // CoordType.GPS 待转换坐标类型
        converter.from(coord)
        // sourceLatLng待转换坐标点
        converter.coord(sourceLatLng)
        // 执行转换操作
        return converter.convert()
    }
}