package com.angcyo.tim.dslitem

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.tim.R
import com.angcyo.tim.chat.MoreActionBean
import com.angcyo.widget.DslViewHolder

/**
 * 更多操作的item
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/11
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class ChatMoreActionItem : DslAdapterItem() {

    var moreActionBean: MoreActionBean? = null

    init {
        itemLayoutId = R.layout.chat_more_action_item
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.img(R.id.lib_image_view)?.setImageResource(moreActionBean?.iconResId ?: 0)
        itemHolder.tv(R.id.lib_text_view)?.text = moreActionBean?.title
    }

}