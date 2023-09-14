package com.angcyo.canvas2.laser.pecker.dialog.dslitem

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.item.DslPropertySwitchItem
import com.angcyo.item.style.itemLabel
import com.angcyo.library.ex._string

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/14
 */
class VarPropertySwitchItem : DslPropertySwitchItem() {

    init {
        itemLayoutId = R.layout.item_var_property_switch

        itemLabel = _string(R.string.variable_reset)
        //itemLabelTextSize = _dimen(R.dimen.text_body_size).toFloat()
    }

}