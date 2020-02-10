package com.angcyo.agora.rtm

import com.angcyo.library.L
import io.agora.rtm.RtmChannelAttribute
import io.agora.rtm.RtmChannelListener
import io.agora.rtm.RtmChannelMember
import io.agora.rtm.RtmMessage
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * https://docs.agora.io/cn/Real-time-Messaging/API%20Reference/RTM_java/interfaceio_1_1agora_1_1rtm_1_1_rtm_channel_listener.html#ad778e702e026a79460f45a992bb8576d
 *
 * 子线程回调
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/02/09
 */
class ChannelListenerDispatch(val channelName: String) : RtmChannelListener {

    companion object {
        private val listenerMap: ConcurrentHashMap<String, CopyOnWriteArrayList<RtmChannelListenerHandler>> =
            ConcurrentHashMap()

        /**监听指定频道的事件*/
        fun addListener(channelName: String?, listener: RtmChannelListenerHandler) {
            if (channelName.isNullOrBlank()) {
                return
            }
            val list = listenerMap[channelName]
            if (list == null) {
                listenerMap[channelName] = CopyOnWriteArrayList()
            }
            listenerMap[channelName]?.add(listener)
        }

        /**移除指定频道的事件*/
        fun removeListener(channelName: String?, listener: RtmChannelListenerHandler) {
            if (channelName.isNullOrBlank()) {
                return
            }
            listenerMap[channelName]?.remove(listener)
        }
    }

    val listeners: CopyOnWriteArrayList<RtmChannelListenerHandler>?
        get() = listenerMap[channelName]

    /**频道属性更新回调。返回所在频道的所有属性。*/
    override fun onAttributesUpdated(attributeList: List<RtmChannelAttribute>) {
        L.d(attributeList)
        listeners?.forEach {
            it.onAttributesUpdated(attributeList)
        }
    }

    /**收到频道消息回调。*/
    override fun onMessageReceived(message: RtmMessage, fromMember: RtmChannelMember) {
        L.d("\n频道:${fromMember.channelId} 用户:${fromMember.userId} 消息:${message.text} type:${message.messageType} offline:${message.isOfflineMessage} ts:${message.serverReceivedTs}")
        listeners?.forEach {
            it.onMessageReceived(message, fromMember)
        }
    }

    /**远端用户加入频道回调。*/
    override fun onMemberJoined(member: RtmChannelMember) {
        L.d("频道:${member.channelId} 用户:${member.userId} 加入")
        listeners?.forEach {
            it.onMemberJoined(member)
        }
    }

    /**频道成员离开频道回调。*/
    override fun onMemberLeft(member: RtmChannelMember) {
        L.d("频道:${member.channelId} 用户:${member.userId} 离开")
        listeners?.forEach {
            it.onMemberLeft(member)
        }
    }

    /**频道成员人数更新回调。返回最新频道成员人数。*/
    override fun onMemberCountUpdated(memberCount: Int) {
        L.d("频道人数更新:$memberCount")
        listeners?.forEach {
            it.onMemberCountUpdated(memberCount)
        }
    }
}

fun RtmChannelListenerHandler.addListener(channelName: String?) {
    ChannelListenerDispatch.addListener(channelName, this)
}

fun RtmChannelListenerHandler.removeListener(channelName: String?) {
    ChannelListenerDispatch.removeListener(channelName, this)
}