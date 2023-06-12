package com.angcyo.quickjs.api.core

import android.webkit.JavascriptInterface
import androidx.annotation.Keep
import com.angcyo.library.toast
import com.angcyo.library.toastQQ
import com.angcyo.library.toastWX
import com.angcyo.quickjs.api.IJSInterface

/**
 * Toast api 通知
 * ```
 * AppJs.T.showQQ("angcyo")
 * AppJs.T.showWX("angcyo")
 * AppJs.T.show("angcyo")
 * ```
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/06/11
 */
@Keep
class TJsApi : IJSInterface {

    override val interfaceName: String = "T"

    @JavascriptInterface
    fun show(msg: String?) {
        toast(msg)
    }

    @JavascriptInterface
    fun showQQ(msg: String?) {
        toastQQ(msg)
    }

    @JavascriptInterface
    fun showWX(msg: String?) {
        toastWX(msg)
    }

}