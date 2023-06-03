package com.angcyo.canvas2.laser.pecker.dslitem.control

import android.widget.LinearLayout
import com.angcyo.canvas.render.util.textElement
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.widget.DslViewHolder

/**
 * 改变文本方向的item, 并且支持多类型互斥
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/10
 */
class TextOrientationItem : BaseTextControlItem() {

    var itemOrientation: Int = LinearLayout.HORIZONTAL

    init {
        itemSingleSelectMutex = true
        itemClick = {
            updateTextProperty {
                orientation = itemOrientation
            }
            updateCurveTextItem()
        }
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        itemIsSelected = itemRenderer?.textElement?.textProperty?.orientation == itemOrientation
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
    }
}