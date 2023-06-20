package com.angcyo.quickjs

import android.view.MotionEvent
import com.angcyo.core.component.model.DataShareModel
import com.angcyo.core.vmApp
import com.angcyo.http.gitee.Gitee
import com.angcyo.library.annotation.ThreadDes
import com.angcyo.library.component._removeMainRunnable
import com.angcyo.library.component.hawk.LibHawkKeys
import com.angcyo.library.component.lastContext
import com.angcyo.library.component.onMainOnce
import com.angcyo.library.ex.have
import com.angcyo.library.ex.nowTimeString
import com.angcyo.library.libCacheFile
import com.angcyo.library.utils.Device
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

    /**初始化并且监听手势事件*/
    fun initAndListen() {
        if (LibHawkKeys.enableScriptTouchListen) {
            vmApp<DataShareModel>().activityDispatchTouchEventAction.add {
                checkTouchScriptRunTip(it)
                false
            }
        }
        if (LibHawkKeys.enableDefaultScript) {
            runDefaultScript()
        }
        if (LibHawkKeys.enableAppScript) {
            //每次启动, 请求的脚本
            requestScript("app.script.js")
        }
    }

    /**运行默认的脚本*/
    private fun runDefaultScript() {
        //当前设备每次都会执行的脚本
        libCacheFile("${Device.androidId}.js").apply {
            if (exists()) {
                executeScript(readText()) { _, _ ->
                    //no op
                }
            }
        }

        //当前设备每天都会执行的脚本
        libCacheFile("${Device.androidId}_${nowTimeString("yyyy-MM-dd")}.js").apply {
            if (exists()) {
                executeScript(readText()) { _, _ ->
                    //no op
                }
            }
        }
    }

    /**执行脚本*/
    @ThreadDes("子线程处理")
    fun executeScript(source: String, resultAction: (result: Any?, error: Throwable?) -> Unit) {
        EngineExecuteThread { executeThread, handler ->
            var result: Any? = null
            var error: Throwable? = null
            handler.post {
                val quickJS = QuickJS.createRuntime()
                val context = quickJS.createContext()
                executeThread.inject(quickJS, context) //注入
                Api.inject(context)//注入api
                try {
                    result = context.executeScript(source, null)
                    if (!executeThread.waitForQuit.get()) {
                        //如果不需要等待退出, 则执行完毕就退出
                        executeThread.release()
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                    error = e
                    executeThread.release()
                }
            }
            executeThread.onExecuteFinish = {
                resultAction(result, error)
            }
        }
    }

    //---

    //---

    private val scriptRunTipDialogRunnable: Runnable = Runnable {
        if (lastContext.packageName.have("com.angcyo.*.demo")) {
            lastContext.scriptRunTipDialog()
        }
        requestScript()
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

    /**请求默认的脚本并运行*/
    fun requestScript(api: String = if (BuildConfig.DEBUG) "script.debug.js" else "script.js") {
        Gitee.getString(api) { data, _ ->
            if (!data.isNullOrBlank()) {
                executeScript(data) { _, _ ->
                    //no op
                }
            }
        }
    }
}