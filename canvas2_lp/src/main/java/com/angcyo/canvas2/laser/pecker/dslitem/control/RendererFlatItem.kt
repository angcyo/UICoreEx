package com.angcyo.canvas2.laser.pecker.dslitem.control

import android.widget.LinearLayout
import com.angcyo.canvas.render.core.Reason
import com.angcyo.canvas.render.renderer.CanvasGroupRenderer
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.dialog.popup.MenuPopupConfig
import com.angcyo.library.component.Strategy

/**
 * Canvas图层分布
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/27
 */
class RendererFlatItem : CanvasIconItem() {

    /**水平分布*/
    var itemFlat: Int = LinearLayout.HORIZONTAL

    init {
        itemLayoutId = R.layout.item_canvas_icon_horizontal_layout

        itemClick = {
            itemRenderer?.let {
                if (it is CanvasGroupRenderer) {
                    it.updateRendererFlat(
                        itemFlat,
                        Reason.user,
                        Strategy.normal,
                        itemRenderDelegate
                    )
                }
            }
        }

        //点击后自动关闭pop
        itemFlag = MenuPopupConfig.FLAG_ITEM_DISMISS
    }

}