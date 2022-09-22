package com.angcyo.canvas.laser.pecker.dslitem

import com.angcyo.canvas.data.textStyle
import com.angcyo.canvas.items.PictureTextItem
import com.angcyo.canvas.items.renderer.DataItemRenderer
import com.angcyo.canvas.items.renderer.PictureItemRenderer
import com.angcyo.canvas.items.renderer.PictureTextItemRenderer
import com.angcyo.canvas.laser.pecker.R
import com.angcyo.dialog.popup.MenuPopupConfig
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.library.ex.have
import com.angcyo.widget.DslViewHolder

/**
 * 改变文本样式的item
 *
 * 2022-9-17 调整ui
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/10
 */
class TextStyleItem : CanvasControlItem2() {

    var itemStyle: Int = PictureTextItem.TEXT_STYLE_NONE

    init {
        itemLayoutId = R.layout.item_canvas_icon_horizontal_layout

        itemClick = {
            itemRenderer?.let { renderer ->
                if (renderer is PictureTextItemRenderer) {
                    val renderItem = renderer.getRendererRenderItem()
                    if (renderItem is PictureTextItem) {
                        renderer.enableTextStyle(itemStyle, !itemIsSelected)
                    }
                } else if (renderer is DataItemRenderer) {
                    renderer.dataTextItem?.updateTextStyle(itemStyle, !itemIsSelected, renderer)
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
        if (renderer is PictureItemRenderer) {
            val renderItem = renderer.getRendererRenderItem()
            if (renderItem is PictureTextItem) {
                itemIsSelected = renderItem.textStyle.have(itemStyle)
            }
        } else if (renderer is DataItemRenderer) {
            itemIsSelected =
                renderer.getRendererRenderItem()?.dataBean?.textStyle()?.have(itemStyle) == true
        }

        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
    }

}