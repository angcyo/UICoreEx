package com.angcyo.canvas2.laser.pecker.dslitem

import androidx.fragment.app.Fragment
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.dsladapter.item.IFragmentItem
import com.angcyo.library.ex._string

/**
 * 添加文本/二维码/条码
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/03
 */
class AddTextItem : CanvasIconItem(), IFragmentItem {

    override var itemFragment: Fragment? = null

    init {
        itemIco = R.drawable.canvas_text_ico
        itemText = _string(R.string.canvas_text)

        itemClick = {
            /*itemFragment?.context?.addTextDialog {
                onAddTextAction = { inputText, type ->
                    itemCanvasDelegate?.addTextRender(inputText, type)
                    UMEvent.CANVAS_TEXT.umengEventValue()
                }
            }*/
        }
    }

}