package com.angcyo.canvas.laser.pecker.dslitem

import android.graphics.Paint
import com.angcyo.canvas.items.data.DataItemRenderer
import com.angcyo.canvas.laser.pecker.R
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.widget.DslViewHolder

/**
 * 改变文本对齐方向的item, 并且支持多类型互斥
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/10
 */
class TextAlignItem : CanvasControlItem2() {

    var itemAlign: Paint.Align = Paint.Align.LEFT

    init {
        itemLayoutId = R.layout.item_canvas_icon_horizontal_layout

        itemSingleSelectMutex = true
        itemClick = {
            itemRenderer?.let { renderer ->
                if (renderer is DataItemRenderer) {
                    renderer.dataTextItem?.updatePaintAlign(itemAlign, renderer)
                }
                updateAdapterItem()
            }
        }
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        val renderer = itemRenderer
        if (renderer is DataItemRenderer) {
            itemIsSelected = renderer.dataTextItem?.itemPaint?.textAlign == itemAlign
        }

        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
    }

}