package com.angcyo.canvas.laser.pecker.dslitem

import android.graphics.Paint
import com.angcyo.canvas.items.data.DataItemRenderer
import com.angcyo.canvas.laser.pecker.R
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
class TextStrokeStyleItem : CanvasControlItem2() {

    var itemStyle: Paint.Style = Paint.Style.STROKE

    init {
        itemLayoutId = R.layout.item_canvas_icon_horizontal_layout

        itemSingleSelectMutex = true
        itemClick = {
            itemRenderer?.let { renderer ->
                if (renderer is DataItemRenderer) {
                    renderer.dataItem?.updatePaintStyle(itemStyle, renderer)
                }
                updateAdapterItem()
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
        val renderer = itemRenderer
        if (renderer is DataItemRenderer) {
            itemIsSelected = renderer.dataTextItem?.textPaint?.style == itemStyle
        }

        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
    }

}