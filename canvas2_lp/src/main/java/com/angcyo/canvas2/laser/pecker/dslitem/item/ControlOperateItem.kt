package com.angcyo.canvas2.laser.pecker.dslitem.item

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.library.ex._string

/**
 * 操作item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/15
 */
class ControlOperateItem : CanvasIconItem() {

    init {
        itemIco = R.drawable.canvas_actions_ico
        itemText = _string(R.string.canvas_operate)
        itemEnable = true
        itemClick = {

        }
    }
}