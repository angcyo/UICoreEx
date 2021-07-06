package com.angcyo.server

import android.content.Context
import android.content.Intent
import com.angcyo.library.component.DslNotify

/**
 * https://github.com/yanzhenjie/AndServer
 *
 * https://yanzhenjie.com/AndServer/
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
    var DEFAULT_CHANNEL_NAME = "Server"

    /**通知图标*/
    var DEFAULT_NOTIFY_ICON = DslNotify.DEFAULT_NOTIFY_ICON

    fun startServer(context: Context, server: Class<*>) {
        val intent = Intent(context, server)
        context.startService(intent)
    }

    fun stopServer(context: Context, server: Class<*>) {
        val intent = Intent(context, server)
        context.stopService(intent)
    }

}