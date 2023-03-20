package com.angcyo.canvas2.laser.pecker.dslitem.control

import android.view.Gravity
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.dialog.popup.menuPopupWindow
import com.angcyo.library.ex._string

/**
 * 元素对齐菜单item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/20
 */
class RendererAlignMenuItem : CanvasIconItem() {

    init {
        itemIco = R.drawable.canvas_align_left_ico
        itemText = _string(R.string.canvas_align)

        itemClick = {
            it.context.menuPopupWindow(it) {
                renderAdapterAction = {
                    RendererAlignItem()() {
                        itemIco = R.drawable.canvas_align_left_ico
                        itemText = _string(R.string.canvas_align_left)
                        this@RendererAlignMenuItem.initSubItem(this)
                        itemAlign = Gravity.LEFT
                    }
                    RendererAlignItem()() {
                        itemIco = R.drawable.canvas_align_right_ico
                        itemText = _string(R.string.canvas_align_right)
                        this@RendererAlignMenuItem.initSubItem(this)
                        itemAlign = Gravity.RIGHT
                    }

                    //
                    RendererAlignItem()() {
                        itemIco = R.drawable.canvas_align_top_ico
                        itemText = _string(R.string.canvas_align_top)
                        this@RendererAlignMenuItem.initSubItem(this)
                        itemAlign = Gravity.TOP
                    }
                    RendererAlignItem()() {
                        itemIco = R.drawable.canvas_align_bottom_ico
                        itemText = _string(R.string.canvas_align_bottom)
                        this@RendererAlignMenuItem.initSubItem(this)
                        itemAlign = Gravity.BOTTOM
                    }

                    //
                    RendererAlignItem()() {
                        itemIco = R.drawable.canvas_align_horizontal_ico
                        itemText = _string(R.string.canvas_align_horizontal)
                        this@RendererAlignMenuItem.initSubItem(this)
                        itemAlign = Gravity.CENTER_HORIZONTAL
                    }
                    RendererAlignItem()() {
                        itemIco = R.drawable.canvas_align_vertical_ico
                        itemText = _string(R.string.canvas_align_vertical)
                        this@RendererAlignMenuItem.initSubItem(this)
                        itemAlign = Gravity.CENTER_VERTICAL
                    }
                    RendererAlignItem()() {
                        itemIco = R.drawable.canvas_align_center_ico
                        itemText = _string(R.string.canvas_align_center)
                        this@RendererAlignMenuItem.initSubItem(this)
                        itemAlign = Gravity.CENTER
                    }
                }
            }
        }
    }
}