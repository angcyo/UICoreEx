package com.angcyo.engrave.dslitem.engrave

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.library.ex.Action
import com.angcyo.widget.DslViewHolder

/**
 * 材质选择item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/12/15
 */
class EngraveMaterialWheelItem : EngraveOptionWheelItem() {

    /**是否要限制保存按钮*/
    var itemShowSaveButton: Boolean = false

    /**保存的回调*/
    var itemSaveAction: Action? = null

    init {
        itemLayoutId = R.layout.item_engrave_material_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.visible(R.id.lib_save_button, itemShowSaveButton)
        itemHolder.click(R.id.lib_save_button) {
            //保存材质
            itemSaveAction?.invoke()
        }
    }

}