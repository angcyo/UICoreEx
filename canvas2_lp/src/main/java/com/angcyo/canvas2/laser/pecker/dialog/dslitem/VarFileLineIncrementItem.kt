package com.angcyo.canvas2.laser.pecker.dialog.dslitem

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.DslIncrementItem
import com.angcyo.item.style.itemIncrementMinValue
import com.angcyo.item.style.itemIncrementValue
import com.angcyo.item.style.itemLabel
import com.angcyo.library.ex._string
import com.angcyo.library.ex.toStr

/**
 * 文件行号设置
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/08
 */
class VarFileLineIncrementItem : DslIncrementItem(), IVarFileItem {

    init {
        itemLabel = _string(R.string.variable_file_increment)
        itemIncrementMinValue = 1
    }

    override fun onSetItemData(data: Any?) {
        super.onSetItemData(data)
        itemIncrementValue = _itemVariableBean?.stepVal?.toStr()
    }

    override fun onItemChangeListener(item: DslAdapterItem) {
        super.onItemChangeListener(item)
        _itemVariableBean?.stepVal = itemIncrementValue?.toString()?.toLongOrNull() ?: 0
        _itemVariableBean?.reset()
    }
}