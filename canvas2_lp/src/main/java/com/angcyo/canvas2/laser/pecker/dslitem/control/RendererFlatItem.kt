package com.angcyo.canvas2.laser.pecker.dslitem.control

import com.angcyo.canvas.render.renderer.CanvasGroupRenderer
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.library.canvas.core.Reason
import com.angcyo.library.component.Strategy

/**
 * Canvas图层分布
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/27
 */
class RendererFlatItem : CanvasIconItem() {

    /**水平分布*/
    var itemFlatType: Int = CanvasGroupRenderer.FLAT_TYPE_HORIZONTAL

    init {
        itemLayoutId = R.layout.item_canvas_icon_horizontal_layout

        itemClick = {
            itemRenderer?.let {
                if (it is CanvasGroupRenderer) {
                    it.updateRendererFlat(
                        itemFlatType,
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