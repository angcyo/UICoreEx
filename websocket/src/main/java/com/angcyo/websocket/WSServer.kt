package com.angcyo.websocket

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.angcyo.core.vmApp
import com.angcyo.library.L
import com.angcyo.library.component.NetUtils
import com.angcyo.library.component.Port
import com.angcyo.websocket.data.WSClientInfo
import org.java_websocket.WebSocket
import org.java_websocket.exceptions.WebsocketNotConnectedException
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import kotlin.concurrent.thread

/**
 * WebSocket 服务端
 * https://github.com/TooTallNate/Java-WebSocket/wiki#server-example
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/08/19
 */
open class WSServer(address: InetSocketAddress) : WebSocketServer(address) {

    companion object {

        /**保存服务实例*/
        val serverMap = hashMapOf<Int, WSServer>()

        /**默认端口*/
        const val DEFAULT_PORT = 9300

        /**[android.os.NetworkOnMainThreadException]*/
        fun startWebSocketServer(port: Int = DEFAULT_PORT, action: (WSServer) -> Unit) {
            val p = Port.generatePort(port)
            thread(name = "WSServer-$p") {
                val server = WSServer(InetSocketAddress(p))
                action(server)
                server.run()
            }
        }

        /**绑定生命周期*/
        fun bindWebSocketServer(
            lifecycleOwner: LifecycleOwner,
            port: Int = DEFAULT_PORT,
            action: (WSServer) -> Unit = {}
        ) {
            var server: WSServer? = null
            lifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    when (event) {
                        Lifecycle.Event.ON_CREATE -> startWebSocketServer(port) {
                            server = it
                            action(it)
                        }
                        Lifecycle.Event.ON_DESTROY -> server?.stop()
                        else -> Unit
                    }
                }
            })
        }
    }

    val wsServerModel = vmApp<WSServerModel>()

    init {

    }

    /**
     * ws://192.168.31.152:9301/test
     * WebSocketServer 连接:/192.168.31.152:9301 /192.168.31.192:62160 /test
     * */
    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        L.i("WebSocketServer 连接:${conn.localSocketAddress} ${conn.remoteSocketAddress} ${handshake.resourceDescriptor}")
        val clientInfo = WSClientInfo(this, conn, handshake)
        wsServerModel.addClient(clientInfo)
    }

    override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
        L.w("WebSocketServer 关闭:${conn.remoteSocketAddress} $code $reason $remote")
        wsServerModel.removeClient(conn)
    }

    /**
     * WebSocketServer 消息:org.java_websocket.WebSocketImpl@38d6a55 测试发送的消息
     * */
    override fun onMessage(conn: WebSocket, message: String?) {
        L.i("WebSocketServer 消息:${conn.remoteSocketAddress} $message")
        wsServerModel.onMessage(conn, message)
    }

    override fun onError(conn: WebSocket, ex: Exception) {
        L.e("WebSocketServer 异常:${conn.remoteSocketAddress} $ex")
        ex.printStackTrace()
    }

    /**服务地址*/
    val serverAddress: String
        get() = "ws:/${NetUtils.localIPAddress}:${port}"

    override fun onStart() {
        L.i("WebSocketServer 已启动->$serverAddress")
        serverMap[port] = this
    }

    override fun run() {
        super.run()
        L.w("WebSocketServer 启动结束!")
        serverMap.remove(port)
        wsServerModel.stopServer(this)
    }

    override fun stop() {
        super.stop()
    }

    override fun stop(timeout: Int, closeMessage: String?) {
        super.stop(timeout, closeMessage)
        L.w("WebSocketServer 停止:${timeout} $closeMessage")
    }

    override fun getConnections(): MutableCollection<WebSocket> {
        return super.getConnections()
    }

    override fun setTcpNoDelay(tcpNoDelay: Boolean) {
        super.setTcpNoDelay(tcpNoDelay)
    }

    //region ---method---

    /**发送数据给所有客户端*/
    fun sendMessage(bytes: ByteArray) {
        for (socket in connections) {
            try {
                socket.send(bytes)
            } catch (e: WebsocketNotConnectedException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**发送数据给所有客户端*/
    fun sendMessage(text: String?) {
        for (socket in connections) {
            try {
                socket.send(text)
            } catch (e: WebsocketNotConnectedException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**发送数据给所有客户端*/
    fun sendMessage(bytes: ByteBuffer) {
        for (socket in connections) {
            try {
                socket.send(bytes)
            } catch (e: WebsocketNotConnectedException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    //endregion ---method---
}

/**启动一个[WebSocket]服务*/
fun LifecycleOwner.bindWebSocketServer(
    port: Int = WSServer.DEFAULT_PORT,
    action: (WSServer) -> Unit = {}
) {
    WSServer.bindWebSocketServer(this, port, action)
}