package com.angcyo.canvas2.laser.pecker.dialog.dslitem

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.doodle.ui.dslitem.DoodleIconItem
import com.angcyo.library.ex._string

/**
 * 变量模板预览item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/08/31
 */
class VariableTextEditItem : DoodleIconItem() {
    init {
        itemText = _string(R.string.ui_edit)
        itemIco = R.drawable.core_edit_svg
    }
}