package com.angcyo.tim.ui.chat

import com.angcyo.tim.chat.SingleChatPresenter

/**
 * 单聊界面
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/11
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
open class SingleChatFragment : BaseChatFragment() {

    init {
        chatPresenter = SingleChatPresenter()
    }

    override fun onInitChat() {
        super.onInitChat()
    }
}