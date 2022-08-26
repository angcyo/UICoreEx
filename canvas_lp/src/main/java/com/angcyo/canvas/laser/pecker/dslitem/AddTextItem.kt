package com.angcyo.canvas.laser.pecker.dslitem

import androidx.fragment.app.Fragment
import com.angcyo.canvas.CanvasView
import com.angcyo.canvas.laser.pecker.R
import com.angcyo.canvas.utils.addPictureTextRender
import com.angcyo.dialog.inputDialog
import com.angcyo.dsladapter.item.IFragmentItem
import com.angcyo.library.ex._string
import com.angcyo.library.ex.dpi
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/04/18
 */
class AddTextItem(val canvasView: CanvasView) : CanvasControlItem(), IFragmentItem {

    override var itemFragment: Fragment? = null

    companion object {
        const val MAX_INPUT_LENGTH = 30

        const val KEY_ADD_TEXT = "canvas_add_text"
    }

    init {
        itemIco = R.drawable.canvas_text_ico
        itemText = _string(R.string.canvas_text)

        itemClick = {
            itemFragment?.context?.inputDialog {
                inputViewHeight = 100 * dpi
                maxInputLength = MAX_INPUT_LENGTH
                inputHistoryHawkKey = KEY_ADD_TEXT
                onInputResult = { dialog, inputText ->
                    if (inputText.isNotEmpty()) {
                        //canvasView.addTextRenderer("$inputText")
                        //canvasView.addPictureTextRenderer("$inputText")
                        canvasView.canvasDelegate.addPictureTextRender("$inputText")
                        UMEvent.CANVAS_TEXT.umengEventValue()
                    }
                    false
                }
            }
        }
    }
}