package com.angcyo.canvas2.laser.pecker.dslitem.item

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.RenderLayoutHelper
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.dsladapter.updateItemSelected
import com.angcyo.library.ex._string
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue

/**
 * 添加形状
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/03
 */
class AddShapesItem : CanvasIconItem() {

    /**控制器*/
    var itemRenderLayoutHelper: RenderLayoutHelper? = null

    init {
        itemIco = R.drawable.canvas_shapes_ico
        itemText = _string(R.string.canvas_shapes)
        itemClick = {
            updateItemSelected(!itemIsSelected)
            if (itemIsSelected) {
                itemRenderLayoutHelper?.renderShapesItems(this, true)
                UMEvent.CANVAS_SHAPE.umengEventValue()
            } else {
                itemRenderLayoutHelper?.renderShapesItems(this, false)
            }
        }
    }
}