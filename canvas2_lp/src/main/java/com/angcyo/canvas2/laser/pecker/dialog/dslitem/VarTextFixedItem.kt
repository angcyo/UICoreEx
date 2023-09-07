package com.angcyo.canvas2.laser.pecker.dialog.dslitem

import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.style.EditItemConfig
import com.angcyo.item.style.IEditItem
import com.angcyo.item.style.itemEditText
import com.angcyo.item.style.itemMaxInputLength
import com.angcyo.widget.DslViewHolder

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/04
 */
class VarTextFixedItem : BaseVarItem(), IEditItem {

    override var editItemConfig: EditItemConfig = EditItemConfig()

    init {
        itemLayoutId = R.layout.item_var_text_fixed_layout
        itemMaxInputLength = HawkEngraveKeys.maxInputTextLengthLimit
        itemMaxInputLength
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
        //super.onItemChangeListener(item)
        _itemVariableBean?.content = itemEditText?.toString() ?: ""
    }
}