package com.angcyo.laserpacker.device.wifi.dslitem

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.laserpacker.device.R
import com.angcyo.library.ex.getWifiSSID
import com.angcyo.widget.DslViewHolder

/**
 * wifi 输入/状态提示
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/07/31
 */
class AddWifiConfigTipItem : DslAdapterItem() {

    init {
        itemLayoutId = R.layout.item_add_wifi_config_tip_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
        itemHolder.visible(R.id.lib_des_view, getWifiSSID().isNullOrBlank())
    }

}