package com.angcyo.tbs.handler

import androidx.fragment.app.Fragment
import com.angcyo.base.back
import com.angcyo.core.component.file.writeToLog
import com.angcyo.library.L
import com.angcyo.library._statusBarHeight
import com.angcyo.library.getAppVersionCode
import com.angcyo.library.getAppVersionName
import com.angcyo.library.toastQQ
import com.angcyo.tbs.core.inner.TbsWebView

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/08/11
 */
class CoreInject : IWebInject {
    override fun inject(fragment: Fragment, webView: TbsWebView) {

        //
        webView.registerHandler("toast") { data, function ->
            toastQQ(data)
            function?.onCallBack("true")
        }
        webView.registerHandler("log") { data, function ->
            data?.writeToLog(logLevel = L.DEBUG)
            function?.onCallBack("true")
        }

        //
        webView.registerHandler("getAppVersionName") { data, function ->
            function?.onCallBack(getAppVersionName())
        }
        webView.registerHandler("getAppVersionCode") { data, function ->
            function?.onCallBack("${getAppVersionCode()}")
        }
        webView.registerHandler("getStatusBarHeight") { data, function ->
            function?.onCallBack("$_statusBarHeight")
        }

        //
        webView.registerHandler("back") { data, function ->
            fragment.back()
            function?.onCallBack("true")
        }
        webView.registerHandler("finish") { data, function ->
            fragment.activity?.finish()
            function?.onCallBack("true")
        }
    }
}