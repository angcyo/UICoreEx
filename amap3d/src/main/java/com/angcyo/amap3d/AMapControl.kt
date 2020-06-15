package com.angcyo.amap3d

import android.content.res.ColorStateList
import android.view.View
import android.widget.TextView
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.CustomMapStyleOptions
import com.angcyo.amap3d.core.RTextureMapView
import com.angcyo.library.ex._color
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.postDelay

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/06/14
 * Copyright (c) 2020 angcyo. All rights reserved.
 */

fun RTextureMapView.bindControlLayout(vh: DslViewHolder) {
    map.bindControlLayout(vh, dslAMap.customStyleOptions)
}

/**绑定控制按钮事件, 定位/放大/缩小*/
fun AMap.bindControlLayout(
    vh: DslViewHolder,
    customMapStyleOptions: CustomMapStyleOptions? = null
) {
    //卫星图/矢量图(普通地图) 切换
    fun View.typeText() {
        if (this is TextView) {
            text = if (this.isSelected) {
                "矢量图"
            } else {
                "影像图"
            }
        }
    }
    vh.view(R.id.map_type_view)?.typeText()
    vh.throttleClick(R.id.map_type_view) {
        if (it.isSelected) {
            //已经是卫星图
            mapType = AMap.MAP_TYPE_NORMAL
            customMapStyleOptions?.apply {
                isEnable = true
                setCustomMapStyle(this)
                it.postDelay(160) {
                    setCustomMapStyle(this)
                }
            }
        } else {
            mapType = AMap.MAP_TYPE_SATELLITE
            customMapStyleOptions?.apply {
                isEnable = false
                setCustomMapStyle(this)
            }
        }
        it.isSelected = !it.isSelected
        vh.view(R.id.map_type_view)?.typeText()
    }
    //缩小
    vh.click(R.id.map_zoom_out_view) {
        moveTo(CameraUpdateFactory.zoomOut())
    }
    //放大
    vh.click(R.id.map_zoom_in_view) {
        moveTo(CameraUpdateFactory.zoomIn())
    }
    //我的位置
    vh.click(R.id.map_location_view) {
        moveToLocation()
    }
    //禁用和启用控制按钮
    onCameraChangeListener {
        vh.enable(R.id.map_zoom_in_view, maxZoomLevel != it.zoom)
        vh.enable(R.id.map_zoom_out_view, minZoomLevel != it.zoom)
        if (maxZoomLevel == it.zoom) {
            vh.img(R.id.map_zoom_in_view)?.imageTintList =
                ColorStateList.valueOf(_color(R.color.lib_disable_bg_color))
        } else {
            vh.img(R.id.map_zoom_in_view)?.imageTintList = null
        }

        if (minZoomLevel == it.zoom) {
            vh.img(R.id.map_zoom_out_view)?.imageTintList =
                ColorStateList.valueOf(_color(R.color.lib_disable_bg_color))
        } else {
            vh.img(R.id.map_zoom_out_view)?.imageTintList = null
        }
    }
}