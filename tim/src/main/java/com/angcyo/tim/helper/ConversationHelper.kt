package com.angcyo.tim.helper

import androidx.fragment.app.Fragment
import com.angcyo.base.dslFHelper
import com.angcyo.library.L
import com.angcyo.putDataSerializable
import com.angcyo.tim.bean.*
import com.angcyo.tim.ui.chat.GroupChatFragment
import com.angcyo.tim.ui.chat.SingleChatFragment
import com.tencent.imsdk.v2.V2TIMConversation
import com.tencent.imsdk.v2.V2TIMGroupAtInfo
import com.tencent.imsdk.v2.V2TIMManager

/**
 * 会话助手
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/10
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
object ConversationHelper {

    var conversationJumpListener = ConversationJumpListener()

    /**转换一下数据结构
     * [V2TIMConversation]->[ConversationInfoBean]*/
    fun convertToConversationBean(conversation: V2TIMConversation): ConversationInfoBean? {
        conversation.apply {
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

                bean.atInfoText = when (getAtInfoType(this)) {
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
            } else {
                L.w("不支持的会话类型:$type")
            }
        }
        return null
    }

    /**获取@的信息*/
    fun getAtInfoType(conversation: V2TIMConversation): Int {
        var atInfoType = 0
        var atMe = false
        var atAll = false
        val atInfoList = conversation.groupAtInfoList
        if (atInfoList == null || atInfoList.isEmpty()) {
            return V2TIMGroupAtInfo.TIM_AT_UNKNOWN
        }
        for (atInfo in atInfoList) {
            if (atInfo.atType == V2TIMGroupAtInfo.TIM_AT_ME) {
                atMe = true
                continue
            }
            if (atInfo.atType == V2TIMGroupAtInfo.TIM_AT_ALL) {
                atAll = true
                continue
            }
            if (atInfo.atType == V2TIMGroupAtInfo.TIM_AT_ALL_AT_ME) {
                atMe = true
                atAll = true
                continue
            }
        }
        atInfoType = if (atAll && atMe) {
            V2TIMGroupAtInfo.TIM_AT_ALL_AT_ME
        } else if (atAll) {
            V2TIMGroupAtInfo.TIM_AT_ALL
        } else if (atMe) {
            V2TIMGroupAtInfo.TIM_AT_ME
        } else {
            V2TIMGroupAtInfo.TIM_AT_UNKNOWN
        }
        return atInfoType
    }

    /**会话跳转*/
    fun conversationJump(fragment: Fragment, bean: ConversationInfoBean) {
        conversationJumpListener.conversationJump(fragment, bean)
    }

    /**会话跳转*/
    fun conversationJump(fragment: Fragment, bean: ChatInfoBean) {
        conversationJumpListener.conversationJump(fragment, bean)
    }
}

/**会话跳转监听*/
open class ConversationJumpListener {

    open fun conversationJump(fragment: Fragment, bean: ConversationInfoBean) {
        fragment.dslFHelper {
            show(if (bean.isGroup) GroupChatFragment::class.java else SingleChatFragment::class.java) {
                putDataSerializable(bean.toChatInfoBean())
            }
        }
    }

    open fun conversationJump(fragment: Fragment, bean: ChatInfoBean) {
        fragment.dslFHelper {
            show(if (bean.isGroup) GroupChatFragment::class.java else SingleChatFragment::class.java) {
                putDataSerializable(bean)
            }
        }
    }

}

fun V2TIMConversation.toConversationInfoBean(): ConversationInfoBean? =
    ConversationHelper.convertToConversationBean(this)