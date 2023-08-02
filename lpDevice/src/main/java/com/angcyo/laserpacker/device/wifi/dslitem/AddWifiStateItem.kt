package com.angcyo.laserpacker.device.wifi.dslitem

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.laserpacker.device.R
import com.angcyo.laserpacker.device.ble.BluetoothSearchHelper
import com.angcyo.library.ex.Action
import com.angcyo.library.ex._string
import com.angcyo.widget.DslViewHolder

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/07/31
 */
class AddWifiStateItem : DslAdapterItem() {

    /**配网中 28%*/
    var itemDes: CharSequence? = null

    /**tip*/
    var itemTip: CharSequence? = null

    /**当前显示的状态*/
    var itemState: Int = STATE_NORMAL
        set(value) {
            field = value
            updateAdapterItem()
        }

    /**重新配置回调*/
    var itemReconfigureAction: Action? = null

    companion object {
        const val STATE_NORMAL = 0
        const val STATE_SUCCESS = 1
        const val STATE_ERROR = -1
    }

    init {
        itemLayoutId = R.layout.item_add_wifi_state_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        //radar
        //itemHolder.v<RadarScanLoadingView>(R.id.radar_scan_loading_view)?.loading(itemScan)

        itemHolder.tv(R.id.lib_des_view)?.text = when (itemState) {
            STATE_SUCCESS -> _string(R.string.wifi_connect_success)
            STATE_ERROR -> _string(R.string.wifi_connect_error)
            else -> itemDes
        }
        itemHolder.tv(R.id.lib_tip_view)?.text = when (itemState) {
            STATE_SUCCESS -> _string(R.string.wifi_connect_success_tip)
            STATE_ERROR -> _string(R.string.wifi_connect_error_tip)
            else -> itemTip
        }

        itemHolder.click(R.id.wifi_reconfigure_button) {
            itemReconfigureAction?.invoke()
        }
        itemHolder.click(R.id.contact_me_view) {
            BluetoothSearchHelper.ON_CONTACT_ME_ACTION?.invoke()
        }

        //state
        itemHolder.invisible(R.id.radar_scan_loading_view, itemState != STATE_NORMAL)
        itemHolder.visible(R.id.state_view, itemState != STATE_NORMAL)
        itemHolder.visible(R.id.contact_me_view, itemState == STATE_ERROR)
        itemHolder.visible(R.id.wifi_reconfigure_button, itemState == STATE_ERROR)
        itemHolder.img(R.id.state_view)?.apply {
            setBackgroundResource(if (itemState == STATE_SUCCESS) R.drawable.wifi_config_state_success else R.drawable.wifi_config_state_error)
            setImageResource(if (itemState == STATE_SUCCESS) R.drawable.lib_confirm else R.drawable.lib_close)
        }

    }

}