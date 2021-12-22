package com.angcyo.script

import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Value


/**
 * Java JavaScript 互操作, Java 解析 js脚本并执行
 *
 * https://github.com/cashapp/zipline
 *
 * https://eclipsesource.com/blogs/tutorials/getting-started-with-j2v8/
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/12/22
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class DslScript {

//     var _engine: QuickJs? = null


    /**初始化引擎*/
    fun wrapEngine() {
        //val v8 = V8.createV8Runtime()

        /*val runtime = V8.createV8Runtime()


        runtime.add("", V8Value)
        runtime.registerJavaMethod()

        val result = runtime.executeIntegerScript(
            """
              var hello = 'hello, ';
              var world = 'world!';
              hello.concat(world).length;
              """.trimIndent()
        )

        //runtime.executeScript()

        runtime.getObject()//.release()

        println(result)

        runtime.release()*/
    }

}