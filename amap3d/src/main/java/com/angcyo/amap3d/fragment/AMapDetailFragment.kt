package com.angcyo.amap3d.fragment

import android.os.Bundle
import com.angcyo.DslFHelper
import com.angcyo.amap3d.*
import com.angcyo.amap3d.core.MapLocation
import com.angcyo.amap3d.core.latLng
import com.angcyo.amap3d.core.toLatLng
import com.angcyo.base.back
import com.angcyo.core.fragment.BaseTitleFragment
import com.angcyo.getData
import com.angcyo.library.L
import com.angcyo.library.ex.dpi
import com.angcyo.library.toast
import com.angcyo.library.toastQQ
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

    override fun initBaseView(savedInstanceState: Bundle?) {
        super.initBaseView(savedInstanceState)

        _vh.initMapView(this, savedInstanceState) {
            map?.apply {
                //地图事件监听
                onMapLoadedListener {
                    targetMapLocation?.let {
                        val targetLatLng = it.latLng()
                        addMarker {
                            position(targetLatLng)
                            icon(markerIcon(R.drawable.map_location_point))
                        }

                        moveInclude(targetLatLng, myLocation.toLatLng(), padding = 150 * dpi)
                    }
                }

                onMyLocationChange {
                    targetMapLocation?.toLatLng()?.apply {
                        val distance = it.toLatLng().distance(this)
                        _vh.tv(R.id.lib_sub_text_view)?.text =
                            "${MapNaviUtils.friendlyDistance(distance)} ${targetMapLocation?.address}"
                    }
                }
            }
        }

        _vh.tv(R.id.lib_text_view)?.text = targetMapLocation?.poiName ?: "位置"
        _vh.tv(R.id.lib_sub_text_view)?.text = targetMapLocation?.address
        _vh.visible(R.id.lib_sub_text_view, targetMapLocation?.address != null)

        _vh.throttleClick(R.id.lib_nav_view) {
            if (AMapHelper.haveNavApp(fContext())) {
                AMapHelper.navTo(fContext(), targetMapLocation!!.toLatLng())
            } else {
                toastQQ("未安装高德APP")
//                MapNaviUtils.openGaoDeNavi(
//                    fContext(),
//                    targetMapLocation!!.toLatLng(),
//                    targetMapLocation?.poiName
//                )
            }
        }
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