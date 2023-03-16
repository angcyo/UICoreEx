package com.angcyo.canvas2.laser.pecker.dslitem.item

import com.angcyo.canvas2.laser.pecker.RenderLayoutHelper
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.dsladapter.updateItemSelected
import com.angcyo.library.ex._string

/**
 * 编辑item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/03
 */
class ControlEditItem : CanvasIconItem() {

    /**控制器*/
    var itemRenderLayoutHelper: RenderLayoutHelper? = null

    companion object {

        /**tag 编辑item*/
        const val TAG_EDIT_ITEM = "tag_edit_item"
    }

    init {
        itemIco = R.drawable.canvas_edit_ico
        itemText = _string(R.string.canvas_edit)
        itemEnable = true
        itemTag = TAG_EDIT_ITEM
        itemClick = {
            updateItemSelected(!itemIsSelected)
            if (itemIsSelected) {
                itemRenderLayoutHelper?.changeSelectItem(this)
            }
            itemRenderLayoutHelper?.renderControlHelper?.visibleControlLayout(itemIsSelected)
        }
    }
}