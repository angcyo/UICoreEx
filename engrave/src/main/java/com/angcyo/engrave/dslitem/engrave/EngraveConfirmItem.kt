package com.angcyo.engrave.dslitem.engrave

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.library.ex.ClickAction

import com.angcyo.widget.DslViewHolder

/**
 * 雕刻确定item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/02
 */
class EngraveConfirmItem : DslAdapterItem() {

    /**雕刻回调*/
    var engraveAction: ClickAction? = null

    init {
        itemLayoutId = R.layout.item_engrave_confirm_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.throttleClick(R.id.lib_button) {
            engraveAction?.invoke(it)
        }
    }
}