package com.angcyo.canvas.laser.pecker.dslitem

import com.angcyo.canvas.CanvasView
import com.angcyo.canvas.utils.addLineRenderer
import com.angcyo.canvas.laser.pecker.R

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/04/22
 */
class ShapeLineItem(val canvasView: CanvasView) : CanvasControlItem() {

    init {
        itemIco = R.drawable.canvas_shape_line_ico
        itemText = "线条!"
        itemClick = {
            canvasView.addLineRenderer()
        }
    }

}