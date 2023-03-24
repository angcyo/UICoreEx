package com.angcyo.websocket

import com.angcyo.core.vmApp
import com.angcyo.library.L
import com.angcyo.viewmodel.updateValue
import com.angcyo.websocket.data.WSServerMessageInfo
import com.angcyo.websocket.data.WSServerStateInfo
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.nio.ByteBuffer

/**
 * WebSocket 客户端
 * https://github.com/TooTallNate/Java-WebSocket/blob/master/src/main/example/ChatClient.java
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2023/03/24
 * Copyright (c) 2020 angcyo. All rights reserved.
 */
open class WSClient(url: String) : WebSocketClient(URI(url)) {

    val wsClientModel = vmApp<WSClientModel>()

    init {
        //connect()//连接到服务, 会自动启动线程
        //run() //直接运行
        //send()//发送数据
        //close()//关闭连接
    }

    override fun onOpen(serverHandshake: ServerHandshake) {
        L.i("WSClient 连上服务器:${getURI()} $serverHandshake")
        wsClientModel.serverStateData.updateValue(
            WSServerStateInfo(
                this,
                WSServerStateInfo.STATE_OPEN,
                serverHandshake
            )
        )
    }

    override fun onMessage(message: ByteBuffer) {
        val bytes = message.array()
        L.i("WSClient 数据:${bytes.size}bytes")
        wsClientModel.serverMessageData.updateValue(WSServerMessageInfo(this, null, bytes))
    }

    override fun onMessage(message: String) {
        L.i("WSClient 数据:${message}")
        wsClientModel.serverMessageData.updateValue(WSServerMessageInfo(this, message))
    }

    override fun onClose(code: Int, reason: String, remote: Boolean) {
        L.w("WSClient 关闭连接:$code $reason $remote")
        wsClientModel.serverStateData.updateValue(
            WSServerStateInfo(this, WSServerStateInfo.STATE_CLOSE)
        )
    }

    override fun onError(ex: Exception) {
        L.e("WSClient 异常:$ex")
        wsClientModel.serverStateData.updateValue(
            WSServerStateInfo(this, WSServerStateInfo.STATE_ERROR, error = ex)
        )
    }

}