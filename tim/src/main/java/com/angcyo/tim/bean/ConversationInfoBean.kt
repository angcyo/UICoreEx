package com.angcyo.tim.bean

import com.angcyo.tim.chat.toMessageInfoBean
import com.angcyo.tim.conversation.ConversationHelper
import com.tencent.imsdk.v2.V2TIMConversation
import com.tencent.imsdk.v2.V2TIMGroupAtInfo
import com.tencent.imsdk.v2.V2TIMManager
import com.tencent.imsdk.v2.V2TIMMessage

/**
 * 会话列表中需要的数据结构
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/10
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class ConversationInfoBean : Comparable<ConversationInfoBean> {

    /**TIM SDK 原始的会话数据*/
    var conversation: V2TIMConversation? = null

    /**草稿信息*/
    var draftInfo: DraftInfoBean? = null

    /**最后一条消息的时间, 毫秒*/
    var lastMessageTime: Long = 0

    /**消息内容*/
    var messageInfoBean: MessageInfoBean? = null

    /**会话需要显示的标题*/
    var title: String? = null

    /**会话界面显示的@提示消息 */
    var atInfoText: String? = null

    /**未读消息数*/
    var unReadCount: Int = 0

    /**是否置顶*/
    var top: Boolean = false

    /**排序key*/
    var orderKey: Long = 0

    /**
     * 从小到大排序
     * */
    override fun compareTo(other: ConversationInfoBean): Int {
        return if (this.top && !other.top) {
            -1
        } else if (!this.top && other.top) {
            1
        } else {
            val thisOrderKey: Long = this.orderKey
            val otherOrderKey: Long = other.orderKey
            when {
                thisOrderKey > otherOrderKey -> -1
                thisOrderKey == otherOrderKey -> 0
                else -> 1
            }
        }
    }
}

/**是否是群聊会话*/
val ConversationInfoBean.isGroup: Boolean
    get() = conversation?.type == V2TIMConversation.V2TIM_GROUP

/**会话的id*/
val ConversationInfoBean.conversationId: String?
    get() = conversation?.conversationID

/**是否是请勿打扰*/
val ConversationInfoBean.isDisturb: Boolean
    get() = conversation?.recvOpt == V2TIMMessage.V2TIM_NOT_RECEIVE_MESSAGE

/**转换一下数据结构*/
fun V2TIMConversation.toConversationInfoBean(): ConversationInfoBean? {
    if (type == V2TIMConversation.V2TIM_C2C || type == V2TIMConversation.V2TIM_GROUP) {
        //单聊消息 和 群组消息

        val bean = ConversationInfoBean()
        bean.conversation = this

        //草稿
        if (!draftText.isNullOrEmpty()) {
            bean.draftInfo = DraftInfoBean(draftText, draftTimestamp * 1000)
        }

        //最后一条消息
        val lastMessage = lastMessage
        if (lastMessage != null) {
            bean.lastMessageTime = lastMessage.timestamp * 1000

            //转换
            bean.messageInfoBean = lastMessage.toMessageInfoBean()
        }

        bean.title = showName
        bean.top = isPinned == true
        bean.orderKey = orderKey

        bean.atInfoText = when (ConversationHelper.getAtInfoType(this)) {
            V2TIMGroupAtInfo.TIM_AT_ME -> "[有人@我]"
            V2TIMGroupAtInfo.TIM_AT_ALL -> "[@所有人]"
            V2TIMGroupAtInfo.TIM_AT_ALL_AT_ME -> "[@所有人][有人@我]"
            else -> null
        }

        if (groupType != V2TIMManager.GROUP_TYPE_AVCHATROOM) {
            //非直播群
            bean.unReadCount = unreadCount
        }

        return bean
    }

    return null
}