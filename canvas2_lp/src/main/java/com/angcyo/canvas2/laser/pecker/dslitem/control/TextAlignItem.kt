package com.angcyo.canvas2.laser.pecker.dslitem.control

import android.graphics.Paint
import com.angcyo.canvas.render.element.toAlignString
import com.angcyo.canvas.render.util.textElement
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.widget.DslViewHolder

/**
 * 改变文本对齐方向的item, 并且支持多类型互斥
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/10
 */
class TextAlignItem : BaseTextControlItem() {

    var itemAlign: Paint.Align = Paint.Align.LEFT

    init {
        itemLayoutId = R.layout.item_canvas_icon_horizontal_layout

        itemSingleSelectMutex = true
        itemClick = {
            updateTextProperty {
                textAlign = itemAlign.toAlignString()
            }
        }
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        itemIsSelected =
            itemRenderer?.textElement?.textProperty?.textAlign == itemAlign.toAlignString()
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
    }

}