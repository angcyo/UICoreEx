package com.angcyo.canvas2.laser.pecker.dslitem.control

import com.angcyo.canvas.render.element.TextElement
import com.angcyo.canvas.render.util.renderElement
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.library.ex._string
import com.angcyo.widget.DslViewHolder

/**
 * 字体选择触发Item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/17
 */
class TextTypefaceSelectItem : CanvasIconItem() {

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
            val renderElement = itemRenderer?.renderElement
            if (renderElement is TextElement) {
                typeface = renderElement.textPaint.typeface
            }
        }
    }

}