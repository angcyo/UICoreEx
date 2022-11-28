package com.angcyo.canvas.laser.pecker.dslitem

import android.graphics.Paint
import com.angcyo.canvas.items.data.DataTextItem
import com.angcyo.canvas.laser.pecker.R
import com.angcyo.dialog.popup.menuPopupWindow
import com.angcyo.dsladapter.updateItemSelected
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.library.ex._string
import com.angcyo.library.ex.have
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue

/**
 * 文本样式选择触发item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/19
 */
class TextStyleSelectItem : CanvasControlItem2() {

    init {
        itemIco = R.drawable.canvas_text_style
        itemText = _string(R.string.canvas_style)
        itemClick = { anchor ->
            val renderer = itemRenderer
            updateItemSelected(!itemIsSelected)

            if (itemIsSelected) {
                UMEvent.CANVAS_SHAPE.umengEventValue()

                anchor.context.menuPopupWindow(anchor) {
                    renderAdapterAction = {
                        val closeTextEditItemsFun = HawkEngraveKeys.closeTextEditItemsFun
                        //
                        if (!closeTextEditItemsFun.have("_solid_")) {
                            TextStrokeStyleItem()() {
                                itemIco = R.drawable.canvas_text_style_solid
                                itemText = _string(R.string.canvas_solid)
                                itemStyle = Paint.Style.FILL
                                itemRenderer = renderer
                            }
                        }
                        if (!closeTextEditItemsFun.have("_stroke_")) {
                            TextStrokeStyleItem()() {
                                itemIco = R.drawable.canvas_text_style_stroke
                                itemText = _string(R.string.canvas_hollow)
                                itemStyle = Paint.Style.STROKE
                                itemRenderer = renderer
                            }
                        }
                        //
                        if (!closeTextEditItemsFun.have("_bold_")) {
                            TextStyleItem()() {
                                itemIco = R.drawable.canvas_text_bold_style_ico
                                itemText = _string(R.string.canvas_bold)
                                itemStyle = DataTextItem.TEXT_STYLE_BOLD
                                itemRenderer = renderer
                            }
                        }
                        if (!closeTextEditItemsFun.have("_italic_")) {
                            TextStyleItem()() {
                                itemIco = R.drawable.canvas_text_italic_style_ico
                                itemText = _string(R.string.canvas_italic)
                                itemStyle = DataTextItem.TEXT_STYLE_ITALIC
                                itemRenderer = renderer
                            }
                        }
                        if (!closeTextEditItemsFun.have("_underline_")) {
                            TextStyleItem()() {
                                itemIco = R.drawable.canvas_text_under_line_style_ico
                                itemText = _string(R.string.canvas_under_line)
                                itemStyle = DataTextItem.TEXT_STYLE_UNDER_LINE
                                itemRenderer = renderer
                            }
                        }
                        if (!closeTextEditItemsFun.have("_deleteline_")) {
                            TextStyleItem()() {
                                itemIco = R.drawable.canvas_text_delete_line_style_ico
                                itemText = _string(R.string.canvas_delete_line)
                                itemStyle = DataTextItem.TEXT_STYLE_DELETE_LINE
                                itemRenderer = renderer
                            }
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