package com.angcyo.canvas2.laser.pecker.dslitem.item

import androidx.fragment.app.Fragment
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dialog.addTextDialog
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.canvas2.laser.pecker.util.LPElementHelper
import com.angcyo.dsladapter.item.IFragmentItem
import com.angcyo.library.ex._string

/**
 * 添加文本/二维码/条码
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023-3-13
 */
class AddTextItem : CanvasIconItem(), IFragmentItem {

    override var itemFragment: Fragment? = null

    init {
        itemIco = R.drawable.canvas_text_ico
        itemText = _string(R.string.canvas_text)

        itemClick = {
            itemFragment?.context?.addTextDialog {
                onAddTextAction = { inputText, type ->
                    LPElementHelper.addTextElement(itemRenderDelegate, inputText, type)
                }
            }
        }
    }

}