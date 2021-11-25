package com.angcyo.amap3d.fragment

import android.os.Bundle
import com.amap.api.maps.AMap
import com.angcyo.amap3d.R
import com.angcyo.amap3d.core.RTextureMapView
import com.angcyo.amap3d.initMapView
import com.angcyo.core.fragment.BaseDslFragment

/**
 * 高德地图基类
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/23
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
open class BaseMapFragment : BaseDslFragment() {

    val mapView: RTextureMapView? get() = _vh.v<RTextureMapView>(R.id.lib_map_view)

    val map: AMap? get() = mapView?.map

    init {
        contentLayoutId = R.layout.base_map_fragment
        fragmentTitle = ""
    }

    override fun initBaseView(savedInstanceState: Bundle?) {
        super.initBaseView(savedInstanceState)

        initMapView(savedInstanceState)
    }

    open fun initMapView(savedInstanceState: Bundle?) {
        _vh.initMapView(this, savedInstanceState) {
            onInitMap(this)
        }
    }

    open fun onInitMap(mapView: RTextureMapView) {
        mapView.apply {
            dslMarker.toHeatMapZoom = -1f //关闭热力图

            //dslAMap.locationMoveFirst = true
            //myLatLng()

            //map?.onMapLoadedListener { }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView?.onDestroyR()
    }

}