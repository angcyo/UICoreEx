package com.angcyo.quickjs.api

import androidx.annotation.Keep
import com.quickjs.JSContext
import com.quickjs.JSObject

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/06/11
 */

@Keep
interface IJSInterface {

    /**接口名字*/
    val interfaceName: String

    /**JS上下文*/
    var jsContext: JSContext?

    /**当前接口,对应的[JSObject]*/
    var jsObject: JSObject?

    /**当前接口被注入到父对象[parent]中*/
    fun onInject(parent: JSObject) {}

}