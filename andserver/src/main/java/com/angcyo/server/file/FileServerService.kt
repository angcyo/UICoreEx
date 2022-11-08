package com.angcyo.server.file

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.angcyo.library.L
import com.angcyo.library.app
import com.angcyo.library.getAppName
import com.angcyo.server.AndServerService
import com.angcyo.server.DslAndServer

/**
 * 文件服务
 *
 * [com.angcyo.server.file.FileWebConfig]
 * [com.angcyo.server.LogFileInterceptor]
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/09/22
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class FileServerService : AndServerService() {

    init {
        notifyName = "${app().getAppName()}-FileServer"
        notifyChannelName = "FileServer"
        group = "file"
    }

    override fun initServer() {
        super.initServer()
    }

    override fun updateNotify() {
        super.updateNotify()
    }
}

/**自动启动和停止[FileServerService]*/
fun LifecycleOwner.bindFileServer() {
    lifecycle.addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            val context: Context? = when (source) {
                is Fragment -> source.context
                is Activity -> source
                is Application -> source
                else -> null
            }

            if (context != null) {
                when (event) {
                    Lifecycle.Event.ON_CREATE -> context.startFileServer()
                    Lifecycle.Event.ON_DESTROY -> context.stopFileServer()
                    else -> Unit
                }
            } else {
                L.w("无效的context类型, 无法启动[FileServerService]")
            }
        }
    })
}

/**开始一个文件服务, 用来访问app外部文件目录*/
fun Context.startFileServer() {
    DslAndServer.startServer(this, FileServerService::class.java)
}

/**停止文件服务*/
fun Context.stopFileServer() {
    DslAndServer.stopServer(this, FileServerService::class.java)
}