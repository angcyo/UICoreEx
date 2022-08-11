package com.angcyo.wifip2p.task

import com.angcyo.library.L
import java.io.IOException
import java.net.BindException
import java.net.ServerSocket

/**
 * 接收数据的[Runnable]
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/08/09
 */
class WifiP2pReceiveRunnable : Runnable {

    companion object {
        /**默认端口*/
        const val SERVER_PORT = 29000

        /**最大Socket连接数*/
        const val MAX_SERVER_CONNECTIONS = 25

        /**Buffer大小*/
        const val BUFFER_SIZE = 20 * 1024
    }

    /**启动监听的端口*/
    var port: Int = SERVER_PORT

    /**接收到[Socket]后的回调*/
    var receiveListener: ReceiveListener? = ByteReceiveListener()

    var _serverSocket: ServerSocket? = null

    /**是否被取消*/
    @Volatile
    var isCancel: Boolean = false

    init {
        _initialize()
    }

    override fun run() {
        _startServerSocket()
    }

    /**初始化, 分配端口*/
    fun _initialize() {
        try {
            _serverSocket = ServerSocket(port, MAX_SERVER_CONNECTIONS)
            _serverSocket?.reuseAddress = true
            _serverSocket?.receiveBufferSize = BUFFER_SIZE
        } catch (e: BindException) {
            e.printStackTrace()
            port++
            _initialize()
        } catch (e: IOException) {
            e.printStackTrace()
            try {
                _serverSocket = ServerSocket(0, MAX_SERVER_CONNECTIONS)
                _serverSocket?.reuseAddress = true
                _serverSocket?.receiveBufferSize = BUFFER_SIZE
                port = _serverSocket?.localPort ?: port //端口
            } catch (ioEx: IOException) {
                L.e("Failed to get a random port, Salut will not work correctly.")
            }
        }
    }

    /**启动服务, 开始接收数据*/
    fun _startServerSocket() {
        val serverSocket = _serverSocket
        if (serverSocket == null) {
            L.e("初始化失败")
        } else {
            while (!isCancel) {
                try {
                    val socket = serverSocket.accept()
                    if (!isCancel) {
                        L.i("客户端IP地址:" + socket.remoteSocketAddress)
                        receiveListener?.onAccept(socket)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            L.i("...WifiP2p Socket Server执行结束.")
        }
    }

    /**取消*/
    fun cancel() {
        isCancel = true
        try {
            _serverSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**启动线程*/
    fun start() {
        val thread = Thread(this, "WifiP2pReceive-$port")
        thread.start()
    }

}

