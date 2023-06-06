package com.angcyo.canvas2.laser.pecker.dslitem.control

import com.angcyo.canvas.render.element.TextElement
import com.angcyo.canvas.render.element.TextElement.Companion.TEXT_STYLE_NONE
import com.angcyo.canvas.render.util.textElement
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.dialog.popup.MenuPopupConfig
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.widget.DslViewHolder
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue

/**
 * 改变文本样式的item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023-3-13
 */
class TextStyleItem : CanvasIconItem() {

    var itemStyle: Int = TEXT_STYLE_NONE

    init {
        itemLayoutId = R.layout.item_canvas_icon_horizontal_layout

        itemClick = {
            when (itemStyle) {
                TextElement.TEXT_STYLE_DELETE_LINE -> UMEvent.CANVAS_TEXT_DELETE_LINE.umengEventValue()
                TextElement.TEXT_STYLE_UNDER_LINE -> UMEvent.CANVAS_TEXT_UNDER_LINE.umengEventValue()
                TextElement.TEXT_STYLE_ITALIC -> UMEvent.CANVAS_TEXT_ITALIC.umengEventValue()
                TextElement.TEXT_STYLE_BOLD -> UMEvent.CANVAS_TEXT_BOLD.umengEventValue()
            }
            itemRenderer?.textElement?.updateTextStyle(
                itemStyle,
                !itemIsSelected,
                itemRenderer,
                itemRenderDelegate
            )
            updateAdapterItem()
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
        itemIsSelected = itemRenderer?.textElement?.haveTextStyle(itemStyle) == true
        itemHolder.tv(R.id.lib_text_view)?.paint?.apply {
            TextElement.updatePaintStyle(this, itemStyle)
        }
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
    }
}