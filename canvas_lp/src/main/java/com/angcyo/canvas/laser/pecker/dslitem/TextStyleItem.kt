package com.angcyo.canvas.laser.pecker.dslitem

import com.angcyo.canvas.items.PictureTextItem
import com.angcyo.canvas.items.renderer.PictureItemRenderer
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.library.ex.have
import com.angcyo.widget.DslViewHolder

/**
 * 改变文本样式的item
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/10
 */
class TextStyleItem : CanvasControlItem() {

    var itemStyle: Int = PictureTextItem.TEXT_STYLE_NONE

    init {
        itemClick = {
            itemRenderer?.let { renderer ->
                if (renderer is PictureItemRenderer) {
                    val renderItem = renderer.getRendererItem()
                    if (renderItem is PictureTextItem) {
                        renderer.enableTextStyle(itemStyle, !itemIsSelected)
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
                itemIsSelected = renderItem.textStyle.have(itemStyle)
            }
        }

        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
    }

}