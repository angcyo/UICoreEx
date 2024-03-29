package com.angcyo.quickjs.api.core

import android.os.Build
import android.webkit.JavascriptInterface
import androidx.annotation.Keep
import com.angcyo.core.component.model.DataShareModel
import com.angcyo.core.component.model.LanguageModel
import com.angcyo.core.vmApp
import com.angcyo.download.giteeVersionUpdate
import com.angcyo.laserpacker.device.DeviceHelper
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.component.RBackground
import com.angcyo.library.component.lastContext
import com.angcyo.library.ex.getAppSignatureMD5
import com.angcyo.library.ex.isAppDebug
import com.angcyo.library.ex.isDebug
import com.angcyo.library.ex.isDebugType
import com.angcyo.library.ex.killCurrentProcess
import com.angcyo.library.ex.openApp
import com.angcyo.library.ex.openUrl
import com.angcyo.library.ex.syncSingle
import com.angcyo.library.ex.toStr
import com.angcyo.library.getAppName
import com.angcyo.library.getAppString
import com.angcyo.library.getAppVersionCode
import com.angcyo.library.getAppVersionName
import com.angcyo.library.utils.Device
import com.angcyo.quickjs.EngineExecuteThread
import com.angcyo.quickjs.QuickJSEngine
import com.angcyo.quickjs.api.BaseJSInterface
import com.angcyo.quickjs.ui.ScriptRunTipDialogConfig
import com.quickjs.JSArray
import com.quickjs.JSFunction
import com.quickjs.JSObject

/**
 * 所有[JSFunction]调用, 必须在
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/06/11
 */
@Keep
class AppJsApi : BaseJSInterface() {

    //region ---base---

    override val interfaceName: String = "AppJs"

    /**注入默认的属性, 可以直接访问
     * ```
     * AppJs.androidId
     * ```
     * */
    @CallPoint
    fun init(jsObject: JSObject) {
        //build 信息
        jsObject.set("userName", getAppString("user_name"))
        jsObject.set("osName", getAppString("os_name"))
        jsObject.set("buildTime", getAppString("build_time"))
        jsObject.set("isDebug", isDebug())
        jsObject.set("isDebugType", isDebugType())
        jsObject.set("isAppDebug", isAppDebug())

        jsObject.set("sdkInt", Build.VERSION.SDK_INT)
        jsObject.set("androidId", Device.androidId)
        jsObject.set("packageName", lastContext.packageName)
        jsObject.set("appName", getAppName())
        jsObject.set("versionName", getAppVersionName())
        jsObject.set("versionCode", getAppVersionCode().toInt())
        jsObject.set("appSignatureMD5", lastContext.getAppSignatureMD5())
        //jsObject.set("Signature", lastContext.getAppSignatureSHA1())

        val locale = lastContext.resources.configuration.locale
        jsObject.set("timeZoneId", LanguageModel.timeZoneId)
        jsObject.set("language", locale.language)//zh
        jsObject.set("country", locale.country)//CN
        jsObject.set("displayName", locale.displayName)//中文 (简体中文,中国)

        //全部使用字符串
        vmApp<DataShareModel>().shareTextMapData.value?.forEach { entry ->
            jsObject.set(entry.key, entry.value.toStr())
        }
    }

    /** 延迟执行
     * [postDelay]*/
    @JavascriptInterface
    fun post(action: JSFunction) {
        val id = engineId
        EngineExecuteThread.get(id)?.let { engineThread ->
            engineThread.executeHandler?.post {
                try {
                    action.call(action, JSArray(action.context))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**延迟执行
     * [post]*/
    @JavascriptInterface
    fun postDelay(delay: Int, action: JSFunction) {
        val id = engineId
        EngineExecuteThread.get(id)?.let { engineThread ->
            engineThread.executeHandler?.postDelayed({
                try {
                    action.call(action, JSArray(action.context))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, delay.toLong())
        }
    }

    /**等待, 直到线程主动退出
     * ```
     * AppJs.waitForQuit();
     * ```
     * */
    @JavascriptInterface
    fun waitForQuit() {
        EngineExecuteThread.get(engineId)?.let { engineThread ->
            engineThread.waitForQuit.set(true)
        }
    }

    /**主动退出*/
    @JavascriptInterface
    fun quit() {
        EngineExecuteThread.get(engineId)?.let { engineThread ->
            engineThread.waitForQuit.set(false)
            engineThread.release()
        }
    }

    //endregion ---base---

    //region ---core---

    /**App是否被切到后台
     * ```
     * AppJs.isBackground()
     * ```
     * */
    @JavascriptInterface
    fun isBackground(): Boolean {
        return RBackground.isBackground()
    }

    /**
     * 打开指定的[url]网页
     * ```
     * AppJs.openUrl("https://www.baidu.com")
     * ```
     * */
    @JavascriptInterface
    fun openUrl(url: String?) = lastContext.openUrl(url)

    /**
     * 打开指定的[packageName]应用app
     * ```
     * AppJs.openApp("com.hingin.rn.hiprint")
     * ```
     * */
    @JavascriptInterface
    fun openApp(packageName: String?) = lastContext.openApp(packageName ?: lastContext.packageName)

    /**休眠当前线程*/
    @JavascriptInterface
    fun sleep(delay: Int) {
        com.angcyo.library.ex.sleep(delay.toLong())
    }

    /**杀掉当前进程*/
    @JavascriptInterface
    fun kill() {
        killCurrentProcess()
    }

    /**执行另一段脚本*/
    @JavascriptInterface
    fun executeScript(source: String, onEnd: JSFunction?) {
        syncSingle {
            var resultString: String? = null
            var errorString: String? = null
            QuickJSEngine.executeScript(source) { result, error ->
                if (error == null) {
                    resultString = result?.toStr()
                } else {
                    errorString = error.message
                }
                onEnd?.call(onEnd, JSArray(onEnd.context).apply {
                    push(resultString)
                    push(errorString)
                })
                it.countDown()
            }
        }
    }

    /**uuid*/
    @JavascriptInterface
    fun uuid() = com.angcyo.library.ex.uuid()

    /**隐藏脚本允运行提示对话框*/
    @JavascriptInterface
    fun hideScriptRunTipDialog() {
        ScriptRunTipDialogConfig.hideScriptRunTipDialog()
    }

    /**当前时间
     * ```
     * AppJs.nowTime().toString();
     * ```
     * 也可以使用js内置对象
     * ```
     * new Date().toString();
     * new Date().getTime(); //返回一个时间的格林威治时间数值。
     * new Date().getMilliseconds(); //返回一个指定的日期对象的毫秒数。
     *
     * new Date().getHours();
     * new Date().getMinutes();
     * new Date().getSeconds();
     * ``
     * https://developer.mozilla.org/zh-CN/docs/Web/JavaScript/Reference/Global_Objects/Date
     * */
    @JavascriptInterface
    fun nowTime() = System.currentTimeMillis()

    /**当前的时间*/
    @JavascriptInterface
    fun nowTimeString(pattern: String = "yyyy-MM-dd") = com.angcyo.library.ex.nowTimeString(pattern)

    //endregion ---core---

    //region ---app---

    /**检查版本更新*/
    @JavascriptInterface
    fun giteeVersionUpdate() {
        lastContext.giteeVersionUpdate()
    }

    //endregion ---app---

    //region ---可选api---

    /**分享雕刻日志*/
    @JavascriptInterface
    fun shareEngraveLog() {
        try {
            DeviceHelper.shareEngraveLog()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //endregion ---可选api---
}