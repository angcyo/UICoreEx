package com.angcyo.engrave.dslitem.engrave

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.widget.DslViewHolder

/**
 * 雕刻完成后的控制item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/27
 */
class EngraveFinishControlItem : DslAdapterItem() {

    var itemShareAction: () -> Unit = {}

    var itemAgainAction: () -> Unit = {}

    init {
        itemLayoutId = R.layout.item_engrave_finish_control_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.click(R.id.share_button) {
            itemShareAction()
        }

        itemHolder.click(R.id.again_button) {
            itemAgainAction()
        }
    }

}