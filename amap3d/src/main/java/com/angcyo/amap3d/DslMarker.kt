package com.angcyo.amap3d

import android.view.View
import android.view.animation.LinearInterpolator
import com.amap.api.maps.AMap
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.animation.Animation
import com.amap.api.maps.model.animation.ScaleAnimation
import com.amap.api.maps.model.animation.TranslateAnimation
import com.angcyo.amap3d.DslMarker.Companion.DEFAULT_MARKER_ANIM_DURATION
import com.angcyo.amap3d.DslMarker.Companion.DEFAULT_MARKER_ANIM_INTERPOLATOR
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
    }

    //<editor-fold desc="初始化">

    var map: AMap? = null

    fun init(map: AMap) {
        this.map = map

        map.setInfoWindowAdapter(this)
    }

    override fun getInfoContents(marker: Marker): View? {
        return null
    }

    override fun getInfoWindow(marker: Marker): View? {
        return getInfoContents(marker)
    }

    //</editor-fold desc="初始化">

}

//<editor-fold desc="Marker动画">

//https://a.amap.com/lbs/static/unzip/Android_Map_Doc/3D/com/amap/api/maps/model/animation/ScaleAnimation.html

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


