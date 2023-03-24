package com.angcyo.websocket.data

import com.angcyo.websocket.WSClient
import com.angcyo.websocket.data.WSServerStateInfo.Companion.STATE_ERROR
import com.angcyo.websocket.data.WSServerStateInfo.Companion.STATE_OPEN
import org.java_websocket.handshake.ServerHandshake

/**
 * 与服务端的状态
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2023/03/24
 * Copyright (c) 2020 angcyo. All rights reserved.
 */
data class WSServerStateInfo(
    /**客户端*/
    val client: WSClient,
    /**状态*/
    val state: Int = STATE_NORMAL,
    /**连接成功状态时的信息
     * [STATE_OPEN]*/
    val serverHandshake: ServerHandshake? = null,
    /**错误状态时的信息
     * [STATE_ERROR]*/
    val error: Exception? = null,
) {
    companion object {
        const val STATE_NORMAL = 0
        const val STATE_OPEN = 1
        const val STATE_CLOSE = 2
        const val STATE_ERROR = 2
    }
}
