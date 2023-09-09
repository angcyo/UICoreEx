package com.angcyo.canvas2.laser.pecker.dslitem.control

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.util.lpTextElement
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.widget.DslViewHolder

/**
 * 改变文本对齐方向的item, 并且支持多类型互斥
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023-9-9
 */
class BarcodeShowStyleSelectItem : BaseTextControlItem() {

    /**显示样式*/
    var itemShowStyle: String? = LPDataConstant.TEXT_SHOW_STYLE_NONE

    init {
        itemLayoutId = R.layout.item_canvas_icon_horizontal_layout

        itemSingleSelectMutex = true
        itemClick = {
            itemRenderer?.lpTextElement()
                ?.updateVariableTextProperty(itemRenderer, itemRenderDelegate) {
                    elementBean.textShowStyle = itemShowStyle
                }
            updateAdapterItem()
        }
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        val textShowStyle = elementBean?.textShowStyle
        if (textShowStyle == null) {
            elementBean?.textShowStyle = LPDataConstant.TEXT_SHOW_STYLE_NONE
        }
        itemIsSelected = textShowStyle == itemShowStyle
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
    }

}