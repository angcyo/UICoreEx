package com.angcyo.tim.model

import android.content.Intent
import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.Observer
import com.angcyo.core.lifecycle.LifecycleViewModel
import com.angcyo.core.vmApp
import com.angcyo.library.component.DslNotify
import com.angcyo.library.component.dslNotify
import com.angcyo.library.component.high
import com.angcyo.library.component.single
import com.angcyo.library.ex.str
import com.angcyo.tim.bean.ChatInfoBean
import com.angcyo.tim.bean.MessageInfoBean
import com.angcyo.tim.bean.chatId
import com.angcyo.tim.bean.showUserName
import com.angcyo.tim.bean.toChatInfoBean
import com.angcyo.tim.helper.ConversationHelper
import com.angcyo.tim.util.handlerEmojiText

/**
 * 新消息的通知栏提醒控制
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/12/30
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */

/**消息通知的配置*/
typealias MessageNotifyConfig = (DslNotify, MessageInfoBean) -> Unit

/**消息通知拦截, 拦截后, 不显示通知*/
typealias MessageNotifyIntercept = (MessageInfoBean) -> Boolean

class MessageNotifyModel : LifecycleViewModel() {

    /**消息通知的图片转换器*/
    var messageNotifyConfig: MessageNotifyConfig? = null

    /**拦截器*/
    val messageNotifyInterceptList = mutableListOf<MessageNotifyIntercept>()

    /**是否关闭消息通知*/
    var closeMessageNotify = false

    /**当前正在聊天的对象*/
    var currentChatId: String? = null

    val _notifyIdMap = hashMapOf<String, Int>()

    companion object {

        /**默认的消息通知通道*/
        var DEFAULT_MESSAGE_CHANNEL = "IM Message"

        const val KEY_MESSAGE_NOTIFY = "key_message_notify"

        /**配置聊天的[Intent]*/
        fun chatIntentConfig(intent: Intent, messageInfoBean: MessageInfoBean): Intent {
            intent.putExtra(KEY_MESSAGE_NOTIFY, messageInfoBean.toChatInfoBean())
            return intent
        }

        /**解析聊天的[Intent]*/
        fun parseChatIntent(caller: ActivityResultCaller, intent: Intent): Boolean {
            val chatInfoBean: ChatInfoBean =
                intent.getSerializableExtra(KEY_MESSAGE_NOTIFY) as ChatInfoBean? ?: return false

            if (vmApp<MessageNotifyModel>().currentChatId == chatInfoBean.chatId) {
                return false
            }

            ConversationHelper.conversationJump(caller, chatInfoBean)
            return true
        }
    }

    /**新消息*/
    val newMessageObserver = Observer<MessageInfoBean?> { messageInfoBean ->

        if (closeMessageNotify) {
            return@Observer
        }

        if (messageInfoBean != null) {
            val chatId = messageInfoBean.chatId.str("default")

            if (currentChatId == chatId) {
                return@Observer
            }

            //拦截处理
            var isIntercept = false
            for (intercept in messageNotifyInterceptList) {
                if (intercept.invoke(messageInfoBean)) {
                    isIntercept = true
                    break
                }
            }

            if (!isIntercept) {
                //取消之前的通知
                cancelChatNotify(chatId)

                val notifyId = dslNotify {
                    channelName = DEFAULT_MESSAGE_CHANNEL
                    single(
                        messageInfoBean.showUserName,
                        messageInfoBean.content?.handlerEmojiText()
                    )
                    high()
                    messageNotifyConfig?.invoke(this, messageInfoBean)
                }
                _notifyIdMap[chatId] = notifyId
            }
        }
    }

    fun init() {
        vmApp<ChatModel>().newMessageInfoData.observeForever(newMessageObserver)
    }

    override fun release(data: Any?) {
        super.release(data)
        vmApp<ChatModel>().newMessageInfoData.removeObserver(newMessageObserver)
        DslNotify.cancelNotifyList(_notifyIdMap.values)
        _notifyIdMap.clear()
    }

    /**取消聊天的通知*/
    fun cancelChatNotify(chatId: String?) {
        val key = chatId ?: ""
        _notifyIdMap[key]?.let {
            DslNotify.cancelNotify(it)
            _notifyIdMap.remove(key)
        }
    }

}