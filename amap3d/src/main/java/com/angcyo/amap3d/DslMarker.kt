package com.angcyo.amap3d

import android.graphics.Color
import android.view.View
import android.view.animation.LinearInterpolator
import com.amap.api.maps.AMap
import com.amap.api.maps.model.*
import com.amap.api.maps.model.animation.AlphaAnimation
import com.amap.api.maps.model.animation.Animation
import com.amap.api.maps.model.animation.ScaleAnimation
import com.amap.api.maps.model.animation.TranslateAnimation
import com.angcyo.amap3d.DslMarker.Companion.DEFAULT_MARKER_ANIM_DURATION
import com.angcyo.amap3d.DslMarker.Companion.DEFAULT_MARKER_ANIM_INTERPOLATOR
import com.angcyo.library.L
import com.angcyo.library.ex.dpi
import kotlin.math.sqrt

/**
 * 管理一组[Marker]
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/06/12
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */

class DslMarker : AMap.InfoWindowAdapter {

    companion object {
        const val DEFAULT_MARKER_ANIM_DURATION = 360L
        val DEFAULT_MARKER_ANIM_INTERPOLATOR = LinearInterpolator()

        val ALT_HEATMAP_GRADIENT_COLORS = intArrayOf(
            Color.argb(0, 0, 255, 255),
            Color.argb(255 / 3 * 2, 0, 255, 0),
            Color.rgb(125, 191, 0),
            Color.rgb(185, 71, 0),
            Color.rgb(255, 0, 0)
        )

        val ALT_HEATMAP_GRADIENT_START_POINTS = floatArrayOf(
            0.0f,
            0.10f, 0.20f, 0.60f, 1.0f
        )
    }

    //<editor-fold desc="配置属性">

    /**自动进入热力图模式, 当缩放级别小于等于此值时. 将所有[Marker]隐藏, 并展示热力图.推荐使用15f*/
    var toHeatMapZoom: Float = 15f

    /**热力图颜色渐变配置*/
    var heatGradient = Gradient(ALT_HEATMAP_GRADIENT_COLORS, ALT_HEATMAP_GRADIENT_START_POINTS)

    //</editor-fold desc="配置属性">

    //<editor-fold desc="初始化">

    var map: AMap? = null

    //管理所有Marker
    var _allMarker: MutableList<Marker> = mutableListOf()

    fun init(map: AMap) {
        this.map = map

        map.setInfoWindowAdapter(this)

        map.onCameraChangeListener(change = {
            if (it.zoom <= toHeatMapZoom) {
                //进入热力图模式
                if (!isHeatOverlay) {
                    changeToHeat()
                }
            } else {
                //显示所有的[Marker]
                if (isHeatOverlay) {
                    changeToMarker()
                }
            }
        })
    }

    fun _checkInit(action: AMap.() -> Unit) {
        if (map == null) {
            L.w("map is not init!")
        } else {
            map?.action()
        }
    }

    /**此方法不能修改整个 InfoWindow 的背景和边框，无论自定义的样式是什么样，SDK 都会在最外层添加一个默认的边框。*/
    override fun getInfoContents(marker: Marker): View? {
        return null
    }

    /**如果此方法返回的 View 没有设置 InfoWindow 背景图，SDK 会默认添加一个背景图。*/
    override fun getInfoWindow(marker: Marker): View? {
        //do something
        return null
    }

    //</editor-fold desc="初始化">

    //<editor-fold desc="热力图操作">

    //热力图层
    var _tileOverlay: TileOverlay? = null

    //是否处于热力图模式
    val isHeatOverlay: Boolean get() = _tileOverlay != null

    /**热力图状态
     * http://a.amap.com/lbs/static/unzip/Android_Map_Doc/3D/com/amap/api/maps/model/HeatmapTileProvider.Builder.html
     * */
    fun changeToHeat() {
        hideAllMarker()
        removeHeatOverlay()
        _checkInit {

            // 第一步： 生成热力点坐标列表
            val latLngList = mutableListOf<LatLng>()
            _allMarker.forEach {
                latLngList.add(it.position)
            }

            // 第二步： 构建热力图 TileProvider
            val builder = HeatmapTileProvider.Builder().apply {
                // 设置热力图绘制的数据
                data(latLngList)
                // 设置热力图渐变，有默认值 DEFAULT_GRADIENT，可不设置该接口
                gradient(heatGradient)
            }

            // Gradient 的设置可见参考手册
            // 构造热力图对象
            val tileProvider = builder.build()

            // 第三步： 构建热力图参数对象
            val tileOverlayOptions = TileOverlayOptions()
            tileOverlayOptions.tileProvider(tileProvider) // 设置瓦片图层的提供者

            // 第四步： 添加热力图
            _tileOverlay = addTileOverlay(tileOverlayOptions)
        }
    }

    /**[Marker]状态*/
    fun changeToMarker() {
        removeHeatOverlay()
        showAllMarker()
    }

    /**移除热力图*/
    fun removeHeatOverlay() {
        _tileOverlay?.clearTileCache()
        _tileOverlay?.remove()
        _tileOverlay = null
    }

    /**隐藏所有[Marker]*/
    fun hideAllMarker() {
        _allMarker.forEach {
            it.isVisible = false
        }
    }

    /**显示所有[Marker]*/
    fun showAllMarker() {
        _allMarker.forEach {
            it.isVisible = true
        }
    }

    //</editor-fold desc="热力图操作">

    //<editor-fold desc="操作方法">

    /**添加一个[Marker]并管理*/
    fun addMarker(
        latLng: LatLng,
        data: Any? = null,
        icon: BitmapDescriptor? = null,
        action: MarkerOptions.() -> Unit = {}
    ) {
        _checkInit {
            addMarker {
                anchor(0.5f, 0.5f)
                icon(icon)
                position(latLng)
                action()
            }.apply {
                `object` = data
                _allMarker.add(this)
            }
        }
    }

    //</editor-fold desc="操作方法">

}

fun AMap.dslMarker(action: DslMarker.() -> Unit): DslMarker {
    val dslMarker = DslMarker()
    dslMarker.init(this)
    dslMarker.action()
    return dslMarker
}

//<editor-fold desc="Marker动画">

//https://a.amap.com/lbs/static/unzip/Android_Map_Doc/3D/com/amap/api/maps/model/animation/ScaleAnimation.html

fun Marker.cancelAnim() {
    val animation = AlphaAnimation(1f, 1f)
    setAnimation(animation)
    startAnimation()
}

fun Marker.anim(animation: Animation, action: Animation.() -> Unit = {}) {
    //缩放动画保持最后一帧
    animation.fillMode = Animation.FILL_MODE_BACKWARDS
    animation.setDuration(DEFAULT_MARKER_ANIM_DURATION)
    animation.setInterpolator(DEFAULT_MARKER_ANIM_INTERPOLATOR)
    animation.action()
    setAnimation(animation)
    startAnimation()
}

fun Marker.scale(from: Float, to: Float, action: ScaleAnimation.() -> Unit = {}) {
    scale(from, to, from, to, action)
}

fun Marker.scale(
    fromX: Float,
    toX: Float,
    fromY: Float,
    toY: Float,
    action: ScaleAnimation.() -> Unit = {}
) {
    val animation = ScaleAnimation(fromX, toX, fromY, toY)
    anim(animation) {
        animation.action()
    }
}

/**上下跳动动画
 * */
fun Marker.jump(map: AMap, offsetY: Int = 100 * dpi, action: TranslateAnimation.() -> Unit = {}) {
    val latLng = position
    val point = map.projection.toScreenLocation(latLng)
    point.y -= offsetY //偏移
    val fromLatLng = map.projection.fromScreenLocation(point)

    //动画
    val animation = TranslateAnimation(fromLatLng)
    anim(animation) {
        animation.setInterpolator { input ->
            // 模拟重加速度的interpolator
            if (input <= 0.5) {
                (0.5f - 2 * (0.5 - input) * (0.5 - input)).toFloat()
            } else {
                (0.5f - sqrt((input - 0.5f) * (1.5f - input).toDouble())).toFloat()
            }
        }
        animation.action()
    }
}

//</editor-fold desc="Marker动画">

//<editor-fold desc="Marker图标">

fun markerIcon(res: Int) = BitmapDescriptorFactory.fromResource(res)

fun markerIcon(view: View) = BitmapDescriptorFactory.fromView(view)

//</editor-fold desc="Marker图标">


