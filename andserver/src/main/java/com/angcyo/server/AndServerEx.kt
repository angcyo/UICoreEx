package com.angcyo.server

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.angcyo.library.L
import com.angcyo.library.ex.toBytes
import com.angcyo.library.ex.toInputStream
import com.angcyo.server.def.AndServerService
import com.angcyo.server.file.FileServerService
import com.yanzhenjie.andserver.framework.body.StreamBody
import com.yanzhenjie.andserver.http.HttpResponse
import com.yanzhenjie.andserver.util.MediaType
import java.io.InputStream

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/07/06
 * Copyright (c) 2020 angcyo. All rights reserved.
 */

/**发送图片*/
fun HttpResponse.sendBitmap(bitmap: Bitmap, mediaType: MediaType = MediaType.IMAGE_PNG) {
    val bytes = bitmap.toBytes()
    val inputStream: InputStream? = bytes?.toInputStream()
    setBody(StreamBody(inputStream, (bytes?.size ?: 0).toLong(), mediaType))
}

//---

/**自动启动和停止[AndServerService]*/
fun LifecycleOwner.bindAndServer() {
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
                    Lifecycle.Event.ON_CREATE -> context.startAndServer()
                    Lifecycle.Event.ON_DESTROY -> context.stopAndServer()
                    else -> Unit
                }
            } else {
                L.w("无效的context类型, 无法启动[AndServerService]")
            }
        }
    })
}

/**开始服务*/
fun Context.startAndServer() {
    DslAndServer.startServer(this, AndServerService::class.java)
}

/**停止服务*/
fun Context.stopAndServer() {
    DslAndServer.stopServer(this, AndServerService::class.java)
}

//---


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