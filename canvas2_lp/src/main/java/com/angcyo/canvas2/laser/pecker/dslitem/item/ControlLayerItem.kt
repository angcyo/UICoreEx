package com.angcyo.canvas2.laser.pecker.dslitem.item

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.RenderLayoutHelper
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.dsladapter.updateItemSelected
import com.angcyo.library.ex._string

/**
 * 图层item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/03
 */
class ControlLayerItem : CanvasIconItem() {

    companion object {

        /**tag 图层item*/
        const val TAG_LAYER_ITEM = "tag_layer_item"
    }

    var itemRenderLayoutHelper: RenderLayoutHelper? = null

    init {
        itemIco = R.drawable.canvas_layer_ico
        itemText = _string(R.string.canvas_layer)
        itemEnable = true
        itemTag = TAG_LAYER_ITEM
        itemClick = {
            //隐藏或者显示图层布局
            val selected = !itemIsSelected
            updateItemSelected(selected)
            itemRenderLayoutHelper?.showLayerLayout(selected)

            if (selected) {
                //选中之后, 渲染图层item rv
                itemRenderLayoutHelper?.renderLayerListLayout()
            }
        }
    }
}