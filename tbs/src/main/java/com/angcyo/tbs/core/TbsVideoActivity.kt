package com.angcyo.tbs.core

import android.os.Bundle
import com.angcyo.base.dslAHelper
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