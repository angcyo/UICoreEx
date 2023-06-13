package com.angcyo.quickjs

import android.view.MotionEvent
import com.angcyo.http.rx.doBack
import com.angcyo.library.annotation.ThreadDes
import com.angcyo.library.component._removeMainRunnable
import com.angcyo.library.component.hawk.LibHawkKeys
import com.angcyo.library.component.lastContext
import com.angcyo.library.component.onMainOnce
import com.angcyo.quickjs.api.Api
import com.angcyo.quickjs.ui.scriptRunTipDialog
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

    //---

    private val scriptRunTipDialogRunnable: Runnable = Runnable {
        lastContext.scriptRunTipDialog()
    }

    private var _lastTouchX = 0f
    private var _lastTouchY = 0f

    /**长按5秒后, 显示对话框*/
    fun checkTouchScriptRunTip(ev: MotionEvent) {
        val actionMasked = ev.actionMasked
        if (actionMasked == MotionEvent.ACTION_DOWN) {
            _lastTouchX = ev.x
            _lastTouchY = ev.y
            onMainOnce(LibHawkKeys.minCheckScriptTime, scriptRunTipDialogRunnable)
        } else if (actionMasked == MotionEvent.ACTION_MOVE) {
            val dx = ev.x - _lastTouchX
            val dy = ev.y - _lastTouchY

            if (dx > 10 || dy > 10) {
                _removeMainRunnable(scriptRunTipDialogRunnable)
            }
        } else if (actionMasked == MotionEvent.ACTION_UP || actionMasked == MotionEvent.ACTION_CANCEL) {
            _removeMainRunnable(scriptRunTipDialogRunnable)
        }
    }

}