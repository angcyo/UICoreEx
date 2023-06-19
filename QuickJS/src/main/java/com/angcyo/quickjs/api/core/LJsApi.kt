package com.angcyo.quickjs.api.core

import android.webkit.JavascriptInterface
import androidx.annotation.Keep
import com.angcyo.library.L
import com.angcyo.quickjs.api.BaseJSInterface
import com.angcyo.quickjs.api.toList
import com.quickjs.JSArray

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/06/12
 */
@Keep
class LJsApi : BaseJSInterface() {

    override val interfaceName: String = "L"

    @JavascriptInterface
    fun d(msg: String?) {
        L.d(msg)
    }

    @JavascriptInterface
    fun v(msg: String?) {
        L.v(msg)
    }

    /**
     * ```
     * AppJs.L.i("返回:" + body);
     * ```
     * */
    @JavascriptInterface
    fun i(msg: String?) {
        L.i(msg)
    }

    @JavascriptInterface
    fun w(msg: String?) {
        L.w(msg)
    }

    @JavascriptInterface
    fun e(msg: String?) {
        L.e(msg)
    }

    @JavascriptInterface
    fun dA(msg: JSArray) {
        L.d(*msg.toList().toTypedArray())
    }

    @JavascriptInterface
    fun vA(msg: JSArray) {
        L.v(*msg.toList().toTypedArray())
    }

    /**
     * ```
     * AppJs.L.iA(["返回:" + body]);
     * ```
     * */
    @JavascriptInterface
    fun iA(msg: JSArray) {
        L.i(*msg.toList().toTypedArray())
    }

    @JavascriptInterface
    fun wA(msg: JSArray) {
        L.w(*msg.toList().toTypedArray())
    }

    @JavascriptInterface
    fun eA(msg: JSArray) {
        L.e(*msg.toList().toTypedArray())
    }

}