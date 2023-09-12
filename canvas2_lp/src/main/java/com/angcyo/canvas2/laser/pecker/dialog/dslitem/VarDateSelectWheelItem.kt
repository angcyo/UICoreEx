package com.angcyo.canvas2.laser.pecker.dialog.dslitem

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.dialog2.dslitem.LPDateWheelItem
import com.angcyo.item.style.itemLabel
import com.angcyo.library.ex._string

/**
 * 自定义日期选择item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/06
 */
class VarDateSelectWheelItem : LPDateWheelItem() {
    init {
        itemLayoutId = R.layout.lp_date_wheel_item
        itemLabel = _string(R.string.variable_date_select)
    }
}