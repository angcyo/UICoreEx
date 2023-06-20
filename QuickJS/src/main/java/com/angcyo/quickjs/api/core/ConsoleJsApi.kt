package com.angcyo.quickjs.api.core

import android.webkit.JavascriptInterface
import androidx.annotation.Keep
import com.angcyo.http.base.toJson
import com.angcyo.library.L
import com.angcyo.quickjs.api.BaseJSInterface
import com.angcyo.quickjs.api.toMap
import com.quickjs.JSObject

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/06/20
 */

@Keep
class ConsoleJsApi : BaseJSInterface() {

    private var count = 0

    override val interfaceName: String = "console"

    @JavascriptInterface
    fun count() = count

    @JavascriptInterface
    fun log(msg: String?) {
        count++
        L.d(msg)
    }

    @JavascriptInterface
    fun logObj(obj: JSObject?): String? {
        count++
        val result = obj?.toMap()?.toString()
        L.d(result)
        return result
    }

    @JavascriptInterface
    fun logObjJson(obj: JSObject?): String? {
        count++
        val result = obj?.toMap()?.toJson()
        L.d(result)
        return result
    }

    //---

    @JavascriptInterface
    fun info(msg: String?) {
        count++
        L.d(msg)
    }

    @JavascriptInterface
    fun infoObj(obj: JSObject?): String? {
        count++
        val result = obj?.toMap()?.toString()
        L.i(result)
        return result
    }

    @JavascriptInterface
    fun infoObjJson(obj: JSObject?): String? {
        count++
        val result = obj?.toMap()?.toJson()
        L.i(result)
        return result
    }

    //---

    @JavascriptInterface
    fun warn(msg: String?) {
        count++
        L.d(msg)
    }

    @JavascriptInterface
    fun warnObj(obj: JSObject?): String? {
        count++
        val result = obj?.toMap()?.toString()
        L.w(result)
        return result
    }

    @JavascriptInterface
    fun warnObjJson(obj: JSObject?): String? {
        count++
        val result = obj?.toMap()?.toJson()
        L.w(result)
        return result
    }

    //---

    @JavascriptInterface
    fun error(msg: String?) {
        count++
        L.d(msg)
    }

    @JavascriptInterface
    fun errorObj(obj: JSObject?): String? {
        count++
        val result = obj?.toMap()?.toString()
        L.e(result)
        return result
    }

    @JavascriptInterface
    fun errorObjJson(obj: JSObject?): String? {
        count++
        val result = obj?.toMap()?.toJson()
        L.e(result)
        return result
    }

}

