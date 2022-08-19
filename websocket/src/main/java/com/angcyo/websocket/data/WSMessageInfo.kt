package com.angcyo.websocket.data

/**
 * 收到客户端的消息
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/08/19
 */
data class WSMessageInfo(
    val clientInfo: WSClientInfo,
    /**消息*/
    val message: String?
)
