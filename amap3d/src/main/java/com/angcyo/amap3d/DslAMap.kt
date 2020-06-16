package com.angcyo.amap3d

import android.content.res.AssetManager
import android.view.MotionEvent
import com.amap.api.maps.*
import com.amap.api.maps.model.*
import com.amap.api.services.core.AMapException.CODE_AMAP_SUCCESS
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.core.PoiItem
import com.angcyo.amap3d.core.MapLocation
import com.angcyo.amap3d.core.toLatLng
import com.angcyo.library.L
import com.angcyo.library.ex.*
import com.autonavi.amap.mapcore.Inner_3dMap_location
import java.util.*


/**
 * [AMap] 常用操作封装类
 * https://a.amap.com/lbs/static/unzip/Android_Map_Doc/index.html
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/06/11
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class DslAMap {

    //<editor-fold desc="定位蓝点相关">

    //https://a.amap.com/lbs/static/unzip/Android_Map_Doc/3D/com/amap/api/maps/model/MyLocationStyle.html

    //https://lbs.amap.com/api/android-sdk/guide/create-map/mylocation#location-marker-5-0

    val locationStyle = MyLocationStyle()

    /**
     * [MyLocationStyle.LOCATION_TYPE_SHOW] 只定位一次。
     * [MyLocationStyle.LOCATION_TYPE_LOCATE] 定位一次，且将视角移动到地图中心点。
     * [MyLocationStyle.LOCATION_TYPE_FOLLOW] 连续定位、且将视角移动到地图中心点，定位蓝点跟随设备移动。（默认1秒1次定位）
     * [MyLocationStyle.LOCATION_TYPE_MAP_ROTATE] 连续定位、且将视角移动到地图中心点，地图依照设备方向旋转，定位点会跟随设备移动。（默认1秒1次定位）
     * [MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE] 连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（默认1秒1次定位）默认执行此种模式。
     * 以下三种模式从5.1.0版本开始提供
     * [MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER] 连续定位、蓝点不会移动到地图中心点，定位点依照设备方向旋转，并且蓝点会跟随设备移动。
     * [MyLocationStyle.LOCATION_TYPE_FOLLOW_NO_CENTER] 连续定位、蓝点不会移动到地图中心点，并且蓝点会跟随设备移动。
     * [MyLocationStyle.LOCATION_TYPE_MAP_ROTATE_NO_CENTER] 连续定位、蓝点不会移动到地图中心点，地图依照设备方向旋转，并且蓝点会跟随设备移动。
     */
    var locationType: Int = MyLocationStyle.LOCATION_TYPE_FOLLOW_NO_CENTER

    /**当定位类型[locationType]没有将视角移动到地图中心点时, 是否在首次定位回调后. 移动到地图中心*/
    var locationMoveFirst = true

    /**回到中心位置时, 需要使用的zoom*/
    var locationMoveZoom = 17f

    /**设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
     * 如果传小于1000的任何值将按照1000计算。该方法只会作用在会执行连续定位的工作模式上。*/
    var locationInterval: Long = 2_000

    /**设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。*/
    var locationEnable: Boolean = true

    /**是否显示定位蓝点, 不影响定位回调*/
    var locationVisible: Boolean = true

    /**蓝点图标, null表示默认
     * [BitmapDescriptorFactory.fromAsset]
     * */
    var locationIcon: BitmapDescriptor? = null

    /**设置定位蓝点精度圆圈的边框颜色的方法。*/
    var locationStrokeColor: Int =
        _color(R.color.colorPrimary)//locationStyle.strokeColor

    /**边框的宽度*/
    var locationStrokeWidth: Float = 1 * dp

    /**设置定位蓝点精度圆圈的填充颜色的方法。*/
    var locationFillColor: Int =
        _color(R.color.colorPrimary).alpha(16)  //locationStyle.radiusFillColor

    /**执行定位样式配置, [myLocationType] 支持动态修改, 实时生效*/
    fun doLocationStyle(map: AMap) {
        //初始化定位蓝点样式类locationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);
        //连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。

        locationStyle.interval(locationInterval)
        locationStyle.myLocationType(locationType)

        //定位蓝点图标：
        locationStyle.myLocationIcon(locationIcon)
        locationStyle.showMyLocation(locationVisible)

        //精度圆圈的自定义：
        locationStyle.strokeColor(locationStrokeColor)
        locationStyle.radiusFillColor(locationFillColor)
        locationStyle.strokeWidth(locationStrokeWidth)

        //暂不知
        //locationStyle.anchor(0f, 10f)

        //设置定位蓝点的Style
        map.myLocationStyle = locationStyle

        //设置默认定位按钮是否显示，非必需设置。
        //map.getUiSettings().setMyLocationButtonEnabled(true);
        map.isMyLocationEnabled = locationEnable

        if (locationMoveFirst) {

            map.myLocation?.let {
                val latLng = it.toLatLng()
                L.w("move to first 1:$latLng $locationMoveZoom")
                map.moveTo(LatLng(it.latitude, it.longitude), locationMoveZoom)
            }

            var listener: AMap.OnMyLocationChangeListener? = null
            listener = map.onMyLocationChange {
                if (it.errorCode == 0) {
                    //定位成功
                    //latitude=0.0#longitude=0.0#province=#city=#district=#cityCode=#adCode=#address=#country=#road=
                    //#poiName=#street=#streetNum=#aoiName=#poiid=#floor=
                    //#errorCode=12#errorInfo=缺少定位权限
                    //请到 http://lbs.amap.com/api/android-location-sdk/guide/utilities/errorcode/ 查看错误码说明,错误详细信息:定位权限被禁用,请授予应用定位权限#1201
                    //#locationDetail=定位权限被禁用,请授予应用定位权限#1201#locationType=0
                    //L.e(it)
                    map.removeOnMyLocationChangeListener(listener)
                    if (locationMoveFirst) {
                        val latLng = it.toLatLng()
                        L.w("move to first 2:$latLng $locationMoveZoom")
                        map.moveTo(latLng, locationMoveZoom)
                    }
                }
            }
        }
    }

    //</editor-fold desc="定位蓝点相关">

    //<editor-fold desc="样式相关">

    var customStyleOptions: CustomMapStyleOptions? = null

    /**从assets中加载高德地图style, [onCreate]之后执行*/
    fun loadStyleFromAssets(
        map: AMap,
        assetManager: AssetManager,
        styleFileName: String = "style.data",
        extraStyleFileName: String = "style_extra.data"
    ): CustomMapStyleOptions {
        //卸载之前
        unloadCustomStyle(map)
        CustomMapStyleOptions().apply {
            customStyleOptions = this

            isEnable = true

            //扩展内容，如网格背景色等
            //style_extra.data

            //具体样式配置
            //style.data

            //纹理图片(zip文件), 需要开通权限才有
            //textures.zip

            try {
                assetManager.open(styleFileName).use {
                    setStyleData(it.readBytes())
                }
            } catch (e: Exception) {
                L.e("AMapStyleHelper ", "style.data error:$e")
            }

            try {
                assetManager.open(extraStyleFileName).use {
                    setStyleExtraData(it.readBytes())
                }
            } catch (e: Exception) {
                L.e("AMapStyleHelper ", "style_extra.data error:$e")
            }
        }
        map.setCustomMapStyle(customStyleOptions)
        return customStyleOptions!!
    }

    /**卸载自定义样式*/
    fun unloadCustomStyle(map: AMap) {
        customStyleOptions?.apply {
            isEnable = false
            map.setCustomMapStyle(this)
        }
    }

    /**切换地图图层
     * [AMap.MAP_TYPE_NORMAL] 矢量地图模式,白昼地图（即普通地图）
     * [AMap.MAP_TYPE_SATELLITE] 卫星地图模式
     * [AMap.MAP_TYPE_NIGHT] 夜景地图模式
     * [AMap.MAP_TYPE_NAVI] 导航地图模式
     * */
    fun switchMapType(map: AMap, type: Int = AMap.MAP_TYPE_SATELLITE) {
        //卸载自定义样式
        unloadCustomStyle(map)
        map.mapType = type
    }

    //</editor-fold desc="样式相关">

    //<editor-fold desc="UI相关">

    //https://a.amap.com/lbs/static/unzip/Android_Map_Doc/3D/com/amap/api/maps/UiSettings.html

    /**设置是否显示室内地图，默认不显示。*/
    var showIndoorMap: Boolean = false

    /**设置是否显示3D建筑物，默认显示。*/
    var showBuildings: Boolean = true

    /**设置是否显示底图文字标注，默认显示。*/
    var showMapText: Boolean = true

    /**显示定位按钮*/
    var showMyLocationButton: Boolean = false

    /**显示指南针*/
    var showCompass: Boolean = false

    /**显示比例尺控件*/
    var showScaleControl: Boolean = false

    /**显示放大缩小控件*/
    var showZoomControl: Boolean = false

    /**
     * 放大缩小控件位置
     * [AMapOptions.ZOOM_POSITION_RIGHT_CENTER]
     * [AMapOptions.LOGO_POSITION_BOTTOM_RIGHT]
     * */
    var zoomControlPosition: Int = AMapOptions.LOGO_POSITION_BOTTOM_RIGHT

    /**显示实时路况*/
    var showTraffic: Boolean = false

    /**激活所有手势*/
    var enableAllGestures: Boolean = true

    /**激活旋转手势*/
    var enableRotateGestures: Boolean = true

    /**激活拖拽手势*/
    var enableScrollGestures: Boolean = true

    /**激活倾斜手势*/
    var enableTiltGestures: Boolean = false

    /**激活双指缩放手势*/
    var enableZoomGestures: Boolean = true

    /**激活以中心点进行手势操作*/
    var enableGestureScaleByMapCenter: Boolean = false

    /**
     * [AMapOptions.LOGO_POSITION_BOTTOM_CENTER]
     * [AMapOptions.LOGO_MARGIN_RIGHT]
     * */
    var logoPosition: Int = AMapOptions.LOGO_POSITION_BOTTOM_LEFT

    /**Logo的偏移, 可以通过负数隐藏Logo. 7.4.0测试通过*/
    var logoBottomMargin = undefined_size

    var logoLeftMargin = undefined_size

    /**执行UI配置*/
    fun doUI(map: AMap) {
        map.uiSettings.apply {
            //室内操作控件
            isIndoorSwitchEnabled = showIndoorMap
            isCompassEnabled = showCompass
            isMyLocationButtonEnabled = showMyLocationButton
            isScaleControlsEnabled = showScaleControl
            isZoomControlsEnabled = showZoomControl

            zoomPosition = zoomControlPosition

            isGestureScaleByMapCenter = enableGestureScaleByMapCenter

            if (enableAllGestures) {
                //激活手势
                setAllGesturesEnabled(enableAllGestures)
                isRotateGesturesEnabled = enableRotateGestures
                isScrollGesturesEnabled = enableScrollGestures
                isTiltGesturesEnabled = enableTiltGestures
                isZoomGesturesEnabled = enableZoomGestures
            } else {
                setAllGesturesEnabled(enableAllGestures)
            }

            logoPosition = this@DslAMap.logoPosition

            if (logoBottomMargin != undefined_size) {
                setLogoBottomMargin(logoBottomMargin)
            }
            if (logoLeftMargin != undefined_size) {
                setLogoLeftMargin(logoLeftMargin)
            }

        }
        map.showIndoorMap(showIndoorMap)
        map.showBuildings(showBuildings)
        map.showMapText(showMapText)
        map.isTrafficEnabled = showTraffic
        //map.myTrafficStyle

        //移动到有室内地图的地方,放大级别才可以看见
        //map.moveTo(LatLng(39.91095, 116.37296), 20f)
    }

    //</editor-fold desc="UI相关">

    //<editor-fold desc="操作相关">

    /**移动至当前位置*/
    fun moveToLocation(map: AMap, animDuration: Long = 250 /*动画耗时250毫秒, 小于0表示关闭动画*/) {
        map.moveToLocation(animDuration, locationMoveZoom)
    }

    //</editor-fold desc="操作相关">
}

/**DSL*/
fun TextureMapView.dslAMap(action: DslAMap.() -> Unit): DslAMap {
    val dslAMap = DslAMap()
    dslAMap.action()
    return dslAMap
}

/**DSL*/
fun MapView.dslAMap(action: DslAMap.() -> Unit): DslAMap {
    val dslAMap = DslAMap()
    dslAMap.action()
    return dslAMap
}

//<editor-fold desc="Move方法">

/**放大*/
fun AMap.zoomIn(animDuration: Long = 250) {
    moveTo(CameraUpdateFactory.zoomIn(), animDuration)
}

/**缩小*/
fun AMap.zoomOut(animDuration: Long = 250) {
    moveTo(CameraUpdateFactory.zoomOut(), animDuration)
}

/**移动至当前位置*/
fun AMap.moveToLocation(
    animDuration: Long = 250 /*动画耗时250毫秒, 小于0表示关闭动画*/,
    locationMoveZoom: Float = cameraPosition.zoom
) {
    myLocation?.let {
        moveTo(LatLng(it.latitude, it.longitude), locationMoveZoom, animDuration)
    }
}

fun AMap.moveTo(update: CameraUpdate, animDuration: Long = 250 /*动画耗时250毫秒, 小于0表示关闭动画*/) {
    if (animDuration > 0) {
        animateCamera(update, animDuration, object : AMap.CancelableCallback {
            override fun onFinish() {
                L.d("AMap Camera Finish!")
            }

            override fun onCancel() {
                L.d("AMap Camera Cancel!")
            }
        })
    } else {
        moveCamera(update)
    }
}

//重置最小及最大缩放级别 将恢复最小级别为3，最大级别为20 。

//https://a.amap.com/lbs/static/unzip/Android_Map_Doc/3D/com/amap/api/maps/CameraUpdateFactory.html
fun AMap.moveTo(
    latLng: LatLng,
    zoom: Float = cameraPosition.zoom /*地图放大级别, 数值越大地图放大的越大.*/,
    animDuration: Long = 250 /*动画耗时250毫秒, 小于0表示关闭动画*/
) {
    moveTo(CameraUpdateFactory.newLatLngZoom(latLng, zoom), animDuration)
}

/**移动地图至包含所有指定的点位*/
fun AMap.moveInclude(bounds: LatLngBounds, padding: Int = 50 * dpi, animDuration: Long = 250) {
    moveTo(CameraUpdateFactory.newLatLngBounds(bounds, padding), animDuration)
}

fun AMap.moveInclude(latLngList: List<LatLng>, padding: Int = 50 * dpi, animDuration: Long = 250) {
    // 设置所有maker显示在当前可视区域地图中
    val bounds = LatLngBounds.Builder().run {
        latLngList.forEach {
            include(it)
        }
        build()
    }
    moveInclude(bounds, padding, animDuration)
}

fun AMap.moveInclude(vararg latLng: LatLng, padding: Int = 50 * dpi, animDuration: Long = 250) {
    // 设置所有maker显示在当前可视区域地图中
    val bounds = LatLngBounds.Builder().run {
        latLng.forEach {
            include(it)
        }
        build()
    }
    moveInclude(bounds, padding, animDuration)
}

//</editor-fold desc="Move方法">

//<editor-fold desc="操作方法">

/**限制地图显示范围，地图旋转手势将会失效。*/
fun AMap.boundsLimit(bounds: LatLngBounds) {
    setMapStatusLimits(bounds)
}

//scalePerPixel  //每像素代表多少米

/**判断目标位置[target], 是否在地图可视范围内
 * 西南坐标 - 东北坐标
 * */
fun AMap.isInVisibleRegion(target: LatLng): Boolean {
    return projection?.visibleRegion?.run {
        latLngBounds.contains(target)
    } ?: false
}

/**判断[target], 是否在[bounds]内*/
fun LatLng.isInBounds(bounds: List<LatLng>): Boolean {
    val latLngBounds = LatLngBounds.Builder().run {
        bounds.forEach {
            include(it)
        }
        build()
    }
    return latLngBounds.contains(this)
}

/**返回两点间的距离，单位米。*/
fun LatLng.distance(endLatLng: LatLng): Float = AMapUtils.calculateLineDistance(this, endLatLng)


/** 在经度/纬度 方向偏移指定的距离 (米)
 * latitude 纬度, 决定上下距离
 * longitude 经度, 决定左右距离
 * [latitudeDistance] 上下偏移的距离 (米)
 * [longitudeDistance] 左右偏移的距离 (米)
 *
 * https://blog.csdn.net/u012539364/java/article/details/74059679
 * */
fun LatLng.offsetDistance(latitudeDistance: Float = 0f, longitudeDistance: Float = 0f): LatLng =
    LatLng(
        latitude + latitudeDistance * 0.000008983152841195214,
        longitude + longitudeDistance * 0.000009405717451407729
    )

fun LatLng.toLatLonPoint(): LatLonPoint = LatLonPoint(latitude, longitude)

fun LatLonPoint.toLatLon(): LatLng = LatLng(latitude, longitude)

/**屏幕中心点的经纬度坐标。*/
fun AMap.getMapCenterPosition(): LatLng = cameraPosition.target

/**搞得搜索包内的搜索API调用成功的返回码*/
fun Int.isSearchSuccess() = this == CODE_AMAP_SUCCESS

/**获取[PoiItem]对应的详细地址信息*/
fun PoiItem.getAddress() = "${provinceName.orString(
    ""
)}${cityName.orString(
    ""
)}${adName.orString(
    ""
)}${businessArea.orString(
    ""
)}${snippet.orString("")}"

fun AMap.myLatLng() = myLocation?.toLatLng()

fun TextureMapView.myLatLng() = map.myLatLng()

fun MapView.myLatLng() = map.myLatLng()

//</editor-fold desc="操作方法">

//<editor-fold desc="事件回调相关">

/**地图定位改变后的回调, 如果使用的是持续定位方式, 则会持续回调.
 * [ON_PAUSE] 之后地图依旧会回调定位信息*/
fun AMap.onMyLocationChange(action: (location: Inner_3dMap_location) -> Unit = {}): AMap.OnMyLocationChangeListener {
    //从location对象中获取经纬度信息，地址描述信息，建议拿到位置之后调用逆地理编码接口获取（获取地址描述数据章节有介绍）
    val listener = AMap.OnMyLocationChangeListener {
        //Inner_3dMap_location
        //latitude=22.568021#longitude=114.066152
        //#province=广东省#city=深圳市#district=福田区#cityCode=0755#adCode=440304
        //#address=广东省深圳市福田区梅林路17号靠近彩梅立交#country=中国#road=梅林路
        //#poiName=#street=#streetNum=#aoiName=#poiid=#floor=#errorCode=0#errorInfo=success
        //#locationDetail=#id:YaDhoZ2U5M2diZjMzZjVlaDk3MmhoZzFjMTIyMzY3LFhCNFZYTnE5NktZREFPSnhyNGVuc3JCVg==
        //#csid:d83314dc0ed646398530b72d732e55a1#locationType=2
        //L.e(it)
        //L.w(cameraPosition)
        //L.w(map.myLocation)

        if (it is Inner_3dMap_location) {
            if (it.errorCode == 0) {
                AMapHelper.lastMapLocation = MapLocation.from(it)
            }
        }

        action(it as Inner_3dMap_location)
    }
    addOnMyLocationChangeListener(listener)
    return listener
}

/**地图加载回调*/
fun AMap.onMapLoadedListener(action: () -> Unit = {}): AMap.OnMapLoadedListener {
    val listener = AMap.OnMapLoadedListener {
        L.w("AMapLoaded...")
        action()
    }
    addOnMapLoadedListener(listener)
    return listener
}

/**地图点击事件
 * [latitude] 纬度
 * [longitude] 经度
 * */
fun AMap.onMapClickListener(action: (LatLng) -> Unit = {}): AMap.OnMapClickListener {
    val listener = AMap.OnMapClickListener {
        action(it)
    }
    addOnMapClickListener(listener)
    return listener
}

/**地图长按事件
 * [latitude] 纬度
 * [longitude] 经度
 * */
fun AMap.onMapLongClickListener(action: (LatLng) -> Unit = {}): AMap.OnMapLongClickListener {
    val listener = AMap.OnMapLongClickListener {
        action(it)
    }
    addOnMapLongClickListener(listener)
    return listener
}

/**地图Camera改变事件
 * [CameraPosition] 包含了倾斜角度, 缩放系数, 方向角度(从正北向逆时针方向计算，从0 度到360 度。), LatLng位置
 * */
fun AMap.onCameraChangeListener(
    change: (CameraPosition) -> Unit = {},
    finish: (CameraPosition) -> Unit = {}
): AMap.OnCameraChangeListener {
    val listener = object : AMap.OnCameraChangeListener {
        override fun onCameraChangeFinish(position: CameraPosition) {
            finish(position)
        }

        override fun onCameraChange(position: CameraPosition) {
            change(position)
        }
    }
    addOnCameraChangeListener(listener)
    return listener
}

fun AMap.onMapTouchListener(action: (MotionEvent) -> Unit = {}): AMap.OnMapTouchListener {
    val listener = AMap.OnMapTouchListener {
        action(it)
    }
    addOnMapTouchListener(listener)
    return listener
}

/**地图上[Poi]的点击事件
 * 点击地图Poi点时，该兴趣点的描述信息 Poi 是指底图上的一个自带Poi点。
 * https://a.amap.com/lbs/static/unzip/Android_Map_Doc/3D/com/amap/api/maps/model/Poi.html
 * */
fun AMap.onPOIClickListener(action: (Poi) -> Unit = {}): AMap.OnPOIClickListener {
    val listener = AMap.OnPOIClickListener {
        action(it)
    }
    addOnPOIClickListener(listener)
    return listener
}

/**
 * true 返回true表示该点击事件已被处理，不再往下传递（如底图点击不会被触发），返回false则继续往下传递。
 * https://a.amap.com/lbs/static/unzip/Android_Map_Doc/3D/com/amap/api/maps/model/Marker.html
 * */
fun AMap.onMarkerClickListener(action: (Marker) -> Boolean = { false }): AMap.OnMarkerClickListener {
    val listener = AMap.OnMarkerClickListener {
        action(it)
    }
    addOnMarkerClickListener(listener)
    return listener
}

/**长按[Marker]可以进行拖拽*/
fun AMap.onMarkerDragListener(
    start: (Marker) -> Unit = { },
    move: (Marker) -> Unit = { },
    end: (Marker) -> Unit = { }
): AMap.OnMarkerDragListener {
    val listener = object : AMap.OnMarkerDragListener {
        override fun onMarkerDragEnd(marker: Marker) {
            end(marker)
        }

        override fun onMarkerDragStart(marker: Marker) {
            start(marker)
        }

        override fun onMarkerDrag(marker: Marker) {
            move(marker)
        }
    }
    addOnMarkerDragListener(listener)
    return listener
}

fun AMap.onInfoWindowClickListener(action: (Marker) -> Boolean = { false }): AMap.OnInfoWindowClickListener {
    val listener = AMap.OnInfoWindowClickListener {
        action(it)
    }
    addOnInfoWindowClickListener(listener)
    return listener
}

//</editor-fold desc="事件回调相关">

//<editor-fold desc="绘制相关">

/**添加标记[Marker]
 * https://a.amap.com/lbs/static/unzip/Android_Map_Doc/3D/com/amap/api/maps/model/MarkerOptions.html
 * */
fun AMap.addMarker(action: MarkerOptions.() -> Unit): Marker {
    val options = markerOptions(action)
    val marker = addMarker(options)
    marker.isVisible = true
    return marker
}

/**添加一个屏幕位置的[Marker]*/
fun AMap.addScreenMarker(x: Int, y: Int, action: MarkerOptions.() -> Unit): Marker {
    val options = MarkerOptions()
    //设置Marker覆盖物的锚点比例。锚点是marker 图标接触地图平面的点。图标的左顶点为（0,0）点，右底点为（1,1）点。
    options.anchor(0.5f, 0.5f)
    options.action()
    val marker = addMarker(options)
    marker.isVisible = true
    marker.setPositionByPixels(x, y)
    return marker
}

/**添加一个屏幕中心的[Marker]*/
fun AMap.addScreenCenterMarker(action: MarkerOptions.() -> Unit): Marker {
    val centerPoint = projection.toScreenLocation(cameraPosition.target)
    return addScreenMarker(centerPoint.x, centerPoint.y, action)
}

/**在地图上添一组图片标记（marker）对象，并设置是否改变地图状态以至于所有的marker对象都在当前地图可视区域范围内显示。*/
fun AMap.addMarkers(
    options: ArrayList<MarkerOptions>,
    moveToCenter: Boolean = true
): MutableList<Marker> {
    val marker = addMarkers(options, moveToCenter)
    return marker
}

/**添加折线[Polyline]
 * https://a.amap.com/lbs/static/unzip/Android_Map_Doc/3D/com/amap/api/maps/model/PolylineOptions.html
 * */
fun AMap.addPolyline(action: PolylineOptions.() -> Unit): Polyline {
    val options = PolylineOptions()
    options.action()
    val polyline = addPolyline(options)
    polyline.isVisible = true
    return polyline
}

/**添加弧形[Arc]
 * https://a.amap.com/lbs/static/unzip/Android_Map_Doc/3D/com/amap/api/maps/model/Arc.html
 * */
fun AMap.addArc(action: ArcOptions.() -> Unit): Arc {
    val options = ArcOptions()
    options.action()
    val arc = addArc(options)
    arc.isVisible = true
    return arc
}

/**添加圆[Circle]
 * https://a.amap.com/lbs/static/unzip/Android_Map_Doc/3D/com/amap/api/maps/model/Circle.html
 * */
fun AMap.addCircle(action: CircleOptions.() -> Unit): Circle {
    val options = CircleOptions()
    options.action()
    val circle = addCircle(options)
    circle.isVisible = true
    return circle
}

/**添加多边形[Polygon]
 * https://a.amap.com/lbs/static/unzip/Android_Map_Doc/3D/com/amap/api/maps/model/Polygon.html
 * */
fun AMap.addPolygon(action: PolygonOptions.() -> Unit): Polygon {
    val options = PolygonOptions()
    options.action()
    val polygon = addPolygon(options)
    polygon.isVisible = true
    return polygon
}

/**添加文本标记[Text]
 * https://a.amap.com/lbs/static/unzip/Android_Map_Doc/3D/com/amap/api/maps/model/Text.html
 * */
fun AMap.addText(action: TextOptions.() -> Unit): Text {
    val options = TextOptions()
    options.action()
    val text = addText(options)
    text.isVisible = true
    return text
}

/**https://a.amap.com/lbs/static/unzip/Android_Map_Doc/3D/com/amap/api/maps/model/NavigateArrowOptions.html
 *
 * 绘制导向箭头, 一头有个三角形, 一头是平角
 * https://www.jianshu.com/p/c577cc2c166b
 * */
fun AMap.addNavigateArrow(action: NavigateArrowOptions.() -> Unit): NavigateArrow {
    val options = NavigateArrowOptions()
    options.action()
    val navigateArrow = addNavigateArrow(options)
    navigateArrow.isVisible = true
    return navigateArrow
}

//</editor-fold desc="绘制相关">