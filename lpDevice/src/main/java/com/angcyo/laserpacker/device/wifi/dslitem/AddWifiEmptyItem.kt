package com.angcyo.laserpacker.device.wifi.dslitem

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.laserpacker.device.R
import com.angcyo.laserpacker.device.ble.BluetoothSearchHelper
import com.angcyo.library.ex.ClickAction
import com.angcyo.widget.DslViewHolder

/**
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2023/07/30
 */
class AddWifiEmptyItem : DslAdapterItem() {

    /**刷新回调*/
    var itemRefreshActon: ClickAction? = null

    init {
        itemLayoutId = R.layout.item_add_wifi_empty_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
        itemHolder.click(R.id.contact_me_view) {
            BluetoothSearchHelper.ON_CONTACT_ME_ACTION?.invoke()
        }
        itemHolder.click(R.id.refresh_button) {
            itemRefreshActon?.invoke(it)
        }
    }

}