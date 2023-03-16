package com.angcyo.canvas2.laser.pecker.dslitem.control

import android.graphics.Paint
import com.angcyo.canvas.render.util.textElement
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.dialog.popup.MenuPopupConfig
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.widget.DslViewHolder

/**
 * 改变文本画笔风格的item, 并且支持互斥
 *
 * 2022-9-17 调整ui
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/10
 */
class TextStrokeStyleItem : BaseTextControlItem() {

    var itemStyle: Paint.Style = Paint.Style.STROKE

    init {
        itemLayoutId = R.layout.item_canvas_icon_horizontal_layout

        itemSingleSelectMutex = true
        itemClick = {
            updateTextProperty {
                paintStyle = itemStyle
            }
        }

        //点击后自动关闭pop
        itemFlag = MenuPopupConfig.FLAG_ITEM_DISMISS
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        itemIsSelected = itemRenderer?.textElement?.paint?.style == itemStyle
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
    }

}