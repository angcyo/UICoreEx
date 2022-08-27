package com.angcyo.canvas.laser.pecker.dslitem

import android.widget.LinearLayout
import com.angcyo.canvas.items.PictureTextItem
import com.angcyo.canvas.items.renderer.PictureItemRenderer
import com.angcyo.canvas.items.renderer.PictureTextItemRenderer
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.widget.DslViewHolder

/**
 * 改变文本方向的item, 并且支持多类型互斥
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/10
 */
class TextOrientationItem : CanvasControlItem() {

    var itemOrientation: Int = LinearLayout.HORIZONTAL

    init {
        itemSingleSelectMutex = true
        itemClick = {
            itemRenderer?.let { renderer ->
                if (renderer is PictureTextItemRenderer) {
                    val renderItem = renderer.getRendererRenderItem()
                    if (renderItem is PictureTextItem) {
                        renderer.updateTextOrientation(itemOrientation)
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
            val renderItem = itemRenderer?.getRendererRenderItem()
            if (renderItem is PictureTextItem) {
                itemIsSelected = renderItem.orientation == itemOrientation
            }
        }

        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
    }

}