package com.angcyo.engrave.dslitem

import com.angcyo.engrave.R
import com.angcyo.engrave.data.EngraveDataInfo
import com.angcyo.item.DslCheckFlowItem
import com.angcyo.item.style.itemCheckItems
import com.angcyo.item.style.itemText
import com.angcyo.library.annotation.Implementation
import com.angcyo.library.ex._string

/**
 * 雕刻数据处理模式item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/01
 */
@Implementation
class EngraveDataModeItem : DslCheckFlowItem() {

    /**待雕刻的数据*/
    var itemEngraveDataInfo: EngraveDataInfo? = null

    init {
        itemText = _string(R.string.ui_slip_menu_model)

        itemCheckItems = listOf("灰度", "黑白", "GCode")
    }

}