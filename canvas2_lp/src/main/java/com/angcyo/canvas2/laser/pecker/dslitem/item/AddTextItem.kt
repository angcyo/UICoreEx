package com.angcyo.canvas2.laser.pecker.dslitem.item

import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dialog.addTextDialog
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.canvas2.laser.pecker.util.LPElementHelper
import com.angcyo.canvas2.laser.pecker.util.lpTextElement
import com.angcyo.library.ex._string
import com.angcyo.library.ex.reverseCharSequenceIfRtl
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue
import kotlin.math.max

/**
 * 添加文本/二维码/条码
 *
 * [AddTextItem]
 * [AddVariableTextItem]
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023-3-13
 */
class AddTextItem : CanvasIconItem() {

    companion object {

        /**修改条码/二维码/文本内容*/
        fun amendInputText(delegate: CanvasRenderDelegate?, renderer: BaseRenderer) {
            delegate ?: return
            val element = renderer.lpTextElement() ?: return
            val bean = element.elementBean
            delegate.view.context?.addTextDialog {
                dataType = bean.mtype
                defaultInputString = element.textProperty.text
                canSwitchType = false
                maxInputLength = max(maxInputLength, defaultInputString?.length ?: 0)
                onAddTextAction = { inputText, type ->
                    element.updateTextProperty(renderer, delegate) {
                        text = "${inputText.reverseCharSequenceIfRtl()}"
                        bean.mtype = type
                    }
                }
            }
        }
    }

    init {
        itemIco = R.drawable.canvas_text_ico
        itemText = _string(R.string.canvas_text)

        itemClick = {
            it.context?.addTextDialog {
                onAddTextAction = { inputText, type ->
                    LPElementHelper.addTextElement(
                        itemRenderDelegate,
                        inputText.reverseCharSequenceIfRtl(),
                        type
                    )
                    UMEvent.CANVAS_TEXT.umengEventValue()
                }
            }
        }
    }
}