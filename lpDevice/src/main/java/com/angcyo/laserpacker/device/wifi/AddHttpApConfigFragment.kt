package com.angcyo.laserpacker.device.wifi

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.provider.Settings
import com.angcyo.base.dslFHelper
import com.angcyo.core.fragment.BaseDslFragment
import com.angcyo.core.fragment.bigTitleLayout
import com.angcyo.laserpacker.device.R
import com.angcyo.laserpacker.device.wifi.AddWifiConfigFragment.Companion.KEY_IS_CONFIG_AP_DEVICE
import com.angcyo.library.ex._color
import com.angcyo.library.ex._string
import com.angcyo.library.ex.getWifiSSID
import com.angcyo.library.ex.startIntent
import com.angcyo.putData

/**
 * A配网, 提示连接Ap界面
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2024/03/01
 */
class AddHttpApConfigFragment : BaseDslFragment() {

    companion object {
        /**ap名称, 同时也是host.local请求地址*/
        var wifiSsid: String? = null
    }

    init {
        fragmentTitle = _string(R.string.add_wifi_device_title)
        fragmentConfig.isLightStyle = true
        fragmentConfig.fragmentBackgroundDrawable =
            ColorDrawable(_color(R.color.lib_theme_white_color))
        bigTitleLayout()

        contentLayoutId = R.layout.layout_add_http_ap_config
    }

    override fun initBaseView(savedInstanceState: Bundle?) {
        super.initBaseView(savedInstanceState)

        renderDslAdapter {
            AddHttpDeviceStep1Item()() {
                itemTipText = _string(R.string.ap_connect_tip2)
            }
        }

        _vh.click(R.id.open_ap_button) {
            //打开系统wifi列表
            it.context.startIntent {
                action = Settings.ACTION_WIFI_SETTINGS
            }
        }

        _vh.click(R.id.next_button) {
            dslFHelper {
                show(AddWifiStateFragment::class) {
                    putData(true, KEY_IS_CONFIG_AP_DEVICE)
                }
            }
        }
    }

    override fun onFragmentShow(bundle: Bundle?) {
        super.onFragmentShow(bundle)
        wifiSsid = getWifiSSID() ?: wifiSsid
    }
}