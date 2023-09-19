package com.angcyo.canvas2.laser.pecker.dialog.dslitem

import android.view.Gravity
import com.angcyo.item.DslRadioGroupItem

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/19
 */
class LPRadioGroupItem : DslRadioGroupItem() {

    init {
        itemFlowLayoutGravity = Gravity.RIGHT
        itemFlowHorizontalSpace = 0//_dimen(R.dimen.lib_hdpi)
        itemFlowVerticalSpace = itemFlowHorizontalSpace
    }

}