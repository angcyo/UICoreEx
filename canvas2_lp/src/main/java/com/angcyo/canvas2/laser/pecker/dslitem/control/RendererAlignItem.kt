package com.angcyo.canvas2.laser.pecker.dslitem.control

import android.view.Gravity
import com.angcyo.canvas.render.renderer.CanvasGroupRenderer
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.library.canvas.core.Reason
import com.angcyo.library.component.Strategy

/**
 * Canvas图层对齐
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/27
 */
class RendererAlignItem : CanvasIconItem() {

    /**对齐*/
    var itemAlign: Int = Gravity.LEFT

    init {
        itemLayoutId = R.layout.item_canvas_icon_horizontal_layout

        itemClick = {
            itemRenderer?.let {
                if (it is CanvasGroupRenderer) {
                    it.updateRendererAlign(
                        itemAlign,
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