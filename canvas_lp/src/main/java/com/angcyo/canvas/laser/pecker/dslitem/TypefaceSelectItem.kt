package com.angcyo.canvas.laser.pecker.dslitem

import com.angcyo.canvas.items.data.DataItemRenderer
import com.angcyo.canvas.laser.pecker.R
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.library.ex._string
import com.angcyo.widget.DslViewHolder

/**
 * 字体选择触发Item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/17
 */
class TypefaceSelectItem : CanvasControlItem2() {

    init {
        itemIco = R.drawable.canvas_text_font_ico
        itemText = _string(R.string.canvas_font)

        itemLayoutId = R.layout.item_canvas_typeface_select_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
        itemHolder.tv(R.id.typeface_view)?.apply {
            val renderer = itemRenderer
            typeface = if (renderer is DataItemRenderer) {
                renderer.dataTextItem?.itemPaint?.typeface
            } else {
                renderer?.paint?.typeface
            }
        }
    }

}