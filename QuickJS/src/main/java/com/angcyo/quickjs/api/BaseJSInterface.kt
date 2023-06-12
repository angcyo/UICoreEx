package com.angcyo.quickjs.api

import com.quickjs.JSContext

/**
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2023/06/12
 */
abstract class BaseJSInterface : IJSInterface {
    override var jsContext: JSContext? = null
}