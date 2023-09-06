package com.angcyo.canvas2.laser.pecker.dialog.dslitem

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.DslIncrementItem
import com.angcyo.item.style.itemIncrementMinValue
import com.angcyo.item.style.itemIncrementValue
import com.angcyo.item.style.itemLabel
import com.angcyo.laserpacker.bean.LPVariableBean
import com.angcyo.library.ex._string
import com.angcyo.library.ex.toStr

/**
 * 时间偏移item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/06
 */
class VarTimeOffsetItem : DslIncrementItem() {

    /**数据结构*/
    var itemVariableBean: LPVariableBean? = null
        set(value) {
            field = value
            updateDefaultDate()
        }

    init {
        itemLayoutId = R.layout.date_offset_item
        itemLabel = _string(R.string.variable_time_offset)
        itemIncrementMinValue = 0
    }

    override fun onItemChangeListener(item: DslAdapterItem) {
        super.onItemChangeListener(item)
        itemVariableBean?.value =
            itemIncrementValue?.toStr()?.toLongOrNull() ?: (itemVariableBean?.value ?: 1)
    }

    private fun updateDefaultDate() {
        itemVariableBean?.let {
            itemIncrementValue = it.value.toStr()
        }
    }

}