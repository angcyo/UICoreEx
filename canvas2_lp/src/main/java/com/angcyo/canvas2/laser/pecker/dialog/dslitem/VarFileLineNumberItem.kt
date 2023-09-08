package com.angcyo.canvas2.laser.pecker.dialog.dslitem

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.DslIncrementItem
import com.angcyo.item.style.itemIncrementMinValue
import com.angcyo.item.style.itemIncrementValue
import com.angcyo.item.style.itemLabel
import com.angcyo.library.ex._string

/**
 * 文件行号设置
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/08
 */
class VarFileLineNumberItem : DslIncrementItem(), IVarFileItem {

    init {
        itemLabel = _string(R.string.variable_file_line)
        itemIncrementMinValue = 1
    }

    override fun onSetItemData(data: Any?) {
        super.onSetItemData(data)
    }

    override fun onItemChangeListener(item: DslAdapterItem) {
        super.onItemChangeListener(item)
        _itemVariableBean?.current = itemIncrementValue?.toString()?.toLongOrNull() ?: 0
        _itemVariableBean?.reset()
    }
}