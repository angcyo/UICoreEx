package com.angcyo.canvas2.laser.pecker.dialog.dslitem

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dialog.ParameterComparisonTableDialogConfig
import com.angcyo.canvas2.laser.pecker.dialog.updateTablePreview
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.clearListeners
import com.angcyo.widget.base.onTextChange
import com.angcyo.widget.base.setInputText

/**
 * 额外需要追加的行列范围
 * [com.angcyo.canvas2.laser.pecker.dialog.ParameterComparisonTableDialogConfig.Companion.isRowColumnInRange]
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/04/21
 */
class RowsColumnsRangeItem : DslAdapterItem() {

    companion object {
        /**行列是否在指定的范围内*/
        fun isRowColumnInRange(row: Int, column: Int): Boolean {
            ParameterComparisonTableDialogConfig.rowsColumnsRange.split(" ").forEach { rls -> //行:列
                val rlList = if (rls.contains(":")) rls.split(":") else rls.split(".") //行 列
                val r = rlList.getOrNull(0)?.toIntOrNull()
                val c = rlList.getOrNull(1)?.toIntOrNull()
                if (r == null && c == null) {
                    //无效数据
                } else if (r == null) {
                    //列所有
                    if (c == column) {
                        return true
                    }
                } else if (c == null) {
                    //行所有
                    if (r == row) {
                        return true
                    }
                } else {
                    //行列都指定
                    val cList =
                        rlList.subList(1, rlList.size).map { it.toIntOrNull() ?: -1 } //所有指定的列
                    if (r == row && cList.contains(column)) {
                        return true
                    }
                }
            }
            return false
        }
    }

    init {
        itemLayoutId = R.layout.item_rows_columns__layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
        itemHolder.ev(R.id.lib_edit_view)?.apply {
            clearListeners()
            setInputText(ParameterComparisonTableDialogConfig.rowsColumnsRange, false)
            onTextChange {
                ParameterComparisonTableDialogConfig.rowsColumnsRange = "$it"
                itemDslAdapter.updateTablePreview()
            }
        }
    }
}