package com.angcyo.laserpacker.device.wifi

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import com.angcyo.core.fragment.BaseDslFragment
import com.angcyo.core.fragment.bigTitleLayout
import com.angcyo.dsladapter.paddingVertical
import com.angcyo.item.DslLabelTextItem
import com.angcyo.item.style.itemLabelText
import com.angcyo.laserpacker.device.R
import com.angcyo.laserpacker.device.wifi.dslitem.AddWifiEmptyItem
import com.angcyo.laserpacker.device.wifi.dslitem.AddWifiRadarScanItem
import com.angcyo.library.ex._dimen
import com.angcyo.library.ex.getColor

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
            ColorDrawable(getColor(R.color.lib_theme_white_color))
        bigTitleLayout()
    }

    override fun initBaseView(savedInstanceState: Bundle?) {
        super.initBaseView(savedInstanceState)
        appendRightItem(ico = R.drawable.lib_refresh) {

        }

        renderDslAdapter {
            DslLabelTextItem()() {
                itemLabelText = "正在扫描附近可添加的设备..."
                paddingVertical(_dimen(R.dimen.lib_hdpi))
            }

            AddWifiRadarScanItem()()
        }

        _vh.postDelay(2_000) {
            renderDslAdapter(true) {
                DslLabelTextItem()() {
                    itemLabelText = "正在扫描附近可添加的设备..."
                    paddingVertical(_dimen(R.dimen.lib_hdpi))
                }
                AddWifiEmptyItem()()
            }
        }
    }

}