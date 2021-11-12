package com.angcyo.tim.ui.chat

import com.angcyo.tim.chat.GroupChatPresenter

/**
 * 群聊界面
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/11
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
open class GroupChatFragment : BaseChatFragment() {

    init {
        chatPresenter = GroupChatPresenter()
    }

    override fun onInitChat() {
        super.onInitChat()

        chatPresenter?.initView(this)
    }

}