package com.angcyo.engrave.canvas.dslitem

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.widget.DslViewHolder

/**
 * 画布数字自增/自减输入item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/14
 */
class CanvasNumberIncrementItem : DslAdapterItem() {

    /**回调*/
    var itemIncrementAction: (plus: Boolean) -> Unit = {

    }

    init {
        itemClickThrottleInterval = 0
        itemLayoutId = R.layout.canvas_number_increment_item_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.click(R.id.minus_increment_view) {
            itemIncrementAction(false)
        }
        itemHolder.click(R.id.plus_increment_view) {
            itemIncrementAction(true)
        }
    }
}