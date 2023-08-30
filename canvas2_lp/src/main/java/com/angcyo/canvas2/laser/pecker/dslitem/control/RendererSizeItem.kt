package com.angcyo.canvas2.laser.pecker.dslitem.control

import com.angcyo.canvas.render.renderer.CanvasGroupRenderer
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.library.canvas.core.Reason
import com.angcyo.library.component.Strategy

/**
 * Canvas元素大小分布
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/08/30
 */
class RendererSizeItem : CanvasIconItem() {

    /**大小分布*/
    var itemSizeType: Int = CanvasGroupRenderer.SIZE_TYPE_WIDTH_HEIGHT

    init {
        itemLayoutId = R.layout.item_canvas_icon_horizontal_layout

        itemClick = {
            itemRenderer?.let {
                if (it is CanvasGroupRenderer) {
                    it.updateRendererSize(
                        itemSizeType,
                        Reason.user,
                        Strategy.normal,
                        itemRenderDelegate
                    )
                }
            }
        }

        //点击后自动关闭pop
        //itemFlag = MenuPopupConfig.FLAG_ITEM_DISMISS
    }

}