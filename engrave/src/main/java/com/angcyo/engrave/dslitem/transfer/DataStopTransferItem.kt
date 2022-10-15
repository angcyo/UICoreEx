package com.angcyo.engrave.dslitem.transfer

import com.angcyo.core.vmApp
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.engrave.model.TransferModel
import com.angcyo.item.BaseButtonItem
import com.angcyo.widget.DslViewHolder

/**
 * 结束传输的item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/27
 */
class DataStopTransferItem : BaseButtonItem() {

    /**数据传输是否有异常*/
    var itemException: Throwable? = null

    init {
        itemLayoutId = R.layout.item_data_stop_transfer_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.visible(R.id.error_text_view, itemException != null)
        itemHolder.visible(R.id.lib_retry_button, itemException != null)

        itemHolder.click(R.id.lib_retry_button) {
            vmApp<TransferModel>().retryTransfer(false)
        }
    }

}