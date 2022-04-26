package com.angcyo.amap3d.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.amap.api.maps.AMap
import com.angcyo.DslFHelper
import com.angcyo.amap3d.*
import com.angcyo.amap3d.core.MapLocation
import com.angcyo.amap3d.core.RTextureMapView
import com.angcyo.amap3d.core.latLng
import com.angcyo.amap3d.core.toLatLng
import com.angcyo.base.back
import com.angcyo.base.dslFHelper
import com.angcyo.core.fragment.BaseTitleFragment
import com.angcyo.getData
import com.angcyo.library.L
import com.angcyo.library.ex.dpi
import com.angcyo.library.ex.postDelay
import com.angcyo.library.toast
import com.angcyo.putData

/**
 * 高德坐标系坐标点详情
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/06/14
 * Copyright (c) 2020 angcyo. All rights reserved.
 */
class AMapDetailFragment : BaseTitleFragment() {

    var targetMapLocation: MapLocation? = null

    val mapView: RTextureMapView? get() = _vh.v<RTextureMapView>(R.id.lib_map_view)

    val map: AMap? get() = mapView?.map

    init {
        contentLayoutId = R.layout.map_detail_fragment
        fragmentTitle = "位置详情"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        targetMapLocation = getData()

        if (targetMapLocation == null) {
            toast("数据异常!")
            back()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView?.onDestroyR()
    }

    override fun initBaseView(savedInstanceState: Bundle?) {
        super.initBaseView(savedInstanceState)

        _vh.initMapView(this, savedInstanceState) {
            dslAMap.locationMoveFirst = false

            map?.apply {
                //地图事件监听
                onMapLoadedListener {
                    targetMapLocation?.let {
                        val targetLatLng = it.latLng()
                        addMarker {
                            position(targetLatLng)
                            icon(markerIcon(R.drawable.map_location_point))
                        }
                    }
                }

                onMyLocationChange {
                    targetMapLocation?.toLatLng()?.apply {
                        val distance = it.toLatLng().distance(this)
                        _vh.tv(R.id.lib_sub_text_view)?.text =
                            "${MapNaviUtils.friendlyDistance(distance)} ${targetMapLocation?.address}"
                    }
                }

                onMapLoadedAndLocationListener {
                    postDelay(100) {
                        moveInclude(targetMapLocation?.latLng(), it.toLatLng(), padding = 150 * dpi)
                    }
                }
            }
        }

        _vh.tv(R.id.lib_text_view)?.text = targetMapLocation?.poiName ?: "位置"
        _vh.tv(R.id.lib_sub_text_view)?.text = targetMapLocation?.address
        _vh.visible(R.id.lib_sub_text_view, targetMapLocation?.address != null)

        _vh.throttleClick(R.id.lib_nav_view) {
            targetMapLocation?.let { location ->
                AMapHelper.navTo(fContext(), location.toLatLng())
                /*if (AMapHelper.haveNavApp(fContext())) {
                } else {
                    //toastQQ("未安装高德APP")
                    //MapNaviUtils.openGaoDeNavi(fContext(), location.toLatLng(), location.poiName)
                }*/
            }
        }
    }
}

/**显示地图详情*/
fun Fragment.aMapDetail(
    latitude: Double,
    longitude: Double,
    address: String? = null,
    poiName: String? = null
) {
    dslFHelper {
        aMapDetail(MapLocation.from(latitude, longitude, address, poiName))
    }
}

/**快速启动高德地图, 并显示指定点的详情*/
fun DslFHelper.aMapDetail(location: MapLocation?) {
    if (location == null || (location.latitude.toLong() == 0L && location.longitude.toLong() == 0L)) {
        L.w("location is not valid.")
        return
    }
    show(AMapDetailFragment::class.java) {
        putData(location)
    }
}