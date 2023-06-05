package com.angcyo.v8

import com.angcyo.library.L
import com.angcyo.library.annotation.TestPoint
import com.angcyo.library.component.lastContext
import com.angcyo.library.ex.readAssets
import com.angcyo.library.toastQQ
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Function
import com.eclipsesource.v8.V8Object


/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/06/05
 */
object V8Engine {

    @TestPoint
    fun test() {
        val runtimes = V8.getActiveRuntimes() //1
        val undefined = V8.getUndefined()
        //val buildID = V8.getBuildID()
        //val v8Version = V8.getV8Version()
        /*runtime.registerResource()
        runtime.registerJavaMethod()
        runtime.registerJavaMethod()
        runtime.add()//注入对象, 仅支持基础数据类型*/

        val v8 = V8.createV8Runtime()
        val toast = V8Object(v8)
        toast.registerJavaMethod({ receiver, parameters ->
            println(parameters[0])
            toastQQ("${parameters[0]}")
        }, "showQQ")
        v8.add("T", toast)
        lastContext.readAssets("test.js")?.let {
            val result = v8.executeScript(it.trimIndent())
            val keys = v8.keys
            val initPlugin = v8.get("initPlugin")
            val plugin = v8.get("plugin")
            if (plugin is V8Object) {
                val result = plugin.executeJSFunction("initPlugin")
                L.i(result)
            }
            if (initPlugin is V8Function) {
                val result = initPlugin.call(v8, null)
                L.i(result)
            }
            L.i(result)
        }
        /*v8.executeVoidScript(
            """
          var person = {};
          var hockeyTeam = {name : 'WolfPack'};
          person.first = 'Ian';
          person['last'] = 'Bull';
          person.hockeyTeam = hockeyTeam;
          T.showQQ('from Js')
          """.trimIndent()
        )*/

        val person = v8.getObject("person")
        val hockeyTeam = person.getObject("hockeyTeam")
        println(hockeyTeam.getString("name"))
        person.close()
        hockeyTeam.close()

        v8.release(true)
    }

}