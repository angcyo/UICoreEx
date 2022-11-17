package com.angcyo.acc2.app.server

import android.app.Activity
import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.angcyo.library.L
import com.angcyo.server.def.AndServerService
import com.angcyo.server.DslAndServer

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/07/05
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class AccServerService : AndServerService() {

    init {
        notifyName = "AccServer"
        group = "acc"
    }

    override fun initServer() {
        super.initServer()
    }

    override fun updateNotify() {
        super.updateNotify()
    }
}

/**自动启动和停止[AccServerService]*/
fun LifecycleOwner.bindAccServer() {
    lifecycle.addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            val context: Context? = when (source) {
                is Fragment -> source.context
                is Activity -> source
                else -> null
            }

            if (context != null) {
                when (event) {
                    Lifecycle.Event.ON_CREATE -> context.startAccServer()
                    Lifecycle.Event.ON_DESTROY -> context.stopAccServer()
                    else -> Unit
                }
            } else {
                L.w("无效的context类型, 无法启动[AccServer]")
            }
        }
    })
}

fun Context.startAccServer() {
    DslAndServer.startServer(this, AccServerService::class.java)
}

fun Context.stopAccServer() {
    DslAndServer.stopServer(this, AccServerService::class.java)
}