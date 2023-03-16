package com.angcyo.canvas2.laser.pecker.dslitem.item

import androidx.fragment.app.Fragment
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dialog.addTextDialog
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.canvas2.laser.pecker.util.LPElementHelper
import com.angcyo.canvas2.laser.pecker.util.lpTextElement
import com.angcyo.dsladapter.item.IFragmentItem
import com.angcyo.library.ex._string
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue

/**
 * 添加文本/二维码/条码
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023-3-13
 */
class AddTextItem : CanvasIconItem(), IFragmentItem {

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
                onAddTextAction = { inputText, type ->
                    element.updateTextProperty(renderer, delegate) {
                        text = "$inputText"
                        bean.mtype = type
                    }
                }
            }
        }
    }

    override var itemFragment: Fragment? = null

    init {
        itemIco = R.drawable.canvas_text_ico
        itemText = _string(R.string.canvas_text)

        itemClick = {
            itemFragment?.context?.addTextDialog {
                onAddTextAction = { inputText, type ->
                    LPElementHelper.addTextElement(itemRenderDelegate, inputText, type)
                    UMEvent.CANVAS_TEXT.umengEventValue()
                }
            }
        }
    }

}