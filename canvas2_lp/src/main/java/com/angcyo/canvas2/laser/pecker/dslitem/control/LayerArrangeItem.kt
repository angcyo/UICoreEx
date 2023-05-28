package com.angcyo.canvas2.laser.pecker.dslitem.control

import com.angcyo.canvas.render.core.CanvasRenderManager
import com.angcyo.canvas.render.core.Reason
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.dialog.popup.MenuPopupConfig
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.library.component.Strategy
import com.angcyo.library.ex._string
import com.angcyo.widget.DslViewHolder

/**
 * Canvas图层排序
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/19
 */
class LayerArrangeItem : CanvasIconItem() {

    /**排序方式*/
    var itemArrange: Int = 0
        set(value) {
            field = value
            when (value) {
                CanvasRenderManager.ARRANGE_FORWARD -> {
                    itemIco = R.drawable.canvas_arrange_forward
                    itemText = _string(R.string.canvas_forward)
                }
                CanvasRenderManager.ARRANGE_BACKWARD -> {
                    itemIco = R.drawable.canvas_arrange_backward
                    itemText = _string(R.string.canvas_backward)
                }
                CanvasRenderManager.ARRANGE_FRONT -> {
                    itemIco = R.drawable.canvas_arrange_front
                    itemText = _string(R.string.canvas_front)
                }
                CanvasRenderManager.ARRANGE_BACK -> {
                    itemIco = R.drawable.canvas_arrange_back
                    itemText = _string(R.string.canvas_back)
                }
            }
        }

    init {
        //itemPaddingTop = _dimen(R.dimen.lib_xhdpi)

        itemLayoutId = R.layout.item_canvas_icon_horizontal_layout

        itemClick = {
            itemRenderer?.let {
                itemRenderDelegate?.renderManager?.arrangeElement(
                    it,
                    itemArrange,
                    Reason.user,
                    Strategy.normal
                )
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
        itemEnable = renderer != null && itemRenderDelegate?.renderManager?.elementCanArrange(
            renderer,
            itemArrange
        ) == true
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
    }

}