package com.angcyo.tim.dslitem

import com.angcyo.tim.R
import com.angcyo.widget.DslViewHolder

/**
 * 不知道的消息消息item
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/17
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class MsgUnknownItem : BaseChatMsgItem() {

    init {
        msgContentLayoutId = R.layout.msg_unknown_layout
    }

    override fun bindMsgContent(itemHolder: DslViewHolder, itemPosition: Int, payloads: List<Any>) {
        super.bindMsgContent(itemHolder, itemPosition, payloads)
        itemHolder.tv(R.id.msg_text_view)?.text = "[不支持的消息类型]"
    }
}