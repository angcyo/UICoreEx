package com.angcyo.agora.rtm

import com.angcyo.library.L
import io.agora.rtm.RtmClientListener
import io.agora.rtm.RtmMessage

/**
 *
 * https://docs.agora.io/cn/Real-time-Messaging/API%20Reference/RTM_java/interfaceio_1_1agora_1_1rtm_1_1_rtm_client_listener.html#ad7268dc770e30b3bf73bda2ef9e9dd5d
 *
 * 子线程回调
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/02/07
 */

class RtmClientListenerHandler : RtmClientListener {

    /**（SDK 断线重连时触发）当前使用的 RTM Token 已超过 24 小时的签发有效期。*/
    override fun onTokenExpired() {
        L.d()
    }

    /**被订阅用户在线状态改变回调。 */
    override fun onPeersOnlineStatusChanged(peersStatus: MutableMap<String, Int>) {
        L.d(peersStatus)
    }

    /**
     * SDK 与 Agora RTM 系统的连接状态发生改变回调。
     * @param state    新连接状态。详见 ConnectionState 。
     * @param reason    连接状态改变原因。详见 ConnectionChangeReason 。
     *
     * ConnectionState :Public Attributes
     * int 	CONNECTION_STATE_DISCONNECTED = 1
     * int 	CONNECTION_STATE_CONNECTING = 2
     * int 	CONNECTION_STATE_CONNECTED = 3
     * int 	CONNECTION_STATE_RECONNECTING = 4
     * int 	CONNECTION_STATE_ABORTED = 5
     *
     * ConnectionChangeReason :Public Attributes
     *  int 	CONNECTION_CHANGE_REASON_LOGIN = 1
     *  int 	CONNECTION_CHANGE_REASON_LOGIN_SUCCESS = 2
     *  int 	CONNECTION_CHANGE_REASON_LOGIN_FAILURE = 3
     *  int 	CONNECTION_CHANGE_REASON_LOGIN_TIMEOUT = 4
     *  int 	CONNECTION_CHANGE_REASON_INTERRUPTED = 5
     *  int 	CONNECTION_CHANGE_REASON_LOGOUT = 6
     *  int 	CONNECTION_CHANGE_REASON_BANNED_BY_SERVER = 7
     *  int 	CONNECTION_CHANGE_REASON_REMOTE_LOGIN = 8
     * */
    override fun onConnectionStateChanged(state: Int, reason: Int) {
        L.d(state.connectionState(), ":", reason.connectionChangeReason())
    }

    /**
     * 收到点对点消息回调。
     * @param message    被接收的消息。详见 RtmMessage 。
     * @param peerId    消息发送者的用户 ID。
     * */
    override fun onMessageReceived(message: RtmMessage, peerId: String) {
        L.d(peerId, ":", message)
    }
}

fun Int.connectionState(): String {
    return when (this) {
        1 -> "CONNECTION_STATE_DISCONNECTED"
        2 -> "CONNECTION_STATE_CONNECTING"
        3 -> "CONNECTION_STATE_CONNECTED"
        4 -> "CONNECTION_STATE_RECONNECTING"
        5 -> "CONNECTION_STATE_ABORTED"
        else -> "unknown"
    }
}

fun Int.connectionChangeReason(): String {
    return when (this) {
        1 -> "CONNECTION_CHANGE_REASON_LOGIN"
        2 -> "CONNECTION_CHANGE_REASON_LOGIN_SUCCESS"
        3 -> "CONNECTION_CHANGE_REASON_LOGIN_FAILURE"
        4 -> "CONNECTION_CHANGE_REASON_LOGIN_TIMEOUT"
        5 -> "CONNECTION_CHANGE_REASON_INTERRUPTED"
        6 -> "CONNECTION_CHANGE_REASON_LOGOUT"
        7 -> "CONNECTION_CHANGE_REASON_BANNED_BY_SERVER"
        8 -> "CONNECTION_CHANGE_REASON_REMOTE_LOGIN"
        else -> "unknown"
    }
}