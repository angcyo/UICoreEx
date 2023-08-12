package com.angcyo.tbs.handler

import androidx.fragment.app.Fragment
import com.angcyo.base.back
import com.angcyo.base.dslAHelper
import com.angcyo.core.component.file.writeToLog
import com.angcyo.http.base.getString
import com.angcyo.http.base.toJsonElement
import com.angcyo.library.L
import com.angcyo.library._navBarHeight
import com.angcyo.library._screenHeight
import com.angcyo.library._screenWidth
import com.angcyo.library._statusBarHeight
import com.angcyo.library.ex.hawkGetString
import com.angcyo.library.ex.hawkPut
import com.angcyo.library.getAppVersionCode
import com.angcyo.library.getAppVersionName
import com.angcyo.library.toastQQ
import com.angcyo.tbs.core.inner.TbsWebView
import com.angcyo.tbs.openSingle

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
        webView.registerHandler("getValue") { data, function ->
            function?.onCallBack(data?.hawkGetString())
        }
        webView.registerHandler("setValue") { data, function ->
            val json = data?.toJsonElement()
            val key = json?.getString("key")
            val value = json?.getString("value")
            function?.onCallBack("${key?.hawkPut(value) ?: false}")
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
        webView.registerHandler("getNavBarHeight") { data, function ->
            function?.onCallBack("$_navBarHeight")
        }
        webView.registerHandler("getScreenWidth") { data, function ->
            function?.onCallBack("$_screenWidth")
        }
        webView.registerHandler("getScreenHeight") { data, function ->
            function?.onCallBack("$_screenHeight")
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
        webView.registerHandler("open") { data, function ->
            fragment.dslAHelper {
                openSingle(data)
            }
            function?.onCallBack("true")
        }
    }
}