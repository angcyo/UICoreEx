package com.angcyo.script.core

import androidx.annotation.Keep
import com.angcyo.library.L
import com.angcyo.script.annotation.ScriptInject
import com.eclipsesource.v8.V8Object

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/12/23
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */

@Keep
@ScriptInject(key = "console")
class Console {

    @ScriptInject(includeReceiver = true)
    fun log(v8Object: V8Object, vararg arguments: Any?) {
        L.i(*arguments)
    }

}