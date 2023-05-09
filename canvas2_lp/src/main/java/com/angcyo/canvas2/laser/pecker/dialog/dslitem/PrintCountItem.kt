package com.angcyo.canvas2.laser.pecker.dialog.dslitem

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dialog.ParameterComparisonTableDialogConfig
import com.angcyo.canvas2.laser.pecker.dialog.updateTablePreview
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.library.ex.size
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.clearListeners
import com.angcyo.widget.base.onTextChange
import com.angcyo.widget.base.setInputText

/**
 * 指定 行.列.次数 的打印次数
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/05/09
 */
class PrintCountItem : DslAdapterItem() {

    companion object {

        /**获取打印次数*/
        fun getPrintCount(depthIndex: Int, powerIndex: Int): Int {
            var result = 1
            ParameterComparisonTableDialogConfig.pctPrintCount.split(" ").forEach { str ->
                val list = str.split(".") //行.列.次数
                if (list.size() >= 3) {
                    val r = list.getOrNull(0)?.toIntOrNull()
                    val c = list.getOrNull(1)?.toIntOrNull()
                    val p = list.getOrNull(2)?.toIntOrNull()
                    if (r == depthIndex && c == powerIndex) {
                        result = p ?: result
                        return result
                    }
                }
            }
            return result
        }
    }

    init {
        itemLayoutId = R.layout.item_print_count_layout
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
            setInputText(ParameterComparisonTableDialogConfig.pctPrintCount, false)
            onTextChange {
                ParameterComparisonTableDialogConfig.pctPrintCount = "$it"
                itemDslAdapter.updateTablePreview()
            }
        }
    }
}