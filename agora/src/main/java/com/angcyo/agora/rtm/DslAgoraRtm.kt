package com.angcyo.agora.rtm

import android.content.Context
import com.angcyo.agora.Agora
import com.angcyo.agora.rtc.AgoraConfig
import com.angcyo.agora.rtc.initRtm
import com.angcyo.library.L
import io.agora.rtm.*


/**
 * 声网rtm, 实时消息
 *
 * https://docs.agora.io/cn/Real-time-Messaging/messaging_android?platform=Android#sdk
 *
 * 1.初始化
 * [com.angcyo.agora.rtm.DslAgoraRtm.initRtm]
 *
 * 2.登录到RTM系统
 * [com.angcyo.agora.rtm.DslAgoraRtm.login]
 *
 * 3.发送点对点消息 或者 创建频道,加入频道,发送频道消息
 * [com.angcyo.agora.rtm.DslAgoraRtm.sendPeerMessage] or [com.angcyo.agora.rtm.DslAgoraRtm.createAndJoinChannel]
 *
 * [com.angcyo.agora.rtm.DslAgoraRtmKt.sendChannelMessage]
 *
 * 4.退出, 释放资源
 * [io.agora.rtm.RtmChannel.leave]
 * [com.angcyo.agora.rtm.DslAgoraRtm.logout]
 * [com.angcyo.agora.rtm.DslAgoraRtm.release]
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/02/07
 */

object DslAgoraRtm {

    var _rtmClient: RtmClient? = null

    fun initRtm(
        context: Context,
        appId: String,
        agoraConfig: AgoraConfig = AgoraConfig(),
        config: RtmClient.() -> Unit = {}
    ) {
        release()
        _rtmClient = RtmClient.createInstance(
            context.applicationContext, appId,
            RtmClientListenerHandler()
        )
        _rtmClient?.apply {
            agoraConfig.initRtm(this)
            config()
        }
        Agora.appId = appId
        Agora.context = context.applicationContext
    }

    /**释放资源*/
    fun release() {
        logout()
        _rtmClient?.release()
    }

    /**登出*/
    fun logout() {
        _rtmClient?.logout(null)
    }

    /**登录账号*/
    fun login(
        userId: Long,
        token: String? = null,
        onResult: (errorInfo: ErrorInfo?) -> Unit = { L.d(it) }
    ) {
        _rtmClient?.login(token, userId.toString(), RtmResultCallback(onResult))
    }

    /**发送点对点消息 https://docs.agora.io/cn/Real-time-Messaging/messaging_android?platform=Android#点对点消息*/
    fun sendPeerMessage(
        userId: Long,
        text: String,
        onResult: (errorInfo: ErrorInfo?) -> Unit = { L.d(it) }
    ) {
        _rtmClient?.run {
            val message: RtmMessage = createMessage()
            message.text = text

            val option = SendMessageOptions()
            option.enableOfflineMessaging = true
            option.enableHistoricalMessaging = true

            sendMessageToPeer(userId.toString(), message, option, RtmResultCallback(onResult))
        }
    }

    /**
     * 创建通道
     * 每个客户端实例最多只能同时加入 20 个频道。
     * https://docs.agora.io/cn/Real-time-Messaging/messaging_android?platform=Android#频道消息
     *
     * 通道名, 不能包含中文.
     *  */
    fun createAndJoinChannel(
        channelName: String,
        listener: RtmChannelListener = ChannelListenerDispatch(channelName),
        onResult: (rtmChannel: RtmChannel, errorInfo: ErrorInfo?) -> Unit = { rtmChannel, errorInfo ->
            L.d("$rtmChannel $errorInfo")
        }
    ): RtmChannel? {
        return _rtmClient?.run {
            createChannel(channelName, listener).apply {
                join(RtmResultCallback {
                    onResult.invoke(this, it)
                })
            }
        }
    }
}

/**
 * 收发频道消息
 * https://docs.agora.io/cn/Real-time-Messaging/messaging_android?platform=Android#开发注意事项
 * */
fun RtmChannel.sendChannelMessage(
    text: String?,
    onResult: (errorInfo: ErrorInfo?) -> Unit = { L.d(it) }
) {
    DslAgoraRtm._rtmClient?.run {
        val message: RtmMessage = createMessage()
        message.text = text
        sendMessage(message, RtmResultCallback(onResult))
    }
}