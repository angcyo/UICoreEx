package com.angcyo.canvas2.laser.pecker.dslitem.item

import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.canvas2.laser.pecker.util.LPConstant
import com.angcyo.canvas2.laser.pecker.util.LPElementHelper

/**
 * 形状item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/17
 */
class ShapesItem : CanvasIconItem() {

    /**形状类型*/
    var itemShapeType: Int = LPConstant.DATA_TYPE_LINE

    init {
        itemClick = {
            LPElementHelper.addShapesElement(itemRenderDelegate, itemShapeType)
        }
    }

}