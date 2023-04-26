package com.angcyo.laserpacker.device.ble.dslitem

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.DslPropertySwitchItem
import com.angcyo.item.style.EditItemConfig
import com.angcyo.item.style.IEditItem
import com.angcyo.item.style.itemEditText
import com.angcyo.item.style.itemSwitchChecked
import com.angcyo.laserpacker.device.R
import com.angcyo.library.component.hawk.LibLpHawkKeys

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/04/26
 */
class DebugWifiConfigItem : DslPropertySwitchItem(), IEditItem {

    override var editItemConfig: EditItemConfig = EditItemConfig().apply {
        itemEditTextStyle.hint = "服务地址: ip:port"
        itemEditTextStyle.editMaxInputLength = 100//最大字符
    }

    init {
        itemLayoutId = R.layout.item_debug_wifi_config
        itemSwitchChecked = LibLpHawkKeys.enableWifiConfig
        itemEditText = LibLpHawkKeys.wifiAddress
    }

    override fun onItemChangeListener(item: DslAdapterItem) {
        //super.onItemChangeListener(item)
        LibLpHawkKeys.enableWifiConfig = itemSwitchChecked
        LibLpHawkKeys.wifiAddress = "$itemEditText"
    }
}