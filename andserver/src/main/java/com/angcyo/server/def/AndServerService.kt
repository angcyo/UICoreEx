package com.angcyo.server.def

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.angcyo.core.component.model.DataShareModel
import com.angcyo.core.vmApp
import com.angcyo.library.L
import com.angcyo.library.app
import com.angcyo.library.component.*
import com.angcyo.library.ex.*
import com.angcyo.library.getAppName
import com.angcyo.library.toastQQ
import com.angcyo.server.DslAndServer.DEFAULT_CHANNEL_NAME
import com.angcyo.server.DslAndServer.DEFAULT_NOTIFY_ICON
import com.angcyo.server.DslAndServer.DEFAULT_PORT
import com.angcyo.server.DslAndServer.DEFAULT_RETRY_COUNT
import com.yanzhenjie.andserver.AndServer
import com.yanzhenjie.andserver.Server
import com.yanzhenjie.andserver.Server.ServerListener
import java.net.BindException
import java.net.InetAddress
import java.util.concurrent.TimeUnit


/**
 * https://yanzhenjie.com/AndServer/
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/07/05
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
open class AndServerService : Service(), ServerListener, NetStateChangeObserver {

    /**端口*/
    var serverPort = DEFAULT_PORT

    /**端口被占用时, 重试的次数*/
    var retryCount = DEFAULT_RETRY_COUNT

    /**通知栏*/
    var showNotify: Boolean? = null

    /**通知通道*/
    var notifyChannelName = DEFAULT_CHANNEL_NAME

    /**通知图标*/
    var notifyIcon = DEFAULT_NOTIFY_ICON!!

    /**通知的名称*/
    var notifyName: String = ""

    /**Server的group
     *
     * 可以通过
     * [com.yanzhenjie.andserver.annotation.Config]
     * 注解声明
     * */
    var group = "default"

    init {
        notifyName = "${app().getAppName()}-${notifyChannelName}"
    }

    //<editor-fold desc="周期回调方法">

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        initServer()

        if (_needNotify) {
            RNetwork.registerObserver(this)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startServer()
        return START_STICKY
    }

    override fun onDestroy() {
        stopServer()
        super.onDestroy()
    }

    //</editor-fold desc="周期回调方法">

    override fun onNetConnected(networkType: NetworkType) {
        updateNotify()
    }

    //<editor-fold desc="Server">

    var _server: Server? = null

    /**初始化服务*/
    open fun initServer() {
        _server = AndServer.webServer(this, group)
            .port(serverPort)
            .timeout(10, TimeUnit.SECONDS)
            .listener(this)
            .build()
    }

    /**
     * Start server.
     */
    open fun startServer() {
        _server?.apply {
            startup()
            L.i(this@AndServerService.classHash() + "已启动!")
        }
    }

    /**
     * Stop server.
     */
    open fun stopServer() {
        _server?.shutdown()
    }

    var _notifyId: Int = (System.currentTimeMillis() and 0xFFFFFFF).toInt()

    val _needNotify: Boolean
        get() = showNotify == true || isDebug()

    override fun onStarted() {
        val address = address()
        L.i("${notifyName}已启动: $address")
        updateNotify()
    }

    override fun onStopped() {
        L.i("${notifyName}已停止: ${address()}")
        if (_notifyId > 0) {
            DslNotify.cancelNotify(this, _notifyId)
        }
    }

    //默认的端口
    var _defPort: Int = -1

    override fun onException(e: Exception) {
        L.e("${notifyName}异常: ${address()}")
        if (e is BindException) {
            if (_defPort < 0) {
                _defPort = serverPort
            }
            if ((serverPort - _defPort) < retryCount) {
                serverPort += 1

                L.w("${notifyName}重试端口: ${address()}")

                initServer()
                startServer()
            }
        } else {
            e.printStackTrace()
        }
    }

    //</editor-fold desc="Server">

    /**地址*/
    fun address(): String {
        return if (RNetwork.isConnect()) {
            val address: InetAddress = NetUtils.localIPAddress ?: return "无网络"
            "http:/$address:${serverPort}".apply {
                vmApp<DataShareModel>().shareServerAddressOnceData.postValue(this)
            }
        } else {
            "无网络"
        }
    }

    open fun updateNotify() {
        if (_needNotify) {
            val address = address()

            L.i("${simpleHash()} 服务地址:$address")

            //foreground
            startForeground(_notifyId, dslBuildNotify {
                notifySmallIcon = notifyIcon
                channelName = notifyChannelName
                notifyOngoing = true
                low()
                clickActivity(address.urlIntent())
                single(notifyName, "${address}\n${nowTimeString()}")
            })

            if (!isNotificationsEnabled() || !notifyChannelName.isChannelEnable()) {
                toastQQ("请打开通知通道[$notifyChannelName]")
            }
        }
    }

}