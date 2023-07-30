package com.angcyo.laserpacker.device.wifi

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import com.angcyo.core.fragment.BaseDslFragment
import com.angcyo.core.fragment.bigTitleLayout
import com.angcyo.dsladapter.paddingVertical
import com.angcyo.item.DslLabelTextItem
import com.angcyo.item.style.itemText
import com.angcyo.laserpacker.device.R
import com.angcyo.laserpacker.device.wifi.dslitem.AddWifiConfigItem
import com.angcyo.laserpacker.device.wifi.dslitem.AddWifiRadarScanItem
import com.angcyo.library.ex._color
import com.angcyo.library.ex._dimen
import com.angcyo.library.ex._string
import com.angcyo.library.ex.copyDrawable
import com.angcyo.library.ex.getWifiSSID

/**
 * wifi配网界面
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2023/07/30
 */
class AddWifiDeviceFragment : BaseDslFragment() {

    init {
        fragmentTitle = "wifi配网"
        fragmentConfig.fragmentBackgroundDrawable =
            ColorDrawable(_color(R.color.lib_theme_white_bg_color))
        bigTitleLayout()
        enableSoftInput = true
    }

    override fun initBaseView(savedInstanceState: Bundle?) {
        super.initBaseView(savedInstanceState)
        appendRightItem(ico = R.drawable.lib_refresh) {

        }

        renderDslAdapter {
            DslLabelTextItem()() {
                itemBackgroundDrawable = fragmentConfig.fragmentBackgroundDrawable?.copyDrawable()
                itemText = _string(R.string.add_wifi_device_scan_tip)
                paddingVertical(_dimen(R.dimen.lib_hdpi))
            }

            AddWifiRadarScanItem()()
        }

        _vh.postDelay(2_000) {
            renderDslAdapter(true) {
                DslLabelTextItem()() {
                    itemBackgroundDrawable =
                        fragmentConfig.fragmentBackgroundDrawable?.copyDrawable()
                    itemText = _string(R.string.add_wifi_device_name_tip)
                    paddingVertical(_dimen(R.dimen.lib_hdpi))
                }
                AddWifiConfigItem()() {
                    itemWifiName = getWifiSSID()
                }
            }
        }
    }

}