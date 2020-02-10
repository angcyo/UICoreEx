package com.angcyo.agora.rtm

import io.agora.rtm.RtmChannelAttribute
import io.agora.rtm.RtmChannelListener
import io.agora.rtm.RtmChannelMember
import io.agora.rtm.RtmMessage

/**
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/02/07
 */
open class RtmChannelListenerHandler : RtmChannelListener {

    /**频道属性更新回调。返回所在频道的所有属性。*/
    override fun onAttributesUpdated(attributeList: List<RtmChannelAttribute>) {
        //L.d(attributeList)
    }

    /**收到频道消息回调。*/
    override fun onMessageReceived(message: RtmMessage, fromMember: RtmChannelMember) {
        //L.d(fromMember, message)
    }

    /**远端用户加入频道回调。*/
    override fun onMemberJoined(member: RtmChannelMember) {
        //L.d(member)
    }

    /**频道成员离开频道回调。*/
    override fun onMemberLeft(member: RtmChannelMember) {
        //L.d(member)
    }

    /**频道成员人数更新回调。返回最新频道成员人数。*/
    override fun onMemberCountUpdated(memberCount: Int) {
        //L.d(memberCount)
    }

}