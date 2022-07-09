package com.angcyo.canvas.laser.pecker.dslitem

import com.angcyo.library.ex._string
import com.angcyo.canvas.laser.pecker.R

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/04/18
 */
class AddDoodleItem : CanvasControlItem() {

    init {
        itemIco = R.drawable.canvas_doodle_ico
        itemText = _string(R.string.canvas_doodle)
    }

}