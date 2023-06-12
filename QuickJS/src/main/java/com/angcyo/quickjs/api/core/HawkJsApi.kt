package com.angcyo.quickjs.api.core

import android.webkit.JavascriptInterface
import androidx.annotation.Keep
import com.angcyo.quickjs.api.BaseJSInterface
import com.angcyo.quickjs.api.IJSInterface
import com.orhanobut.hawk.Hawk

/**
 * Hawk相关操作api
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/06/12
 */
@Keep
class HawkJsApi : BaseJSInterface() {

    override val interfaceName: String = "hawk"

    @JavascriptInterface
    fun have(key: String): Boolean = Hawk.contains(key)

    @JavascriptInterface
    fun contains(key: String): Boolean = Hawk.contains(key)

    @JavascriptInterface
    fun delete(key: String): Boolean = Hawk.delete(key)

    //---

    @JavascriptInterface
    fun putBoolean(key: String, value: Boolean?): Boolean = Hawk.put(key, value)

    @JavascriptInterface
    fun putInt(key: String, value: Int?): Boolean = Hawk.put(key, value)

    @JavascriptInterface
    fun putDouble(key: String, value: Double?): Boolean = Hawk.put(key, value)

    @JavascriptInterface
    fun putFloat(key: String, value: Double?): Boolean = Hawk.put(key, value?.toFloat())

    @JavascriptInterface
    fun putString(key: String, value: String?): Boolean = Hawk.put(key, value)

    //---

    @JavascriptInterface
    fun getBoolean(key: String, def: Boolean?): Boolean? = Hawk.get(key, def)

    @JavascriptInterface
    fun getInt(key: String, def: Int?): Int? = Hawk.get(key, def)

    @JavascriptInterface
    fun getDouble(key: String, def: Double?): Double? = Hawk.get(key, def)

    @JavascriptInterface
    fun getFloat(key: String, def: Double?): Float? = Hawk.get(key, def?.toFloat())

    @JavascriptInterface
    fun getString(key: String, def: String?): String? = Hawk.get(key, def)
}