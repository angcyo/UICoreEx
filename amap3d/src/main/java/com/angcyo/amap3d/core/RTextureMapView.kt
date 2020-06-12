package com.angcyo.amap3d.core

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.amap.api.maps.TextureMapView
import com.angcyo.amap3d.AmapHelper
import com.angcyo.amap3d.DslAMap
import com.angcyo.library.L

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

    //<editor-fold desc="样式配置">

    val dslAMap = DslAMap().apply {
        //do thing
    }

    //</editor-fold desc="样式配置">

    //</editor-fold desc="必须的方法">

    /**绑定[LifecycleOwner]的生命周期*/
    fun bindLifecycle(owner: LifecycleOwner, savedInstanceState: Bundle?) {
        //请求权限
        AmapHelper.requestPermissions(context, true)

        if (owner.lifecycle.currentState != Lifecycle.State.CREATED) {
            L.w("错误的生命周期绑定时机!")
        } else {
            owner.lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    //L.e("改变:$source ->$event")
                    when (event) {
                        Lifecycle.Event.ON_CREATE -> {
                            onCreate(savedInstanceState)
                            dslAMap.apply {
                                doLocationStyle(map)
                                doUI(map)
                                loadStyleFromAssets(map, context.assets)
                            }
                        }
                        Lifecycle.Event.ON_RESUME -> onResume()
                        Lifecycle.Event.ON_PAUSE -> onPause()
                        Lifecycle.Event.ON_DESTROY -> onDestroy()
                        else -> Unit
                    }
                }
            })
        }
    }

    //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
    fun saveInstanceState(outState: Bundle?) {
        onSaveInstanceState(outState)
    }

    //<editor-fold desc="必须的方法">

}
