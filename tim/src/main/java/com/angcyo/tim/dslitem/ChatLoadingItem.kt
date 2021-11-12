package com.angcyo.tim.dslitem

import android.view.View
import android.widget.LinearLayout
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.UpdateAdapterProperty
import com.angcyo.tim.R
import com.angcyo.widget.DslViewHolder

/**
 * 聊天界面 加载聊天的item
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/12
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class ChatLoadingItem : DslAdapterItem() {

    var isLoading: Boolean by UpdateAdapterProperty(true)

    init {
        itemLayoutId = R.layout.chat_loading_item
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        val param = itemHolder.itemView.layoutParams
        if (isLoading) {
            param.width = LinearLayout.LayoutParams.MATCH_PARENT
            param.height = LinearLayout.LayoutParams.WRAP_CONTENT
            itemHolder.itemView.visibility = View.VISIBLE
        } else {
            param.height = 0
            param.width = 0
            itemHolder.itemView.visibility = View.GONE
        }
        itemHolder.itemView.layoutParams = param
    }

}