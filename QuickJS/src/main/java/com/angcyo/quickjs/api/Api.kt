package com.angcyo.quickjs.api

import com.angcyo.http.base.jsonObject
import com.angcyo.library.annotation.CallPoint
import com.angcyo.quickjs.api.core.AppJsApi
import com.angcyo.quickjs.api.core.ConsoleJsApi
import com.angcyo.quickjs.api.core.DeviceJsApi
import com.angcyo.quickjs.api.core.DialogJsApi
import com.angcyo.quickjs.api.core.FileJsApi
import com.angcyo.quickjs.api.core.HawkJsApi
import com.angcyo.quickjs.api.core.HttpJsApi
import com.angcyo.quickjs.api.core.LJsApi
import com.angcyo.quickjs.api.core.LoopJsApi
import com.angcyo.quickjs.api.core.ReflectJsApi
import com.angcyo.quickjs.api.core.TJsApi
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.quickjs.JSArray
import com.quickjs.JSContext
import com.quickjs.JSObject
import com.quickjs.JSValue
import okhttp3.Headers
import org.json.JSONObject

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/06/11
 */

/**注入对象的回调*/
typealias InjectAction = (context: JSContext, appJs: JSObject) -> Unit

object Api {

    /**注入行为*/
    val injectApiAction = mutableListOf<InjectAction>()

    /**注入对象api到[com.quickjs.JSContext]*/
    @CallPoint
    fun inject(context: JSContext) {
        //全局变量
        //context.set()
        context.injectInterface(ConsoleJsApi())

        //AppJs 接口
        val appJsApi = AppJsApi()
        val appJs = context.injectInterface(appJsApi)
        appJsApi.init(appJs)//注入默认的属性

        //core 核心api
        appJs.injectInterface(LJsApi())
        appJs.injectInterface(TJsApi())
        appJs.injectInterface(LoopJsApi())
        appJs.injectInterface(DeviceJsApi())
        appJs.injectInterface(HawkJsApi())
        appJs.injectInterface(FileJsApi())
        appJs.injectInterface(ReflectJsApi())
        appJs.injectInterface(HttpJsApi())
        appJs.injectInterface(DialogJsApi())

        //自定义api
        for (action in injectApiAction) {
            try {
                action(context, appJs)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**额外注入对象的回调*/
    fun injectApiAction(action: InjectAction) {
        injectApiAction.add(action)
    }
}

/**注入对象*/
@CallPoint
fun JSObject.injectInterface(jsInterface: IJSInterface): JSObject {
    val jsObject = addJavascriptInterface(jsInterface, jsInterface.interfaceName)
    jsInterface.jsContext = context
    jsInterface.jsObject = jsObject
    jsInterface.onInject(this)
    return jsObject
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

fun JSArray.toByteList(): List<Byte> {
    val result = mutableListOf<Byte>()
    for (i in 0 until length()) {
        getInteger(i).let { result.add(it.toByte()) }
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

/**[toMap]*/
fun JSObject.toHeaders(): Headers {
    val builder = Headers.Builder()
    for (key in keys) {
        try {
            builder.add(key, getString(key))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return builder.build()
}

/**[toHeaders]*/
fun JSObject.toMap(): Map<String, Any?> {
    val result = mutableMapOf<String, Any?>()
    for (key in keys) {
        try {
            result[key] = get(key)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return result
}

//---

fun JSObject.getOrInt(key: String, def: Int = 0): Int {
    getType(key)?.let {
        if (it == JSValue.TYPE.INTEGER) {
            return getInteger(key)
        }
    }
    return def
}

fun JSObject.getOrLong(key: String, def: Long = 0): Long {
    getType(key)?.let {
        if (it == JSValue.TYPE.INTEGER) {
            return getInteger(key).toLong()
        }
    }
    return def
}

fun JSObject.getOrDouble(key: String, def: Double = 0.0): Double {
    getType(key)?.let {
        if (it == JSValue.TYPE.INTEGER) {
            return getInteger(key).toDouble()
        } else if (it == JSValue.TYPE.DOUBLE) {
            return getDouble(key)
        }
    }
    return def
}

fun JSObject.getOrBoolean(key: String, def: Boolean = false): Boolean {
    getType(key)?.let {
        if (it == JSValue.TYPE.BOOLEAN) {
            return getBoolean(key)
        }
    }
    return def
}

fun JSObject.getOrString(key: String, def: String = ""): String {
    getType(key)?.let {
        if (it == JSValue.TYPE.STRING) {
            return getString(key) ?: def
        }
    }
    return def
}

fun JSObject.getOrObject(key: String, def: JSObject): JSObject {
    getType(key)?.let {
        if (it == JSValue.TYPE.JS_OBJECT) {
            return getObject(key) ?: def
        }
    }
    return def
}

fun JSObject.getOrArray(key: String, def: JSArray): JSArray {
    getType(key)?.let {
        if (it == JSValue.TYPE.JS_ARRAY) {
            return getArray(key) ?: def
        }
    }
    return def
}

fun JSObject.executeFunction(name: String, parameters: JSArray): Any? {
    getType(name)?.let {
        if (it == JSValue.TYPE.JS_FUNCTION) {
            return executeFunction(name, parameters)
        }
    }
    return null
}

fun JSObject.getOrNull(key: String, def: Any?): Any? {
    getType(key)?.let {
        return when (it) {
            JSValue.TYPE.INTEGER -> getInteger(key)
            JSValue.TYPE.DOUBLE -> getDouble(key)
            JSValue.TYPE.BOOLEAN -> getBoolean(key)
            JSValue.TYPE.STRING -> getString(key)
            JSValue.TYPE.JS_OBJECT -> getObject(key)
            JSValue.TYPE.JS_ARRAY -> getArray(key)
            else -> def
        }
    }
    return def
}

//---

fun JSObject.put(map: Map<String, Any?>): JSObject {
    map.forEach { (key, value) ->
        put(key, value)
    }
    return this
}

fun JSObject.put(key: String, value: Any?): JSObject {
    value ?: return this
    when (value) {
        is Int -> set(key, value)
        is Long -> set(key, value.toInt())
        is Double -> set(key, value)
        is Float -> set(key, value.toDouble())
        is Boolean -> set(key, value)
        is String -> set(key, value)
        is JSArray -> set(key, value)
        is JSObject -> set(key, value)
        is JSValue -> set(key, value)
        else -> set(key, value.toString())
    }
    return this
}

//---