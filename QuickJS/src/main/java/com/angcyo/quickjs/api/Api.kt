package com.angcyo.quickjs.api

import com.angcyo.library.annotation.CallPoint
import com.angcyo.quickjs.api.core.AppJsApi
import com.angcyo.quickjs.api.core.ReflectJsApi
import com.angcyo.quickjs.api.core.TJsApi
import com.quickjs.JSContext
import com.quickjs.JSObject

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/06/11
 */
object Api {

    /**注入对象api到[com.quickjs.JSContext]*/
    @CallPoint
    fun inject(context: JSContext) {
        val appJsApi = context.injectInterface(AppJsApi())
        appJsApi.injectInterface(TJsApi())
        appJsApi.injectInterface(ReflectJsApi())
    }

    /**注入对象*/
    fun JSObject.injectInterface(jsInterface: IJSInterface): JSObject {
        return addJavascriptInterface(jsInterface, jsInterface.interfaceName)
    }

}