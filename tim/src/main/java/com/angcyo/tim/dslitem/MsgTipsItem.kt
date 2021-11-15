package com.angcyo.tim.dslitem

import com.angcyo.tim.R
import com.angcyo.widget.DslViewHolder

/**
 * 消息轻提示item, xxx撤回了消息
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/15
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class MsgTipsItem : BaseChatMsgEmptyItem() {

    init {
        msgContentLayoutId = R.layout.msg_tips_layout
    }

    override fun bindMsgContent(itemHolder: DslViewHolder, itemPosition: Int, payloads: List<Any>) {
        super.bindMsgContent(itemHolder, itemPosition, payloads)
        itemHolder.tv(R.id.msg_tips_view)?.text = messageInfoBean?.content
    }
}