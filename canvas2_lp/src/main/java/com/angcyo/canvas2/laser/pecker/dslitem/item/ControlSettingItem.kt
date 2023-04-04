package com.angcyo.canvas2.laser.pecker.dslitem.item

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dialog.canvasSettingWindow
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.dsladapter.updateItemSelected
import com.angcyo.library.ex._string

/**
 * 设置item
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
                it.context.canvasSettingWindow(it) {
                    this.delegate = itemRenderDelegate
                    onDismiss = {
                        itemIsSelected = false
                        updateAdapterItem()
                        false
                    }
                }
            }
        }
    }
}