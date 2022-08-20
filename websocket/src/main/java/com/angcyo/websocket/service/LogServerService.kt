package com.angcyo.websocket.service

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.angcyo.library.L

/**
 * 用来输出[L]日志的[WebSocket]服务
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/08/19
 */
class LogServerService : WSService() {

    init {
        notifyName = "LogServer"
        notifyChannelName = "LogServer"
    }

    override fun initServer() {
        super.initServer()
        L.logPrintList.add { tag: String, level: Int, msg: String ->
            _wsServer?.sendMessage(msg)
        }
    }
}

/**自动启动和停止[LogServerService]*/
fun LifecycleOwner.bindLogServer() {
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
                    Lifecycle.Event.ON_CREATE -> context.startLogServer()
                    Lifecycle.Event.ON_DESTROY -> context.stopLogServer()
                    else -> Unit
                }
            } else {
                L.w("无效的context类型, 无法启动[LogServerService]")
            }
        }
    })
}

/**开始一个文件服务, 用来访问app外部文件目录*/
fun Context.startLogServer() {
    val intent = Intent(this, LogServerService::class.java)
    try {
        startService(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**停止文件服务*/
fun Context.stopLogServer() {
    val intent = Intent(this, LogServerService::class.java)
    stopService(intent)
}