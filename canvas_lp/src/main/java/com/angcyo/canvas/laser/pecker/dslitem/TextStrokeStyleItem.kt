package com.angcyo.canvas.laser.pecker.dslitem

import android.graphics.Paint
import com.angcyo.canvas.items.PictureTextItem
import com.angcyo.canvas.items.renderer.PictureItemRenderer
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.widget.DslViewHolder

/**
 * 改变文本画笔风格的item, 并且支持互斥
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/10
 */
class TextStrokeStyleItem : CanvasControlItem() {

    var itemStyle: Paint.Style = Paint.Style.STROKE

    init {
        itemSingleSelectMutex = true
        itemClick = {
            itemRenderer?.let { renderer ->
                if (renderer is PictureItemRenderer) {
                    val renderItem = renderer.getRendererItem()
                    if (renderItem is PictureTextItem) {
                        renderer.updatePaintStyle(itemStyle)
                        updateAdapterItem()
                    }
                }
            }
        }
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {

        if (itemRenderer is PictureItemRenderer) {
            val renderItem = itemRenderer?.getRendererItem()
            if (renderItem is PictureTextItem) {
                itemIsSelected = itemRenderer?.paint?.style == itemStyle
            }
        }

        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
    }

}