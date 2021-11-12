package com.angcyo.tim.bean

import com.tencent.imsdk.v2.V2TIMConversation

/**
 * 开始聊天时, 需要的数据结构
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/11
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class ChatInfoBean {

    /**聊天的标题*/
    var chatTitle: String? = null

    /**草稿信息*/
    var draftInfoBean: DraftInfoBean? = null

    /**最后一条消息*/
    var lastMessageInfoBean: MessageInfoBean? = null

    /**聊天类型*/
    var chatType: Int = V2TIMConversation.V2TIM_C2C

    /**聊天的唯一标识符
     * 如果是 单聊, 则应该是对方的 userId
     * 如果是 群聊, 应该是 groupId
     * */
    var chatId: String? = null
}

/**是否是群聊*/
val ChatInfoBean.isGroup: Boolean
    get() = chatType == V2TIMConversation.V2TIM_GROUP

fun ConversationInfoBean.toChatInfoBean(): ChatInfoBean {
    val chatInfoBean = ChatInfoBean()

    chatInfoBean.chatTitle = title
    chatInfoBean.draftInfoBean = draftInfo
    chatInfoBean.lastMessageInfoBean = messageInfoBean

    val type = conversation?.type
    chatInfoBean.chatType = type ?: V2TIMConversation.V2TIM_C2C

    //聊天id
    chatInfoBean.chatId = if (type == V2TIMConversation.V2TIM_C2C) {
        conversation?.userID
    } else {
        conversation?.groupID
    }

    return chatInfoBean
}