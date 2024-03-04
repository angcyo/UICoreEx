package com.angcyo.laserpacker.device.wifi.dslitem

import android.provider.Settings
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.laserpacker.device.R
import com.angcyo.library.ex.startIntent
import com.angcyo.library.ex.toStr
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.listenerTextChange

/**
 * 账号密码配置
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2023/07/30
 */
class AddWifiConfigItem : DslAdapterItem() {

    /**wifi的名称*/
    var itemWifiName: String? = null

    /**wifi的密码*/
    var itemWifiPassword: String? = null

    var _selectWifiHappened = false

    init {
        itemLayoutId = R.layout.item_add_wifi_config_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.ev(R.id.wifi_name_edit_text)?.listenerTextChange(itemWifiName) {
            itemWifiName = it.toStr()
            itemChanging = true
        }
        itemHolder.ev(R.id.wifi_password_edit_text)?.listenerTextChange(itemWifiPassword) {
            itemWifiPassword = it.toStr()
            itemChanging = true
        }
        itemHolder.check(
            R.id.remember_password_box,
            HawkEngraveKeys.rememberWifiPassword
        ) { _, isChecked ->
            HawkEngraveKeys.rememberWifiPassword = isChecked
        }

        itemHolder.click(R.id.select_wifi_button) {
            _selectWifiHappened = true
            it.context.startIntent {
                action = Settings.ACTION_WIFI_SETTINGS
            }
        }
        _selectWifiHappened = false
    }

    override fun onItemChangeListener(item: DslAdapterItem) {
        //super.onItemChangeListener(item)
    }
}