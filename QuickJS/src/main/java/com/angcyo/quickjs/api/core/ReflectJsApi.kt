package com.angcyo.quickjs.api.core

import android.webkit.JavascriptInterface
import androidx.annotation.Keep
import com.angcyo.library.utils.Reflect
import com.angcyo.quickjs.api.BaseJSInterface
import com.angcyo.quickjs.api.IJSInterface
import com.quickjs.JSArray

/**
 * 反射调用api
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/06/11
 */
@Keep
class ReflectJsApi : BaseJSInterface() {

    override val interfaceName: String = "reflect"

    //region ---set---

    @JavascriptInterface
    fun setStaticFieldBoolean(className: String, fieldName: String, value: Boolean?): Any? {
        return Reflect.setStaticField(className, fieldName, value)
    }

    @JavascriptInterface
    fun setStaticFieldInt(className: String, fieldName: String, value: Int?): Any? {
        return Reflect.setStaticField(className, fieldName, value)
    }

    /*@JavascriptInterface
    fun setStaticFieldLong(className: String, fieldName: String, value: Long?): Any? {
        //不支持Long类型
        return Reflect.setStaticField(className, fieldName, value)
    }

    @JavascriptInterface
    fun setStaticFieldFloat(className: String, fieldName: String, value: Float?): Any? {
        //不支持Float类型
        return Reflect.setStaticField(className, fieldName, value)
    }*/

    @JavascriptInterface
    fun setStaticFieldDouble(className: String, fieldName: String, value: Double?): Any? {
        return Reflect.setStaticField(className, fieldName, value)
    }

    /**
     * ```
     * AppJs.reflect.setStaticFieldString( "com.angcyo.quickjs.QuickJSEngine", "testStringValue", "new value" );
     * ```
     * */
    @JavascriptInterface
    fun setStaticFieldString(className: String, fieldName: String, value: String?): Any? {
        return Reflect.setStaticField(className, fieldName, value)
    }

    //endregion ---set---

    //region ---get/invoke---

    /**获取指定类的静态字段
     * ```
     * AppJs.reflect.getStaticField("android.os.Build$VERSION", "SDK_INT")
     * ```
     * */
    @JavascriptInterface
    fun getStaticField(className: String, fieldName: String): Any? {
        //android.os.Build.UNKNOWN
        //android.os.Build.VERSION.SDK_INT
        return Reflect.getStaticField(className, fieldName)
    }

    /**调用静态方法, 没有参数时, 也需要传null或者空数组
     * ```
     *  AppJs.reflect.invokeStaticMethod("android.os.Build", "isDebuggable", null)
     *  AppJs.reflect.invokeStaticMethod("android.os.Build", "isDebuggable", [])
     * ```
     * */
    @JavascriptInterface
    fun invokeStaticMethod(className: String, fieldName: String, array: JSArray?): Any? {
        //android.os.Build#isDebuggable
        val args = mutableListOf<Any?>()
        for (i in 0 until (array?.length() ?: 0)) {
            args.add(array?.get(i))
        }
        return Reflect.invokeStaticMethod(className, fieldName, *args.toTypedArray())
    }

    //endregion ---get/invoke---
}