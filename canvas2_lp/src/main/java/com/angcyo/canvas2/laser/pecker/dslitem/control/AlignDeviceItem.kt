package com.angcyo.canvas2.laser.pecker.dslitem.control

import android.graphics.RectF
import com.angcyo.canvas.render.util.alignInBounds
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.library.ex._string
import com.angcyo.library.ex.emptyRectF

/**
 *  在设备范围内对齐, 比如居中
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/08
 */
class AlignDeviceItem : CanvasIconItem() {

    var itemDeviceBounds: RectF? = null

    init {
        itemIco = R.drawable.canvas_align_center_ico
        itemText = _string(R.string.canvas_bounds_center)
        itemClick = {
            itemRenderer?.alignInBounds(itemRenderDelegate, itemDeviceBounds ?: emptyRectF())
        }
    }
}