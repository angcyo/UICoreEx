package com.angcyo.engrave.dslitem.transfer

import com.angcyo.core.vmApp
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.engrave.model.TransferModel
import com.angcyo.engrave.transition.DataException
import com.angcyo.engrave.transition.EmptyException
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

        if (itemException is EmptyException) {
            itemHolder.tv(R.id.error_text_view)?.text = "No data needs to be transmitted!"
        } else if (itemException is DataException) {
            itemHolder.tv(R.id.error_text_view)?.text = "data exception!"
        }

        itemHolder.click(R.id.lib_retry_button) {
            vmApp<TransferModel>().retryTransfer(false)
        }
    }

}