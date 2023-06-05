package com.angcyo.quickjs

import android.webkit.JavascriptInterface
import com.angcyo.library.L
import com.angcyo.library.annotation.TestPoint
import com.angcyo.library.component.lastContext
import com.angcyo.library.ex.readAssets
import com.quickjs.JSFunction
import com.quickjs.JSObject
import com.quickjs.QuickJS


/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/06/05
 */
object QuickJSEngine {

    @TestPoint
    fun test() {
        val quickJS = QuickJS.createRuntime()
        val context = quickJS.createContext()
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
        /*val result = context.executeIntegerScript("var a = 2+10;\n a;", null)
        L.i(result)*/
        context.close()
        quickJS.close()
    }

}