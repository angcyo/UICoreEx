package com.angcyo.canvas2.laser.pecker.dslitem.control

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.dsladapter.updateItemSelected
import com.angcyo.library.ex._string

/**
 * 图层item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/03
 */
class ControlSettingItem : CanvasIconItem() {

    init {
        itemIco = R.drawable.canvas_setting_ico
        itemText = _string(R.string.canvas_setting)
        itemEnable = true

        itemClick = {
            updateItemSelected(!itemIsSelected)

            if (itemIsSelected) {
                /*it.context.canvasSettingWindow(it) {
                    this.canvasDelegate = canvasDelegate
                    onDismiss = {
                        itemIsSelected = false
                        updateAdapterItem()
                        false
                    }
                }*/
            }
        }
    }
}