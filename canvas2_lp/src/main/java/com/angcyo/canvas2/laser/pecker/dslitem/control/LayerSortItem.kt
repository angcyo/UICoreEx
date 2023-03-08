package com.angcyo.canvas2.laser.pecker.dslitem.control

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.drawCanvasRight
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.library.ex._string

/**
 * 图层排序item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/08
 */
class LayerSortItem : CanvasIconItem() {
    init {
        itemIco = R.drawable.canvas_layer_sort
        itemText = _string(R.string.canvas_sort)
        itemEnable = true
        drawCanvasRight()
        itemClick = {
            /*it.context.menuPopupWindow(it) {
                renderAdapterAction = {
                    CanvasArrangeItem()() {
                        itemArrange = CanvasDelegate.ARRANGE_FORWARD
                        itemRenderer = renderer
                        itemCanvasDelegate = canvasView.canvasDelegate
                    }
                    CanvasArrangeItem()() {
                        itemArrange = CanvasDelegate.ARRANGE_BACKWARD
                        itemRenderer = renderer
                        itemCanvasDelegate = canvasView.canvasDelegate
                    }
                    CanvasArrangeItem()() {
                        itemArrange = CanvasDelegate.ARRANGE_FRONT
                        itemRenderer = renderer
                        itemCanvasDelegate = canvasView.canvasDelegate
                        itemFlag = MenuPopupConfig.FLAG_ITEM_DISMISS
                    }
                    CanvasArrangeItem()() {
                        itemArrange = CanvasDelegate.ARRANGE_BACK
                        itemRenderer = renderer
                        itemCanvasDelegate = canvasView.canvasDelegate
                    }
                }
            }*/
        }
    }
}