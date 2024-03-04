package com.angcyo.laserpacker.device.wifi

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import com.angcyo.base.dslFHelper
import com.angcyo.bluetooth.fsc.FscBleApiModel
import com.angcyo.bluetooth.fsc.WifiApiModel
import com.angcyo.core.fragment.BaseDslFragment
import com.angcyo.core.fragment.bigTitleLayout
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.laserpacker.device.R
import com.angcyo.laserpacker.device.model.FscDeviceModel
import com.angcyo.laserpacker.device.wifi.AddWifiConfigFragment.Companion.KEY_IS_CONFIG_AP_DEVICE
import com.angcyo.library.component.startCountDown
import com.angcyo.library.ex._color
import com.angcyo.library.ex._string
import com.angcyo.library.ex.ceilInt
import com.angcyo.putData
import com.angcyo.widget.DslViewHolder

/**
 * Ap配网, 第一步界面
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2024/03/01
 */
class AddHttpDeviceFragment : BaseDslFragment() {

    init {
        fragmentTitle = _string(R.string.add_wifi_device_title)
        fragmentConfig.isLightStyle = true
        fragmentConfig.fragmentBackgroundDrawable =
            ColorDrawable(_color(R.color.lib_theme_white_color))
        bigTitleLayout()

        contentLayoutId = R.layout.layout_add_wifi_device

        //2023-8-1 lp5 ble
        vmApp<FscBleApiModel>().disconnectAll()
        vmApp<WifiApiModel>().disconnectAll()

        //2023-8-21 禁用自动连接
        FscDeviceModel.disableAutoConnect()
    }

    override fun initBaseView(savedInstanceState: Bundle?) {
        super.initBaseView(savedInstanceState)

        renderDslAdapter {
            AddHttpDeviceStep1Item()() {
                itemTipText = _string(R.string.ap_connect_tip)
            }
        }

        _vh.visible(R.id.bind_wifi_wrap_layout)
        _vh.enable(R.id.bind_wifi_button, false)
        _vh.tv(R.id.bind_wifi_button)?.text = _string(R.string.confirm_next_label)

        startCountDown(3) {
            if (it == 0L) {
                _vh.enable(R.id.bind_wifi_button, true)
                _vh.tv(R.id.bind_wifi_button)?.text = _string(R.string.confirm_next_label)
                _vh.click(R.id.bind_wifi_button) {
                    dslFHelper {
                        //finishActivityOnLastFragmentRemove = false
                        remove(this@AddHttpDeviceFragment)
                        show(AddWifiConfigFragment()) {
                            putData(true, KEY_IS_CONFIG_AP_DEVICE)
                        }
                    }
                }
            } else {
                _vh.tv(R.id.bind_wifi_button)?.text =
                    "${_string(R.string.confirm_next_label)}(${(it / 1000f).ceilInt()})"
            }
        }
    }
}

class AddHttpDeviceStep1Item : DslAdapterItem() {

    var itemTipText: CharSequence? = null

    init {
        itemLayoutId = R.layout.item_add_http_device_step1
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.tv(R.id.lib_text_view)?.text = itemTipText
    }
}