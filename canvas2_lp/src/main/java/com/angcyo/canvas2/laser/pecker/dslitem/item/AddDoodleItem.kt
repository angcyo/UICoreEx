package com.angcyo.canvas2.laser.pecker.dslitem.item

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.drawCanvasRight
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.canvas2.laser.pecker.util.LPElementHelper
import com.angcyo.doodle.ui.doodleDialog
import com.angcyo.library.ex._string
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue

/**
 * 添加涂鸦
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/03
 */
class AddDoodleItem : CanvasIconItem() {

    init {
        itemIco = R.drawable.canvas_doodle_ico
        itemText = _string(R.string.canvas_doodle)
        itemEnable = true
        itemClick = {
            UMEvent.CANVAS_DOODLE.umengEventValue()
            it.context.doodleDialog {
                onDoodleResultAction = { bitmap ->
                    LPElementHelper.addBitmapElement(itemRenderDelegate, bitmap)
                }
            }
        }
    }

}