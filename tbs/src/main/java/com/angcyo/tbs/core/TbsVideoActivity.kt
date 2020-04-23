package com.angcyo.tbs.core

import android.content.res.Configuration
import android.os.Bundle
import com.angcyo.activity.showDebugInfoView
import com.angcyo.base.dslAHelper
import com.angcyo.library.ex.isDebug
import com.tencent.smtt.sdk.VideoActivity

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/08
 */
class TbsVideoActivity : VideoActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(null)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        onShowDebugInfoView(hasFocus)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (isDebug()) {
            window.decorView.postDelayed({
                onShowDebugInfoView()
            }, 300)
        }
    }

    fun onShowDebugInfoView(show: Boolean = true) {
        showDebugInfoView(show)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        //L.d("TbsVideoActivity...")
    }

    override fun finish() {
        super.finish()
        //L.d("TbsVideoActivity...")
        dslAHelper { onFinish() }
    }
}