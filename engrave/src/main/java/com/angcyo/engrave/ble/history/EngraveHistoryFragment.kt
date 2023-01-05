package com.angcyo.engrave.ble.history

import com.angcyo.core.fragment.BasePagerFragment
import com.angcyo.engrave.R
import com.angcyo.library.ex._string

/**
 * 历史文档界面
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/05
 */
class EngraveHistoryFragment : BasePagerFragment() {

    init {
        fragmentTitle = _string(R.string.ui_slip_menu_history)
        fragmentConfig.isLightStyle = true

        addPage(_string(R.string.app_history_title), EngraveAppHistoryFragment::class.java)
        addPage(_string(R.string.device_history_title), EngraveDeviceHistoryFragment::class.java)
    }
}