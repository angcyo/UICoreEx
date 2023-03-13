package com.angcyo.canvas2.laser.pecker.dslitem.control

import android.graphics.Paint
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.dialog.popup.menuPopupWindow
import com.angcyo.dsladapter.updateItemSelected
import com.angcyo.library.ex._string
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue

/**
 * 文本对齐触发item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/19
 */
class TextAlignSelectItem : BaseTextControlItem() {

    init {
        itemIco = R.drawable.canvas_text_align
        itemText = _string(R.string.canvas_align)
        itemClick = { anchor ->
            updateItemSelected(!itemIsSelected)

            if (itemIsSelected) {
                UMEvent.CANVAS_SHAPE.umengEventValue()
                anchor.context.menuPopupWindow(anchor) {
                    renderAdapterAction = {
                        TextAlignItem()() {
                            initItem()
                            itemIco = R.drawable.canvas_text_style_align_left_ico
                            itemText = _string(R.string.canvas_align_left)
                            itemAlign = Paint.Align.LEFT
                        }
                        TextAlignItem()() {
                            initItem()
                            itemIco = R.drawable.canvas_text_style_align_center_ico
                            itemText = _string(R.string.canvas_align_center)
                            itemAlign = Paint.Align.CENTER
                        }
                        TextAlignItem()() {
                            initItem()
                            itemIco = R.drawable.canvas_text_style_align_right_ico
                            itemText = _string(R.string.canvas_align_right)
                            itemAlign = Paint.Align.RIGHT
                        }
                    }
                    onDismiss = {
                        updateItemSelected(false)
                        false
                    }
                }
            }
        }
    }

}