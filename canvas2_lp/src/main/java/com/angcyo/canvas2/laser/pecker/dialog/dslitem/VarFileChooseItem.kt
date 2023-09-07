package com.angcyo.canvas2.laser.pecker.dialog.dslitem

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.dialog2.dslitem.LPLabelWheelItem
import com.angcyo.item.style.itemLabel
import com.angcyo.library.ex._string

/**
 * 文件选择item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/07
 */
class VarFileChooseItem : LPLabelWheelItem() {

    init {
        itemLabel = _string(R.string.variable_file_label)
    }

}