package com.angcyo.acc2.app.dslitem

import com.angcyo.acc2.app.R
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.widget.DslViewHolder

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/02/06
 * Copyright (c) 2020 angcyo. All rights reserved.
 */
class AppOpStatisticsItem : DslAdapterItem() {

    companion object {
        const val ITEM_TAG = "OpStatisticsItem"
    }

    var statisticsText: CharSequence? = null

    init {
        itemLayoutId = R.layout.app_op_statistics_item
        itemTag = ITEM_TAG
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.tv(R.id.lib_text_view)?.apply {
            text = statisticsText
        }
    }
}