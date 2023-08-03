package com.angcyo.laserpacker.project.dslitem

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.laserpacker.device.R
import com.angcyo.library.ex.have
import com.angcyo.library.extend.IFilterItem
import com.angcyo.library.toastQQ
import com.angcyo.widget.DslViewHolder

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/08/03
 */
class LpbFileItem : DslAdapterItem(), IFilterItem {

    /**文件名*/
    var itemFileName: CharSequence? = null

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

        itemHolder.tv(R.id.lib_text_view)?.text = itemFileName

        itemHolder.click(R.id.preview_view) {
            toastQQ("功能开发中")
        }

        itemHolder.click(R.id.engrave_view) {
            toastQQ("功能开发中")
        }
    }

    override fun containsFilterText(text: CharSequence): Boolean = itemFileName.have(text)
}
