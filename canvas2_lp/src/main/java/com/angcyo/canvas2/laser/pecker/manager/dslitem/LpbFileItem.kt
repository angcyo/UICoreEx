package com.angcyo.canvas2.laser.pecker.manager.dslitem

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.laserpacker.device.R
import com.angcyo.library.ex.StringAction
import com.angcyo.library.ex.have
import com.angcyo.library.ex.isDebug
import com.angcyo.library.extend.IFilterItem
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.span.span

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/08/03
 */
class LpbFileItem : DslAdapterItem(), IFilterItem {

    /**文件名*/
    var itemFileName: String? = null

    /**文件索引*/
    var itemFileIndex: Int? = null

    /**开始预览*/
    var itemPreviewAction: StringAction? = null

    /**开始雕刻*/
    var itemEngraveAction: StringAction? = null

    init {
        itemLayoutId = R.layout.item_lpb_file
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.tv(R.id.lib_text_view)?.text = span {
            if (isDebug()) {
                itemFileIndex?.let {
                    append(it.toString())
                    appendln()
                }
            }
            itemFileName?.let {
                append(it)
            }
        }

        itemHolder.click(R.id.preview_view) {
            itemPreviewAction?.invoke(itemFileName)
        }

        itemHolder.click(R.id.engrave_view) {
            itemEngraveAction?.invoke(itemFileName)
        }
    }

    override fun containsFilterText(text: CharSequence): Boolean = itemFileName.have(text)
}
