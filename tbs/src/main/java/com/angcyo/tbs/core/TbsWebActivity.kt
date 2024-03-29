package com.angcyo.tbs.core

import android.app.SearchManager
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Bundle
import com.angcyo.activity.BaseAppCompatActivity
import com.angcyo.base.dslFHelper
import com.angcyo.library.model.WebConfig
import com.angcyo.tbs.DslTbs

/**
 * [TbsWebFragment] 容器
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/01
 */

open class TbsWebActivity : BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFormat(PixelFormat.TRANSLUCENT)
    }

    override fun onHandleIntent(intent: Intent, fromNewIntent: Boolean) {
        super.onHandleIntent(intent, fromNewIntent)
        parseTbsWebConfig(intent, fromNewIntent)
    }

    open fun parseTbsWebConfig(intent: Intent, fromNewIntent: Boolean) {
        //https://developer.android.google.cn/guide/topics/search

        var searchUri: Uri? = null
        if (intent.action == Intent.ACTION_WEB_SEARCH) {
            val searchWord = intent.getStringExtra(SearchManager.QUERY)
            val searchEngine = "https://m.baidu.com/s?from=angcyo&wd="
            searchUri = Uri.parse("$searchEngine$searchWord")
        }

        val data = searchUri ?: intent.data
        val config: WebConfig? = intent.getParcelableExtra(WebConfig.KEY_CONFIG)

        //参数传递
        val webConfig = config ?: WebConfig(data)
        if (webConfig.uri == null) {
            webConfig.uri = data
        }

        val arg = Bundle()
        intent.extras?.run {
            if (!this.isEmpty) {
                arg.putAll(this)
            }
        }
        arg.putParcelable(WebConfig.KEY_CONFIG, webConfig)

        dslFHelper {
            show(config?.targetClass ?: DslTbs.DEF_TBS_FRAGMENT ?: TbsWebFragment::class.java)
            configFragment {
                arguments = arg
            }
        }
    }
}