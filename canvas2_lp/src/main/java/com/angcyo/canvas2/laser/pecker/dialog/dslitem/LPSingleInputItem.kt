package com.angcyo.canvas2.laser.pecker.dialog.dslitem

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.item.DslSingleInputItem
import com.angcyo.item.style.itemEditMaxInputLength

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/12
 */
open class LPSingleInputItem : DslSingleInputItem() {

    companion object {
        /**允许默认输入的字符长度*/
        var LP_DEFAULT_MAX_INPUT_LENGTH = 20

        val helpIcoPaddingRight = 0//_dimen(R.dimen.lib_xxhdpi)
    }

    init {
        itemLayoutId = R.layout.lp_single_input_item
        itemEditMaxInputLength = LP_DEFAULT_MAX_INPUT_LENGTH
    }
}