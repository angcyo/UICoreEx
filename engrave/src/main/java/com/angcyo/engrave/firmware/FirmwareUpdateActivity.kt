package com.angcyo.engrave.firmware

import android.os.Bundle
import com.angcyo.activity.BaseAppCompatActivity
import com.angcyo.base.dslFHelper
import com.angcyo.engrave.firmware.FirmwareUpdateFragment

/**
 * 固件升级界面
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/08
 */
class FirmwareUpdateActivity : BaseAppCompatActivity() {

    override fun onCreateAfter(savedInstanceState: Bundle?) {
        super.onCreateAfter(savedInstanceState)
        dslFHelper {
            restore(FirmwareUpdateFragment::class)
        }
    }
}