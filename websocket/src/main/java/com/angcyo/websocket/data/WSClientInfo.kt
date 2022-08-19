package com.angcyo.websocket.data

import com.angcyo.websocket.WSServer
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake

/**
 * 客户端信息
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/08/19
 */
data class WSClientInfo(
    /**服务端*/
    val server: WSServer,
    /**客户端*/
    val client: WebSocket,
    /**握手信息*/
    val handshake: ClientHandshake
)