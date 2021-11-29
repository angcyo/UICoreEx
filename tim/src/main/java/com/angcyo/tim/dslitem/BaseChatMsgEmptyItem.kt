package com.angcyo.tim.dslitem

import com.angcyo.tim.R
import com.angcyo.widget.DslViewHolder

/**
 * 仅有消息的时间和消息体, 没有消息的左右2个头像和消息状态
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/15
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
abstract class BaseChatMsgEmptyItem : BaseChatMsgItem() {

    init {
        itemLayoutId = R.layout.chat_msg_empty_item
    }

    override fun bindMsgStyle(itemHolder: DslViewHolder, itemPosition: Int, payloads: List<Any>) {
        super.bindMsgStyle(itemHolder, itemPosition, payloads)
        clearMsgBackground(itemHolder)
    }
}