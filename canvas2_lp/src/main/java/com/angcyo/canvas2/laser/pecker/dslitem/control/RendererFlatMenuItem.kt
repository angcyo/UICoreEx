package com.angcyo.canvas2.laser.pecker.dslitem.control

import android.widget.LinearLayout
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.dialog.popup.menuPopupWindow
import com.angcyo.library.ex._string

/**
 * 元素分布菜单item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/20
 */
class RendererFlatMenuItem : CanvasIconItem() {

    init {
        itemIco = R.drawable.canvas_flat_horizontal_svg
        itemText = _string(R.string.canvas_flat)

        itemClick = {
            it.context.menuPopupWindow(it) {
                renderAdapterAction = {
                    RendererFlatItem()() {
                        itemIco = R.drawable.canvas_flat_horizontal_svg
                        itemText = _string(R.string.canvas_flat_horizontal)
                        this@RendererFlatMenuItem.initSubItem(this)
                        itemFlat = LinearLayout.HORIZONTAL
                    }
                    RendererFlatItem()() {
                        itemIco = R.drawable.canvas_flat_vertical_svg
                        itemText = _string(R.string.canvas_flat_vertical)
                        this@RendererFlatMenuItem.initSubItem(this)
                        itemFlat = LinearLayout.VERTICAL
                    }
                }
            }
        }
    }
}