package com.angcyo.amap3d.core

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.core.view.doOnLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.amap.api.maps.TextureMapView
import com.angcyo.amap3d.AMapHelper
import com.angcyo.amap3d.DslAMap
import com.angcyo.amap3d.DslMarker
import com.angcyo.library.L
import com.angcyo.library.utils.requestMultiplePermissionsLauncher
import com.angcyo.library.ex.isTouchDown
import com.angcyo.library.ex.isTouchFinish
import com.angcyo.library.ex.mH
import com.angcyo.library.ex.mW

/**
 * 常用地图容器
 * https://lbs.amap.com/api/android-sdk/guide/create-map/show-map#map-view
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/06/11
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */

class RTextureMapView(context: Context, attributeSet: AttributeSet? = null) :
    TextureMapView(context, attributeSet) {

    //<editor-fold desc="基础">

    var requestMultiplePermissionsLauncher: ActivityResultLauncher<Array<String>>? = null

    //</editor-fold desc="基础">

    //<editor-fold desc="样式配置">

    /**包含默认的样式配置*/
    val dslAMap: DslAMap = DslAMap().apply {
        //do something
    }

    /**提供[Marker]的常规操作*/
    val dslMarker: DslMarker = DslMarker().apply {
        //do something
        //toHeatMapZoom = -1f //热力图
    }

    //</editor-fold desc="样式配置">

    //<editor-fold desc="必须的方法">

    /**绑定[LifecycleOwner]的生命周期*/
    fun bindLifecycle(owner: LifecycleOwner, savedInstanceState: Bundle?) {
        //请求权限
        val context = context
        val backLocation = true //是否需要后台定位权限
        val permissions = AMapHelper.permissions(true).toTypedArray()
        if (owner is ActivityResultCaller) {
            requestMultiplePermissionsLauncher =
                owner.requestMultiplePermissionsLauncher(ActivityResultCallback {
                    var result = true
                    for (p in permissions) {
                        if (it[p] == false) {
                            result = false
                            break
                        }
                    }
                    if (result) {
                        //有权限
                        dslAMap.checkMoveToFirst(map)
                    } else {
                        //无权限
                    }
                    requestMultiplePermissionsLauncher = null
                })
        } else {
            AMapHelper.requestPermissions(context, backLocation)
        }

        if (owner.lifecycle.currentState != Lifecycle.State.CREATED) {
            L.w("错误的生命周期绑定时机!")
        } else {
            owner.lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    //L.e("改变:$source ->$event")
                    when (event) {
                        Lifecycle.Event.ON_CREATE -> {
                            //地图初始化
                            onCreateR(savedInstanceState)
                            dslMarker.init(context, map)
                            dslAMap.apply {
                                doLocationStyle(map)
                                doUI(map)
                                loadStyleFromAssets(map, context.assets)
                            }
                        }
                        Lifecycle.Event.ON_RESUME -> {
                            onResume()
                            requestMultiplePermissionsLauncher?.launch(
                                AMapHelper.permissions(backLocation).toTypedArray()
                            )
                        }
                        Lifecycle.Event.ON_PAUSE -> onPause()
                        //OnDestroy 方法需要在OnDestroyView中调用
                        Lifecycle.Event.ON_DESTROY -> {
                            dslAMap._isMapLoaded = false
                            if (!_isDestory) {
                                onDestroyR()
                            }
                        }
                        else -> Unit
                    }
                }
            })
        }
    }

    var _isDestory = false

    fun onCreateR(savedInstanceState: Bundle?) {
        _isDestory = false
        onCreate(savedInstanceState)
    }

    fun onDestroyR() {
        _isDestory = true
        onDestroy()
    }

    //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
    fun saveInstanceState(outState: Bundle?) {
        onSaveInstanceState(outState)
    }

    //</editor-fold desc="必须的方法">

    /**设置地图中心点坐标, 相对于视图的坐标, 默认是0.5,0.5*/
    fun setPointToCenter(x: Float, y: Float) {
        doOnLayout {
            map.setPointToCenter((mW() * x).toInt(), (mH() * y).toInt())
        }
    }

    //<editor-fold desc="事件拦截">

    var interceptEvent: Boolean = false

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (interceptEvent) {
            if (ev.isTouchDown()) {
                parent.requestDisallowInterceptTouchEvent(true)
            } else if (ev.isTouchFinish()) {
                parent.requestDisallowInterceptTouchEvent(false)
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    //</editor-fold desc="事件拦截">
}
