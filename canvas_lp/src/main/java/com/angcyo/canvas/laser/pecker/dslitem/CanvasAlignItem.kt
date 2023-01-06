package com.angcyo.canvas.laser.pecker.dslitem

import android.view.Gravity
import com.angcyo.canvas.core.renderer.GroupRenderer
import com.angcyo.canvas.laser.pecker.R
import com.angcyo.dialog.popup.MenuPopupConfig

/**
 * Canvas图层对齐
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/27
 */
class CanvasAlignItem : CanvasControlItem2() {

    /**对齐*/
    var itemAlign: Int = Gravity.LEFT

    init {
        itemLayoutId = R.layout.item_canvas_icon_horizontal_layout

        itemClick = {
            itemRenderer?.let {
                if (it is GroupRenderer) {
                    it.updateAlign(itemAlign)
                }
            }
        }

        //点击后自动关闭pop
        itemFlag = MenuPopupConfig.FLAG_ITEM_DISMISS
    }

}