package com.angcyo.amap3d

import android.content.res.AssetManager
import com.amap.api.maps.*
import com.amap.api.maps.model.*
import com.angcyo.library.L
import com.angcyo.library.ex._color
import com.angcyo.library.ex.alpha
import com.angcyo.library.ex.dp
import com.angcyo.library.ex.undefined_size
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
class DslAmap {

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

    /**执行定位样式配置*/
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
                map.moveTo(LatLng(it.latitude, it.longitude), locationMoveZoom)
            }

            var listener: AMap.OnMyLocationChangeListener? = null
            listener = onMyLocationChange(map) {
                map.removeOnMyLocationChangeListener(listener)
                if (locationMoveFirst) {
                    map.moveTo(LatLng(it.latitude, it.longitude), locationMoveZoom)
                }
            }
        }
    }

    //</editor-fold desc="定位蓝点相关">

    //<editor-fold desc="样式相关">

    var customStyleOptions: CustomMapStyleOptions? = null

    /**从assets中加载高德地图style*/
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
     * [AMap.MAP_TYPE_NORMAL] 矢量地图模式
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
    var showMyLocationButton: Boolean = true

    /**显示指南针*/
    var showCompass: Boolean = true

    /**显示比例尺控件*/
    var showScaleControl: Boolean = true

    /**显示放大缩小控件*/
    var showZoomControl: Boolean = true

    /**
     * 放大缩小控件位置
     * [AMapOptions.ZOOM_POSITION_RIGHT_CENTER]
     * [AMapOptions.LOGO_POSITION_BOTTOM_RIGHT]
     * */
    var zoomControlPosition: Int = AMapOptions.LOGO_POSITION_BOTTOM_RIGHT

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

            logoPosition = this@DslAmap.logoPosition

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
        //移动到有室内地图的地方,放大级别才可以看见
        //map.moveTo(LatLng(39.91095, 116.37296), 20f)
    }

    //</editor-fold desc="UI相关">

    //<editor-fold desc="事件回调相关">

    /**地图定位改变后的回调, 如果使用的是持续定位方式, 则会持续回调.
     * [ON_PAUSE] 之后地图依旧会回调定位信息*/
    fun onMyLocationChange(
        map: AMap,
        action: (location: Inner_3dMap_location) -> Unit = {}
    ): AMap.OnMyLocationChangeListener {
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
            //L.w(map.cameraPosition)
            //L.w(map.myLocation)
            action(it as Inner_3dMap_location)
        }
        map.addOnMyLocationChangeListener(listener)
        return listener
    }

    /**地图加载回调*/
    fun onMapLoadedListener(
        map: AMap,
        action: () -> Unit = {}
    ): AMap.OnMapLoadedListener {
        val listener = AMap.OnMapLoadedListener {
            L.e("AMapLoaded...")
            action()
        }
        map.setOnMapLoadedListener(listener)
        return listener
    }

    //</editor-fold desc="事件回调相关">

    //<editor-fold desc="绘制相关">

    /**添加标记[Marker]
     * https://a.amap.com/lbs/static/unzip/Android_Map_Doc/3D/com/amap/api/maps/model/MarkerOptions.html
     * */
    fun addMarker(map: AMap, action: MarkerOptions.() -> Unit): Marker {
        val options = MarkerOptions()
        options.action()
        val marker = map.addMarker(options)
        marker.isVisible = true
        return marker
    }

    /**在地图上添一组图片标记（marker）对象，并设置是否改变地图状态以至于所有的marker对象都在当前地图可视区域范围内显示。*/
    fun addMarkers(
        map: AMap,
        options: ArrayList<MarkerOptions>,
        moveToCenter: Boolean = true
    ): MutableList<Marker> {
        val marker = map.addMarkers(options, moveToCenter)
        return marker
    }

    /**添加折线[Polyline]
     * https://a.amap.com/lbs/static/unzip/Android_Map_Doc/3D/com/amap/api/maps/model/PolylineOptions.html
     * */
    fun addPolyline(map: AMap, action: PolylineOptions.() -> Unit): Polyline {
        val options = PolylineOptions()
        options.action()
        val polyline = map.addPolyline(options)
        polyline.isVisible = true
        return polyline
    }

    /**添加弧形[Arc]
     * https://a.amap.com/lbs/static/unzip/Android_Map_Doc/3D/com/amap/api/maps/model/Arc.html
     * */
    fun addArc(map: AMap, action: ArcOptions.() -> Unit): Arc {
        val options = ArcOptions()
        options.action()
        val arc = map.addArc(options)
        arc.isVisible = true
        return arc
    }

    /**添加圆[Circle]
     * https://a.amap.com/lbs/static/unzip/Android_Map_Doc/3D/com/amap/api/maps/model/Circle.html
     * */
    fun addCircle(map: AMap, action: CircleOptions.() -> Unit): Circle {
        val options = CircleOptions()
        options.action()
        val circle = map.addCircle(options)
        circle.isVisible = true
        return circle
    }

    /**添加多边形[Polygon]
     * https://a.amap.com/lbs/static/unzip/Android_Map_Doc/3D/com/amap/api/maps/model/Polygon.html
     * */
    fun addPolygon(map: AMap, action: PolygonOptions.() -> Unit): Polygon {
        val options = PolygonOptions()
        options.action()
        val polygon = map.addPolygon(options)
        polygon.isVisible = true
        return polygon
    }

    /**添加文本标记[Text]
     * https://a.amap.com/lbs/static/unzip/Android_Map_Doc/3D/com/amap/api/maps/model/Text.html
     * */
    fun addText(map: AMap, action: TextOptions.() -> Unit): Text {
        val options = TextOptions()
        options.action()
        val text = map.addText(options)
        text.isVisible = true
        return text
    }

    //</editor-fold desc="绘制相关">

    //<editor-fold desc="操作相关">

    /**移动至当前位置*/
    fun moveToLocation(map: AMap, animDuration: Long = 250 /*动画耗时250毫秒, 小于0表示关闭动画*/) {
        map.myLocation?.let {
            map.moveTo(LatLng(it.latitude, it.longitude), locationMoveZoom, animDuration)
        }
    }

    //</editor-fold desc="操作相关">
}

/**DSL*/
fun TextureMapView.dslAmap(action: DslAmap.() -> Unit) {
    val dslAmap = DslAmap()
    dslAmap.action()
}

/**DSL*/
fun MapView.dslAmap(action: DslAmap.() -> Unit) {
    val dslAmap = DslAmap()
    dslAmap.action()
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