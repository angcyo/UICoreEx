package com.angcyo.tim.bean

import com.angcyo.tim.helper.ConversationHelper
import com.tencent.imsdk.v2.V2TIMConversation
import com.tencent.imsdk.v2.V2TIMMessage
import java.io.Serializable

/**
 * 会话列表中需要的数据结构
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/10
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class ConversationInfoBean : Serializable, Comparable<ConversationInfoBean> {

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
    var isTop: Boolean = false

    /**排序key*/
    var orderKey: Long = 0

    /**
     * 从小到大排序
     * */
    override fun compareTo(other: ConversationInfoBean): Int {
        return if (this.isTop && !other.isTop) {
            -1
        } else if (!this.isTop && other.isTop) {
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

/**用户id*/
val ConversationInfoBean.userId: String?
    get() = conversation?.userID

/**群组id*/
val ConversationInfoBean.groupId: String?
    get() = conversation?.groupID

/**聊天id*/
val ConversationInfoBean.chatId: String?
    get() = if (conversation?.type == V2TIMConversation.V2TIM_C2C) {
        conversation?.userID
    } else {
        conversation?.groupID
    }

fun V2TIMConversation.toConversationInfoBean(): ConversationInfoBean? =
    ConversationHelper.convertToConversationBean(this)