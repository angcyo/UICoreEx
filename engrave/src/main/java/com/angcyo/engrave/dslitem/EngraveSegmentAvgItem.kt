package com.angcyo.engrave.dslitem

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.widget.DslViewHolder

/**
 * Segment 等宽均分 item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/27
 */
open class EngraveSegmentAvgItem : EngraveSegmentScrollItem() {

    init {
        itemLayoutId = R.layout.item_engrave_layer_config_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
    }

    override fun onItemChangeListener(item: DslAdapterItem) {
        super.onItemChangeListener(item)
    }

}