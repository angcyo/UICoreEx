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
import com.angcyo.library.app
import com.angcyo.library.getAppName

/**
 * 用来输出[L]日志的[WebSocket]服务
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/08/19
 */
class LogWSService : WSService() {

    init {
        notifyName = "${app().getAppName()}-LogServer"
        notifyChannelName = "LogServer"
    }

    override fun initServer() {
        super.initServer()

        L.logPrintList.add { tag: String, level: Int, msg: String ->
            //将L输出的日志, 全部发送给客户端
            _wsServer?.sendMessage(msg)
        }
    }
}

/**自动启动和停止[LogWSService]*/
fun LifecycleOwner.bindLogWSServer() {
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
                    Lifecycle.Event.ON_CREATE -> context.startLogWSServer()
                    Lifecycle.Event.ON_DESTROY -> context.stopLogWSServer()
                    else -> Unit
                }
            } else {
                L.w("无效的context类型, 无法启动[LogServerService]")
            }
        }
    })
}

/**开始一个文件服务, 用来访问app外部文件目录*/
fun Context.startLogWSServer() {
    val intent = Intent(this, LogWSService::class.java)
    try {
        startService(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**停止文件服务*/
fun Context.stopLogWSServer() {
    val intent = Intent(this, LogWSService::class.java)
    stopService(intent)
}