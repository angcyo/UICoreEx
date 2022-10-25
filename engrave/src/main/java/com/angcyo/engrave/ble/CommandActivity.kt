package com.angcyo.engrave.ble

import android.os.Bundle
import com.angcyo.activity.BaseAppCompatActivity
import com.angcyo.base.dslFHelper

/**
 * 指令列表
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/25
 */
class CommandActivity : BaseAppCompatActivity() {

    override fun onCreateAfter(savedInstanceState: Bundle?) {
        super.onCreateAfter(savedInstanceState)
        dslFHelper {
            restore(CommandFragment::class)
        }
    }

}