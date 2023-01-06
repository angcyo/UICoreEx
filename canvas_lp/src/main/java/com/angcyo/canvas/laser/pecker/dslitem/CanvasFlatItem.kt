package com.angcyo.canvas.laser.pecker.dslitem

import android.widget.LinearLayout
import com.angcyo.canvas.core.renderer.GroupRenderer
import com.angcyo.canvas.laser.pecker.R
import com.angcyo.dialog.popup.MenuPopupConfig

/**
 * Canvas图层分布
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/27
 */
class CanvasFlatItem : CanvasControlItem2() {

    /**水平分布*/
    var itemFlat: Int = LinearLayout.HORIZONTAL

    init {
        itemLayoutId = R.layout.item_canvas_icon_horizontal_layout

        itemClick = {
            itemRenderer?.let {
                if (it is GroupRenderer) {
                    it.updateFlat(itemFlat)
                }
            }
        }

        //点击后自动关闭pop
        itemFlag = MenuPopupConfig.FLAG_ITEM_DISMISS
    }

}