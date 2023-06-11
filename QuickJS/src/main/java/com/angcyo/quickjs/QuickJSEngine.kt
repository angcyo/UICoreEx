package com.angcyo.quickjs

import com.angcyo.http.rx.doBack
import com.angcyo.library.annotation.ThreadDes
import com.angcyo.quickjs.api.Api
import com.quickjs.QuickJS


/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/06/05
 */
object QuickJSEngine {

    /*@JvmStatic
    var testStringValue: String = "测试值"

    @JvmStatic
    var testIntValue: Int = 0

    @JvmStatic
    var testDoubleValue: Float = 0f

    @JvmStatic
    var testBooleanValue: Boolean = true

    @TestPoint
    fun test() {
        val quickJS = QuickJS.createRuntime()
        val context = quickJS.createContext()
        Api.inject(context)//注入api
        context.addJavascriptInterface(object {
            @JavascriptInterface
            fun showQQ(msg: String) {
                L.i(msg)
            }
        }, "T")
        lastContext.readAssets("test.js")?.let {
            val result = context.executeScript(it.trimIndent(), null)
            val keys = context.keys //不支持let/const声明的变量, 仅支持var
            var initPlugin = context.get("initPlugin")
            val plugin = context.get("plugin")
            //context.executeScript("plugin.initPlugin();", null)
            if (plugin is JSObject) {
                initPlugin = plugin.get("initPlugin")
                //val result = plugin.executeFunction("initPlugin", null)
                //L.i(result)
            }
            if (initPlugin is JSFunction) {
                val result = initPlugin.call(context, null)
                L.i(result)
            }
            L.i(result)
        }
        *//*val result = context.executeIntegerScript("var a = 2+10;\n a;", null)
        L.i(result)*//*
        context.close()
        quickJS.close()
    }*/

    /**执行脚本*/
    @ThreadDes("子线程处理")
    fun executeScript(source: String, resultAction: (result: Any?, error: Throwable?) -> Unit) {
        doBack {
            val quickJS = QuickJS.createRuntime()
            val context = quickJS.createContext()
            Api.inject(context)//注入api
            try {
                val result = context.executeScript(source, null)
                resultAction(result, null)
            } catch (e: Throwable) {
                e.printStackTrace()
                resultAction(null, e)
            } finally {
                context.close()
                quickJS.close()
            }
        }
    }

}