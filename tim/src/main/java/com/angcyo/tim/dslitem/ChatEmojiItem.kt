package com.angcyo.tim.dslitem

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.tim.R
import com.angcyo.tim.bean.Emoji
import com.angcyo.widget.DslViewHolder

/**
 * 显示表情的item
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/11
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class ChatEmojiItem : DslAdapterItem() {

    var itemEmoji: Emoji? = null

    init {
        itemLayoutId = R.layout.chat_emoji_item
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.img(R.id.lib_image_view)?.setImageBitmap(itemEmoji?.icon)
    }

}