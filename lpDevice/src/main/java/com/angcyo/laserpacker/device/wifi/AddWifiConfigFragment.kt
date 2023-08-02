package com.angcyo.laserpacker.device.wifi

import android.Manifest
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import com.angcyo.base.dslFHelper
import com.angcyo.base.hideSoftInput
import com.angcyo.base.removeThis
import com.angcyo.core.component.dslPermissions
import com.angcyo.core.fragment.BaseDslFragment
import com.angcyo.core.fragment.bigTitleLayout
import com.angcyo.dsladapter.updateItem
import com.angcyo.dsladapter.updateItemByClass
import com.angcyo.getDataParcelable
import com.angcyo.laserpacker.device.BuildConfig
import com.angcyo.laserpacker.device.R
import com.angcyo.laserpacker.device.wifi.dslitem.AddWifiConfigItem
import com.angcyo.laserpacker.device.wifi.dslitem.AddWifiConfigTipItem
import com.angcyo.library.ex._color
import com.angcyo.library.ex._string
import com.angcyo.library.ex.getWifiSSID
import com.angcyo.library.ex.isBuildDebug
import com.angcyo.library.toastQQ
import com.angcyo.putDataParcelable
import com.clj.fastble.data.BleDevice

/**
 * wifi配网 wifi配置界面
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2023/07/30
 */
class AddWifiConfigFragment : BaseDslFragment() {

    /**选择的设备*/
    var bleDevice: BleDevice? = null

    init {
        fragmentTitle = _string(R.string.select_wifi_title)
        fragmentConfig.fragmentBackgroundDrawable =
            ColorDrawable(_color(R.color.lib_theme_white_bg_color))
        bigTitleLayout()
        enableSoftInput = true

        contentLayoutId = R.layout.layout_add_wifi_config
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bleDevice = getDataParcelable()
    }

    override fun initBaseView(savedInstanceState: Bundle?) {
        super.initBaseView(savedInstanceState)

        _vh.click(R.id.next_button) {
            dslFHelper {
                hideSoftInput()
                show(AddWifiStateFragment::class) {
                    _adapter.get<AddWifiConfigItem>().firstOrNull()?.let {
                        putDataParcelable(
                            WifiConfigBean(bleDevice!!, it.itemWifiName!!, it.itemWifiPassword!!)
                        )
                    }
                }
            }
        }

        renderDslAdapter(true) {
            AddWifiConfigTipItem()()
            AddWifiConfigItem()() {
                itemWifiName = getWifiSSID()
                observeItemChange {
                    _vh.enable(
                        R.id.next_button,
                        !itemWifiName.isNullOrBlank() && !itemWifiPassword.isNullOrBlank()
                    )
                }
                if (BuildConfig.BUILD_TYPE.isBuildDebug()) {
                    itemWifiPassword = "Hi@Yanfa123"
                    itemChanging = true
                }
            }
        }

        if (bleDevice == null) {
            removeThis()
            toastQQ(_string(R.string.core_thread_error_tip))
        }
    }

    /**[com.angcyo.laserpacker.device.ble.BluetoothSearchHelper.Companion.checkAndSearchDevice]*/
    override fun onFragmentFirstShow(bundle: Bundle?) {
        super.onFragmentFirstShow(bundle)
        dslPermissions(listOf(Manifest.permission.ACCESS_WIFI_STATE)) { allGranted, foreverDenied ->
            if (allGranted) {
                updateConfigItem(true)
            }
        }
    }

    override fun onFragmentShow(bundle: Bundle?) {
        super.onFragmentShow(bundle)
        updateConfigItem()
    }

    private fun updateConfigItem(updateSsid: Boolean? = null) {
        _adapter.updateItem { it is AddWifiConfigTipItem }
        _adapter.updateItemByClass(AddWifiConfigItem::class)?.apply {
            if (_selectWifiHappened || updateSsid == true) {
                val ssid = getWifiSSID()
                if (!ssid.isNullOrBlank()) {
                    itemWifiName = ssid
                }
            }
        }
    }
}