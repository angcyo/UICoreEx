package com.angcyo.server.file

import android.app.Activity
import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.angcyo.library.L
import com.angcyo.server.AndServerService
import com.angcyo.server.DslAndServer

/**
 * 文件服务
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/09/22
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */

class FileServer : AndServerService() {

    init {
        notifyName = "FileServer已启动"
    }

    override fun initServer() {
        super.initServer()
    }

    override fun updateNotify() {
        super.updateNotify()
    }
}

/**自动启动和停止[FileServer]*/
fun LifecycleOwner.bindFileServer() {
    lifecycle.addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            val context: Context? = when (source) {
                is Fragment -> source.context
                is Activity -> source
                else -> null
            }

            if (context != null) {
                when (event) {
                    Lifecycle.Event.ON_CREATE -> context.startFileServer()
                    Lifecycle.Event.ON_DESTROY -> context.stopFileServer()
                    else -> Unit
                }
            } else {
                L.w("无效的context类型, 无法启动[FileServer]")
            }
        }
    })
}

fun Context.startFileServer() {
    DslAndServer.startServer(this, FileServer::class.java)
}

fun Context.stopFileServer() {
    DslAndServer.stopServer(this, FileServer::class.java)
}