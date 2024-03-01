package com.angcyo.laserpacker.device

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import com.angcyo.base.dslAHelper
import com.angcyo.base.removeThis
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker._deviceSettingBean
import com.angcyo.bluetooth.fsc.laserpacker.bean.AddDeviceConfigBean
import com.angcyo.core.fragment.BaseDslFragment
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.getAllItemSelectedList
import com.angcyo.dsladapter.updateItemSelected
import com.angcyo.laserpacker.device.ble.BluetoothSearchHelper
import com.angcyo.laserpacker.device.wifi.AddWifiDeviceFragment
import com.angcyo.library.ex._color
import com.angcyo.library.ex._string
import com.angcyo.library.model.singlePage
import com.angcyo.widget.DslViewHolder

/**
 * 添加设备界面
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2024/03/01
 */
class AddDeviceFragment : BaseDslFragment() {

    init {
        fragmentTitle = _string(R.string.add_wifi_device_title)
        fragmentConfig.isLightStyle = true
        fragmentConfig.fragmentBackgroundDrawable =
            ColorDrawable(_color(R.color.lib_theme_white_color))

        contentLayoutId = R.layout.layout_add_device

        singlePage()
    }

    override fun initBaseView(savedInstanceState: Bundle?) {
        super.initBaseView(savedInstanceState)

        loadDataEnd(AddDeviceItem::class, _deviceSettingBean?.addDeviceList, null) {
            observeItemSelectedChange {
                val itemList = _adapter.getAllItemSelectedList()
                _vh.enable(R.id.next_button, itemList.isNotEmpty())
            }
        }

        _vh.click(R.id.next_button) {
            val item = _adapter.getAllItemSelectedList().firstOrNull()
            (item as? AddDeviceItem)?.itemDeviceConfigBean?.let {
                //选择了设备
                if (it.channelPage == AddDeviceConfigBean.CHANNEL_PAGE_BLE) {
                    /*removeThis()
                    onMainDelay {
                        BluetoothSearchHelper.checkAndSearchDevice()
                    }*/
                    BluetoothSearchHelper.checkAndSearchDevice(this)
                } else if (it.channelPage == AddDeviceConfigBean.CHANNEL_PAGE_WIFI) {
                    removeThis()
                    context.dslAHelper {
                        start(AddWifiDeviceFragment::class)
                    }
                } else if (it.channelPage == AddDeviceConfigBean.CHANNEL_PAGE_HTTP) {
                    removeThis()
                    context.dslAHelper {
                        start(AddWifiDeviceFragment::class)
                    }
                }
            }
        }
    }
}

class AddDeviceItem : DslAdapterItem() {

    val itemDeviceConfigBean: AddDeviceConfigBean?
        get() = itemData as? AddDeviceConfigBean

    init {
        itemLayoutId = R.layout.item_add_device_layout
        itemSingleSelectMutex = true

        itemClick = {
            if (!itemIsSelected) {
                updateItemSelected(true)
            }
        }
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.selected(R.id.lib_item_root_layout, itemIsSelected)
        itemHolder.visible(R.id.lib_check_view, itemIsSelected)

        itemHolder.tv(R.id.lib_text_view)?.text = itemDeviceConfigBean?.name
        itemHolder.img(R.id.lib_image_view)?.setImageResource(
            if (LaserPeckerHelper.LI.equals(itemDeviceConfigBean?.name, true)) {
                R.mipmap.device_l1
            } else if (LaserPeckerHelper.LII.equals(itemDeviceConfigBean?.name, true)) {
                R.mipmap.device_l2
            } else if (LaserPeckerHelper.LIII.equals(itemDeviceConfigBean?.name, true)) {
                R.mipmap.device_l3
            } else if (LaserPeckerHelper.LIV.equals(itemDeviceConfigBean?.name, true)) {
                R.mipmap.device_l4
            } else if (LaserPeckerHelper.LV.equals(itemDeviceConfigBean?.name, true)) {
                R.mipmap.device_l5
            } else if (LaserPeckerHelper.CI.equals(itemDeviceConfigBean?.name, true)) {
                R.mipmap.device_c1
            } else if (LaserPeckerHelper.LX2.equals(itemDeviceConfigBean?.name, true)) {
                R.mipmap.device_lx2
            } else {
                R.mipmap.device_l1
            }
        )

    }
}