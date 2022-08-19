package com.angcyo.websocket

import android.content.Intent
import android.os.IBinder
import com.angcyo.component.LifecycleService
import com.angcyo.core.vmApp
import com.angcyo.library.component.*
import com.angcyo.library.ex.isDebug
import com.angcyo.library.ex.nowTimeString
import com.angcyo.library.ex.urlIntent
import com.angcyo.library.toastQQ
import java.net.InetAddress

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/08/19
 */
open class WSService : LifecycleService(), NetStateChangeObserver {

    /**端口*/
    var serverPort = WSServer.DEFAULT_PORT

    /**通知栏*/
    var showNotify: Boolean? = null

    /**通知通道*/
    var notifyChannelName = "WebSocket"

    /**通知图标*/
    var notifyIcon = DslNotify.DEFAULT_NOTIFY_ICON

    /**通知的名称*/
    var notifyName: String = ""

    /**数据模型*/
    val wsServerModel = vmApp<WSServerModel>()

    init {
        notifyName = "WebSocket"
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

    /**服务端*/
    var _wsServer: WSServer? = null

    /**初始化服务*/
    open fun initServer() {
        WSServer.startWebSocketServer(serverPort) {
            serverPort = it.port
            _wsServer = it

            updateNotify()
        }

        //
        wsServerModel.stopServerData.observe(this) {
            it?.let {
                onStopped()
            }
        }
    }

    /**
     * Stop server.
     */
    open fun stopServer() {
        _wsServer?.stop()
    }

    var _notifyId: Int = (System.currentTimeMillis() and 0xFFFFFFF).toInt()

    val _needNotify: Boolean
        get() = showNotify == true || isDebug()

    fun onStopped() {
        if (_notifyId > 0) {
            DslNotify.cancelNotify(this, _notifyId)
        }
    }

    //</editor-fold desc="Server">

    /**地址*/
    fun address(): String {
        return if (RNetwork.isConnect()) {
            val address: InetAddress = NetUtils.localIPAddress ?: return "无网络"
            "ws:/$address:${serverPort}"
        } else {
            "无网络"
        }
    }

    open fun updateNotify() {
        if (_needNotify) {
            val address = address()

            //foreground
            startForeground(_notifyId, dslBuildNotify {
                notifySmallIcon = notifyIcon
                channelName = notifyChannelName
                notifyOngoing = true
                low()
                clickActivity(address.urlIntent())
                single(notifyName, "${nowTimeString()}\n${address}")
            })

            if (!isNotificationsEnabled() || !notifyChannelName.isChannelEnable()) {
                toastQQ("请打开通知通道[$notifyChannelName]")
            }
        }
    }
}