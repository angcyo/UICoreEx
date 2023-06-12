package com.angcyo.quickjs.api

import com.angcyo.http.base.jsonObject
import com.angcyo.library.annotation.CallPoint
import com.angcyo.quickjs.api.core.AppJsApi
import com.angcyo.quickjs.api.core.HttpJsApi
import com.angcyo.quickjs.api.core.LJsApi
import com.angcyo.quickjs.api.core.ReflectJsApi
import com.angcyo.quickjs.api.core.TJsApi
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.quickjs.JSArray
import com.quickjs.JSContext
import com.quickjs.JSObject
import okhttp3.Headers
import org.json.JSONObject

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/06/11
 */
object Api {

    /**注入对象api到[com.quickjs.JSContext]*/
    @CallPoint
    fun inject(context: JSContext) {
        val appJsApi = AppJsApi()
        val appJs = context.injectInterface(appJsApi)
        appJsApi.init(appJs)//注入默认的属性

        //core 核心api
        appJs.injectInterface(LJsApi())
        appJs.injectInterface(TJsApi())
        appJs.injectInterface(ReflectJsApi())
        appJs.injectInterface(HttpJsApi())
    }

    /**注入对象*/
    fun JSObject.injectInterface(jsInterface: IJSInterface): JSObject {
        return addJavascriptInterface(jsInterface, jsInterface.interfaceName)
    }

}

/**类型转换*/
fun JSArray.toList(): List<Any> {
    val result = mutableListOf<Any>()
    for (i in 0 until length()) {
        get(i)?.let { result.add(it) }
    }
    return result
}

fun JSArray.toStringList(): List<String> {
    val result = mutableListOf<String>()
    for (i in 0 until length()) {
        getString(i)?.let { result.add(it) }
    }
    return result
}

fun JSArray.toDoubleList(): List<Double> {
    val result = mutableListOf<Double>()
    for (i in 0 until length()) {
        getDouble(i).let { result.add(it) }
    }
    return result
}

fun JSArray.toIntegerList(): List<Int> {
    val result = mutableListOf<Int>()
    for (i in 0 until length()) {
        getInteger(i).let { result.add(it) }
    }
    return result
}

fun JSArray.toBooleanList(): List<Boolean> {
    val result = mutableListOf<Boolean>()
    for (i in 0 until length()) {
        getBoolean(i).let { result.add(it) }
    }
    return result
}

fun JSONObject.toJsonElement(): JsonElement? {
    return try {
        JsonParser.parseString(toString())
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun JSObject.toJsonElement(): JsonElement? {
    return try {
        jsonObject {
            keys?.forEach { key ->
                get(key)?.let {
                    if (it is JSObject) {
                        add(key, it.toJsonElement())
                    } else {
                        add(key, it)
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun JSObject.toHeaders(): Headers {
    val builder = Headers.Builder()
    for (key in keys) {
        builder.add(key, getString(key))
    }
    return builder.build()
}