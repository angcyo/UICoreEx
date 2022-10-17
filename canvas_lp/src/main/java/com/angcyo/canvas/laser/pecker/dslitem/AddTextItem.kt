package com.angcyo.canvas.laser.pecker.dslitem

import androidx.fragment.app.Fragment
import com.angcyo.canvas.CanvasView
import com.angcyo.canvas.graphics.addTextRender
import com.angcyo.canvas.items.data.DataItemRenderer
import com.angcyo.canvas.laser.pecker.R
import com.angcyo.canvas.laser.pecker.addTextDialog
import com.angcyo.dsladapter.item.IFragmentItem
import com.angcyo.library.ex._string
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/04/18
 */
class AddTextItem : CanvasControlItem2(), IFragmentItem {

    companion object {

        /**修改条码/二维码/文本内容*/
        fun amendInputText(canvasView: CanvasView?, itemRenderer: DataItemRenderer) {
            val dataBean = itemRenderer.dataItem?.dataBean ?: return
            canvasView?.context?.addTextDialog {
                dataType = dataBean.mtype
                defaultInputString = dataBean.text
                canSwitchType = false
                onAddTextAction = { inputText, type ->
                    itemRenderer.dataTextItem?.updateText("$inputText", type, itemRenderer)
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
                    itemCanvasDelegate?.addTextRender(inputText, type)
                    UMEvent.CANVAS_TEXT.umengEventValue()
                }
            }
        }
    }
}