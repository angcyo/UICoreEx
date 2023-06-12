package com.angcyo.quickjs.api

import androidx.annotation.Keep
import com.quickjs.JSContext

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

}