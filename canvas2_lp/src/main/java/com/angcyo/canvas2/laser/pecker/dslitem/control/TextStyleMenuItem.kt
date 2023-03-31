package com.angcyo.canvas2.laser.pecker.dslitem.control

import android.graphics.Paint
import com.angcyo.canvas.render.element.TextElement
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.dialog.popup.menuPopupWindow
import com.angcyo.dsladapter.updateItemSelected
import com.angcyo.laserpacker.device.HawkEngraveKeys
import com.angcyo.library.ex._string
import com.angcyo.library.ex.have
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue

/**
 * 文本样式选择触发item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/19
 */
class TextStyleMenuItem : BaseTextControlItem() {

    init {
        itemIco = R.drawable.canvas_text_style
        itemText = _string(R.string.canvas_style)
        itemClick = { anchor ->
            updateItemSelected(!itemIsSelected)

            if (itemIsSelected) {
                UMEvent.CANVAS_SHAPE.umengEventValue()

                anchor.context.menuPopupWindow(anchor) {
                    renderAdapterAction = {
                        val closeTextEditItemsFun = HawkEngraveKeys.closeTextEditItemsFun
                        //
                        if (!closeTextEditItemsFun.have("_solid_")) {
                            TextStrokeStyleItem()() {
                                initItem()
                                itemIco = R.drawable.canvas_text_style_solid
                                itemText = _string(R.string.canvas_solid)
                                itemStyle = Paint.Style.FILL
                            }
                        }
                        if (!closeTextEditItemsFun.have("_stroke_")) {
                            TextStrokeStyleItem()() {
                                initItem()
                                itemIco = R.drawable.canvas_text_style_stroke
                                itemText = _string(R.string.canvas_hollow)
                                itemStyle = Paint.Style.STROKE
                            }
                        }
                        //
                        if (!closeTextEditItemsFun.have("_bold_")) {
                            TextStyleItem()() {
                                initItem()
                                itemIco = R.drawable.canvas_text_bold_style_ico
                                itemText = _string(R.string.canvas_bold)
                                itemStyle = TextElement.TEXT_STYLE_BOLD
                            }
                        }
                        if (!closeTextEditItemsFun.have("_italic_")) {
                            TextStyleItem()() {
                                initItem()
                                itemIco = R.drawable.canvas_text_italic_style_ico
                                itemText = _string(R.string.canvas_italic)
                                itemStyle = TextElement.TEXT_STYLE_ITALIC
                            }
                        }
                        if (!closeTextEditItemsFun.have("_underline_")) {
                            TextStyleItem()() {
                                initItem()
                                itemIco = R.drawable.canvas_text_under_line_style_ico
                                itemText = _string(R.string.canvas_under_line)
                                itemStyle = TextElement.TEXT_STYLE_UNDER_LINE
                            }
                        }
                        if (!closeTextEditItemsFun.have("_deleteline_")) {
                            TextStyleItem()() {
                                initItem()
                                itemIco = R.drawable.canvas_text_delete_line_style_ico
                                itemText = _string(R.string.canvas_delete_line)
                                itemStyle = TextElement.TEXT_STYLE_DELETE_LINE
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