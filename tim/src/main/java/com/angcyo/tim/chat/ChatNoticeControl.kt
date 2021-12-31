package com.angcyo.tim.chat

import com.angcyo.core.vmApp
import com.angcyo.item.style.itemText
import com.angcyo.tim.R
import com.angcyo.tim.bean.groupId
import com.angcyo.tim.bean.toChatInfoBean
import com.angcyo.tim.bean.userId
import com.angcyo.tim.dslitem.ChatConnectTipItem
import com.angcyo.tim.dslitem.ChatOtherMessageTipItem
import com.angcyo.tim.helper.ConversationHelper
import com.angcyo.tim.model.ChatModel
import com.angcyo.tim.model.ConversationModel
import com.angcyo.tim.ui.chat.BaseChatFragment
import com.angcyo.widget.base.removeDslItem
import com.angcyo.widget.base.updateOrInsertDslItem

/**
 * 聊天界面, 无网络/其他消息时的提示布局控制
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/19
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class ChatNoticeControl : BaseChatControl() {

    /**SDK未连接时的提示*/
    val chatConnectTipItem = ChatConnectTipItem()

    /**其他会话的消息*/
    val chatOtherMessageTipItem = ChatOtherMessageTipItem()

    override fun initControl(fragment: BaseChatFragment) {
        super.initControl(fragment)

        val noticeWrapLayout = _vh?.group(R.id.msg_notice_wrap_layout)
        vmApp<ChatModel>().sdkConnectData.observe(fragment) {
            if (it) {
                //网络链接
                noticeWrapLayout?.removeDslItem(chatConnectTipItem)
            } else {
                //未连接
                noticeWrapLayout?.updateOrInsertDslItem(chatConnectTipItem, 0)
            }
        }

        vmApp<ConversationModel>().newConversationMessageData.observe(fragment) { bean ->
            if (bean != null) {
                val chatId = fragment.chatInfoBean?.chatId
                if (chatId != null && bean.userId != chatId && bean.groupId != chatId) {
                    //非正在聊天的对象
                    chatOtherMessageTipItem.itemText = buildString {
                        append(bean.title)
                        append(":")
                        append(bean.messageInfoBean?.content)
                    }

                    //先配置
                    chatOtherMessageTipItem.itemClick = {
                        ConversationHelper.conversationJump(fragment, bean.toChatInfoBean())
                        noticeWrapLayout?.removeDslItem(chatOtherMessageTipItem)
                    }

                    //后更新
                    noticeWrapLayout?.updateOrInsertDslItem(chatOtherMessageTipItem)
                }
            }
        }
    }
}