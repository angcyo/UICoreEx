package com.angcyo.canvas2.laser.pecker.dslitem.item

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.RenderLayoutHelper
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.dsladapter.updateItemSelected
import com.angcyo.library.ex._string

/**
 * 添加变量文本
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/08/30
 */
class VariableTextItem : CanvasIconItem() {

    /**控制器*/
    var itemRenderLayoutHelper: RenderLayoutHelper? = null

    init {
        itemIco = R.drawable.canvas_variable_text_ico
        itemText = _string(R.string.canvas_variable_template)
        itemClick = {
            updateItemSelected(!itemIsSelected)
            itemRenderLayoutHelper?.renderVariableTextItems(this, itemIsSelected)
        }
    }
}