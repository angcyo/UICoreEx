package com.angcyo.amap3d

import android.content.res.ColorStateList
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.angcyo.library.ex._color
import com.angcyo.widget.DslViewHolder

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/06/14
 * Copyright (c) 2020 angcyo. All rights reserved.
 */

/**绑定控制按钮事件, 定位/放大/缩小*/
fun AMap.bindControlLayout(vh: DslViewHolder) {
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