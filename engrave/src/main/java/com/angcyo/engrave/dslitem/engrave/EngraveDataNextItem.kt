package com.angcyo.engrave.dslitem.engrave

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.item.DslButtonItem
import com.angcyo.library.ex.ClickAction
import com.angcyo.widget.DslViewHolder

/**
 * 雕刻前, 下一步按钮item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/02
 */
class EngraveDataNextItem : DslButtonItem() {

    /**开始预览*/
    var itemPreviewAction: ClickAction? = null

    init {
        itemLayoutId = R.layout.item_engrave_data_next
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.visible(R.id.lib_preview_button, itemPreviewAction != null)
        itemHolder.click(R.id.lib_preview_button, itemPreviewAction)
    }

}