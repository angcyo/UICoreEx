package com.angcyo.canvas2.laser.pecker.dialog.dslitem

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dialog.ParameterComparisonTableDialogConfig
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.eachItem
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
                updateTablePreview()
            }
        }
    }

    fun updateTablePreview() {
        itemDslAdapter?.eachItem { index, dslAdapterItem ->
            if (dslAdapterItem is TablePreviewItem) {
                dslAdapterItem.updatePreview()
            }
        }
    }

}