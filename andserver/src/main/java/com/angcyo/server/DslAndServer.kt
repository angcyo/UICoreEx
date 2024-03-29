package com.angcyo.server

import android.content.Context
import android.content.Intent
import com.angcyo.core.coreApp
import com.angcyo.item.component.DebugFragment
import com.angcyo.library.component.DslNotify

/**
 * https://github.com/yanzhenjie/AndServer
 *
 * https://yanzhenjie.com/AndServer/
 *
 * [com.yanzhenjie.andserver.DispatcherHandler]
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/07/05
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
object DslAndServer {

    /**默认端口*/
    var DEFAULT_PORT = 9200

    /**重试次数*/
    var DEFAULT_RETRY_COUNT = 10

    /**通知通道名*/
    var DEFAULT_CHANNEL_NAME = "AndServer"

    /**通知图标*/
    var DEFAULT_NOTIFY_ICON: Int? = null
        get() = field ?: DslNotify.DEFAULT_NOTIFY_ICON

    init {
        DebugFragment.addDebugAction {
            name = "AndServer"
            action = { _, _ ->
                coreApp().bindAndServer()
            }
        }
        DebugFragment.addDebugAction {
            name = "FileServer"
            action = { _, _ ->
                coreApp().bindFileServer()
                //(RBackground.lastActivityRef?.get() as? LifecycleOwner ?: it)
            }
        }

        //注入api 2023-09-10 减少依赖, 请手动注入
        /*try {
            Api.injectApiAction { _, appJs ->
                appJs.registerJavaMethod({ _, _ ->
                    lastContext.startAndServer()
                }, "startAndServer")
                appJs.registerJavaMethod({ _, _ ->
                    lastContext.stopAndServer()
                }, "stopAndServer")
                appJs.registerJavaMethod({ _, _ ->
                    lastContext.startFileServer()
                }, "startFileServer")
                appJs.registerJavaMethod({ _, _ ->
                    lastContext.stopFileServer()
                }, "stopFileServer")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }*/
    }

    //app is in background uid UidRecord{d5b2549 u0a216 TPSL idle procs:1 seq(0,0,0)}
    fun startServer(context: Context, server: Class<*>) {
        val intent = Intent(context, server)
        try {
            context.startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopServer(context: Context, server: Class<*>) {
        val intent = Intent(context, server)
        context.stopService(intent)
    }

}