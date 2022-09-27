package com.angcyo.material.dslitem

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.DslBaseEditItem
import com.angcyo.item.style.itemEditDigits
import com.angcyo.item.style.itemEditHint
import com.angcyo.item.style.itemInputFilterList
import com.angcyo.item.style.itemMaxInputLength
import com.angcyo.material.R
import com.angcyo.widget.DslViewHolder

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/18
 */
class DslTextInputLayoutItem : DslBaseEditItem() {

    init {
        itemLayoutId = R.layout.dsl_text_input_layout_item

        itemEditHint
        itemMaxInputLength
        itemInputFilterList
        itemEditDigits
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
        /*itemHolder.v<BufferTextInputLayout>(R.id.lib_buffer_input_layout)?.apply {
            hint = itemEditHint
            counterMaxLength = itemMaxInputLength
        }*/
    }

}