package com.angcyo.engrave.dslitem.engrave

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.progress.DslProgressBar

/**
 * 数据传输中的item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/27
 */
class DataTransmittingItem : DslAdapterItem() {

    /**进度*/
    var itemProgress: Int = 0

    init {
        itemLayoutId = R.layout.item_data_transmitting_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.v<DslProgressBar>(R.id.lib_progress_bar)?.setProgress(itemProgress)
    }
}