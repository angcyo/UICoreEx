package com.angcyo.canvas2.laser.pecker.engrave.dslitem

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.DslSegmentSolidTabItem
import com.angcyo.item.style.itemCurrentIndex
import com.angcyo.widget.DslViewHolder

/**
 * Segment 支持滚动 item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/01
 */
open class EngraveSegmentScrollItem : DslSegmentSolidTabItem() {

    /**描述文本*/
    var itemText: CharSequence? = null

    init {
        itemLayoutId = R.layout.item_engrave_data_px
        itemSegmentLayoutId = R.layout.layout_engrave_segment
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.tv(R.id.lib_text_view)?.text = itemText
    }

    override fun onItemChangeListener(item: DslAdapterItem) {
        super.onItemChangeListener(item)
        itemCurrentIndex
    }

}