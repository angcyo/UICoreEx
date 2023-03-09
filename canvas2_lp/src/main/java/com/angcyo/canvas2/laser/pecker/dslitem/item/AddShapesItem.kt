package com.angcyo.canvas2.laser.pecker.dslitem.item

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.dsladapter.updateItemSelected
import com.angcyo.library.ex._string

/**
 * 添加形状
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/03
 */
class AddShapesItem : CanvasIconItem() {

    init {
        itemIco = R.drawable.canvas_shapes_ico
        itemText = _string(R.string.canvas_shapes)
        itemClick = {
            updateItemSelected(!itemIsSelected)
            /*if (itemIsSelected) {
                vh.showControlLayout(canvasView, false, true)
                selectedItemWith(vh, canvasView, this)
                showShapeSelectLayout(vh, canvasView)
                UMEvent.CANVAS_SHAPE.umengEventValue()
            } else {
                selectedItemWith(vh, canvasView, null)
            }*/
        }
    }
}