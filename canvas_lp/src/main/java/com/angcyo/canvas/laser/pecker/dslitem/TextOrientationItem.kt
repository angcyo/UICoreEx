package com.angcyo.canvas.laser.pecker.dslitem

import android.widget.LinearLayout
import com.angcyo.canvas.items.data.DataItemRenderer
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
                if (renderer is DataItemRenderer) {
                    renderer.dataTextItem?.updateTextOrientation(itemOrientation, renderer)
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
            itemIsSelected = renderer.dataItem?.dataBean?.orientation == itemOrientation
        }

        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
    }

}