package com.angcyo.tbs.core

import android.content.Intent
import android.graphics.PixelFormat
import android.os.Bundle
import com.angcyo.activity.BaseAppCompatActivity
import com.angcyo.base.dslFHelper

/**
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/01
 */

open class TbsWebActivity : BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFormat(PixelFormat.TRANSLUCENT)
    }

    override fun onHandleIntent(intent: Intent, fromNew: Boolean) {
        super.onHandleIntent(intent, fromNew)

        val data = intent.data
        val config: TbsWebConfig? = intent.getParcelableExtra(TbsWebFragment.KEY_CONFIG)

        //参数传递
        val webConfig = config ?: TbsWebConfig(data)
        if (webConfig.uri == null) {
            webConfig.uri = data
        }

        val arg = Bundle()
        intent.extras?.run {
            if (!this.isEmpty) {
                arg.putAll(this)
            }
        }
        arg.putParcelable(TbsWebFragment.KEY_CONFIG, webConfig)

        dslFHelper {
            show(config?.targetClass ?: TbsWebFragment::class.java)
            configFragment {
                arguments = arg
            }
        }
    }
}