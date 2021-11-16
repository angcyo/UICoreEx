package com.angcyo.tim.dslitem

import com.angcyo.tim.R
import com.angcyo.tim.util.handlerEmojiText
import com.angcyo.widget.DslViewHolder

/**
 * 文本消息item
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/12
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class MsgTextItem : BaseChatMsgItem() {

    init {
        msgContentLayoutId = R.layout.msg_text_layout
    }

    override fun bindMsgContent(itemHolder: DslViewHolder, itemPosition: Int, payloads: List<Any>) {
        super.bindMsgContent(itemHolder, itemPosition, payloads)
        itemHolder.tv(R.id.msg_text_view)?.text = messageInfoBean?.content?.handlerEmojiText()
    }

}