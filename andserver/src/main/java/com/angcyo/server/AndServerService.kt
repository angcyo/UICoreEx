package com.angcyo.server

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.angcyo.library.L
import com.angcyo.library.component.*
import com.angcyo.library.ex.isDebug
import com.angcyo.library.ex.urlIntent
import com.angcyo.library.toastQQ
import com.angcyo.server.DslAndServer.DEFAULT_CHANNEL_NAME
import com.angcyo.server.DslAndServer.DEFAULT_NOTIFY_ICON
import com.angcyo.server.DslAndServer.DEFAULT_PORT
import com.angcyo.server.DslAndServer.DEFAULT_RETRY_COUNT
import com.yanzhenjie.andserver.AndServer
import com.yanzhenjie.andserver.Server
import com.yanzhenjie.andserver.Server.ServerListener
import java.net.InetAddress
import java.net.SocketException
import java.util.concurrent.TimeUnit


/**
 * https://yanzhenjie.com/AndServer/
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/07/05
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
open class AndServerService : Service(), ServerListener {

    /**端口*/
    var serverPort = DEFAULT_PORT

    /**端口被占用时, 重试的次数*/
    var retryCount = DEFAULT_RETRY_COUNT

    /**通知栏*/
    var showNotify: Boolean? = null

    /**通知通道*/
    var notifyChannelName = DEFAULT_CHANNEL_NAME

    /**通知图标*/
    var notifyIcon = DEFAULT_NOTIFY_ICON

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        initServer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startServer()
        return START_STICKY
    }

    override fun onDestroy() {
        stopServer()
        super.onDestroy()
    }

    var _server: Server? = null

    /**初始化服务*/
    open fun initServer() {
        _server = AndServer.webServer(this)
            .port(serverPort)
            .timeout(10, TimeUnit.SECONDS)
            .listener(this)
            .build()
    }

    /**
     * Start server.
     */
    open fun startServer() {
        _server?.startup()
    }

    /**
     * Stop server.
     */
    open fun stopServer() {
        _server?.shutdown()
    }

    /**地址*/
    fun address(): String {
        val address: InetAddress? = NetUtils.localIPAddress
        return "http:/$address:${serverPort}"
    }

    var _notifyId: Int = (System.currentTimeMillis() and 0xFFFFFFF).toInt()

    override fun onStarted() {
        val address = address()
        L.i("AndServer已启动: $address")
        if (showNotify == true || isDebug()) {

            //foreground
            startForeground(_notifyId, dslBuildNotify {
                notifySmallIcon = notifyIcon
                channelName = notifyChannelName
                notifyOngoing = true
                low()
                clickActivity(address.urlIntent())
                single("AccServer已启动", address)
            })

            if (!isNotificationsEnabled() || !notifyChannelName.isChannelEnable()) {
                toastQQ("请打开通知通道[$notifyChannelName]")
            }
        }
    }

    override fun onStopped() {
        L.i("AndServer已停止: ${address()}")
        if (_notifyId > 0) {
            DslNotify.cancelNotify(this, _notifyId)
        }
    }

    //默认的端口
    var _defPort: Int = -1

    override fun onException(e: Exception) {
        L.e("AndServer异常: ${address()}")
        e.printStackTrace()

        if (e is SocketException) {
            if (_defPort < 0) {
                _defPort = serverPort
            }
            if ((serverPort - _defPort) < retryCount) {
                serverPort += 1

                L.w("AndServer重试端口: ${address()}")

                initServer()
                startServer()
            }
        }
    }
}