package com.angcyo.script

import com.angcyo.script.annotation.ScriptInject

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/12/22
 * Copyright (c) 2020 angcyo. All rights reserved.
 */

@ScriptInject
class Test {

    @ScriptInject
    @JvmField
    var a: Int? = null

    @ScriptInject
    var b: Int = 1

    @ScriptInject
    val b2: Long = 1

    @ScriptInject
    var dd: Double? = null

    @ScriptInject
    private var c: String? = null

    @ScriptInject
    var d: Test? = null

    @ScriptInject
    fun test(): String? {
        a = 100
        b = 100
        return "/ ${a} / ${b}"
    }

    @ScriptInject
    fun test1(a1: Double?, a2: String?): String? {
        dd = a1
        c = a2
        return "${a1} / ${a2} / ${a} / ${b}"
    }

    private fun test2(b1: Double?, a2: Test?): String? {
        return "${b1} // ${a2} // ${c} / ${d}"
    }
}

data class Test2(

    @ScriptInject
    var a: Int = 1,

    @ScriptInject
    var b: Int? = null,
)