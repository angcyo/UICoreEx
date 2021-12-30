package com.angcyo.tim.model

import android.graphics.Bitmap
import androidx.lifecycle.Observer
import com.angcyo.core.lifecycle.LifecycleViewModel
import com.angcyo.core.vmApp
import com.angcyo.library.component.DslNotify
import com.angcyo.library.component.dslNotify
import com.angcyo.library.component.high
import com.angcyo.library.component.single
import com.angcyo.library.ex.str
import com.angcyo.tim.bean.MessageInfoBean
import com.angcyo.tim.bean.chatId
import com.angcyo.tim.bean.showUserName
import com.angcyo.tim.util.handlerEmojiText

/**
 * 新消息的通知栏提醒控制
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/12/30
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */

typealias MessageNotifyIconConvert = (MessageInfoBean) -> Bitmap?

class MessageNotifyModel : LifecycleViewModel() {

    /**消息通知的图片转换器*/
    var messageNotifyIconConvert: MessageNotifyIconConvert? = null

    /**是否关闭消息通知*/
    var closeMessageNotify = false

    /**当前正在聊天的对象*/
    var currentChatChatId: String? = null

    val _notifyIdMap = hashMapOf<String, Int>()

    /**新消息*/
    val newMessageObserver = Observer<MessageInfoBean> {

        if (closeMessageNotify) {
            return@Observer
        }

        if (it != null) {
            val chatId = it.chatId.str("default")

            if (currentChatChatId == chatId) {
                return@Observer
            }

            //取消之前的通知
            DslNotify.cancelNotify(_notifyIdMap[chatId])

            val notifyId = dslNotify {
                channelName = "IM Message"
                single(it.showUserName, it.content?.handlerEmojiText())
                high()
                notifyLargeIcon = messageNotifyIconConvert?.invoke(it)
            }
            _notifyIdMap[chatId] = notifyId
        }
    }

    fun init() {
        vmApp<ChatModel>().newMessageInfoData.observeForever(newMessageObserver)
    }

    override fun release() {
        super.release()
        vmApp<ChatModel>().newMessageInfoData.removeObserver(newMessageObserver)
    }

}