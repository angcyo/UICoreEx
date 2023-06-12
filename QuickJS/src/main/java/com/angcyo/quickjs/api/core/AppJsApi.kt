package com.angcyo.quickjs.api.core

import android.webkit.JavascriptInterface
import androidx.annotation.Keep
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.component.RBackground
import com.angcyo.library.component.lastContext
import com.angcyo.library.ex.getAppSignatureMD5
import com.angcyo.library.ex.killCurrentProcess
import com.angcyo.library.ex.openApp
import com.angcyo.library.ex.openUrl
import com.angcyo.library.getAppName
import com.angcyo.library.getAppVersionCode
import com.angcyo.library.getAppVersionName
import com.angcyo.library.utils.Device
import com.angcyo.quickjs.api.IJSInterface
import com.quickjs.JSObject

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/06/11
 */
@Keep
class AppJsApi : IJSInterface {

    override val interfaceName: String = "AppJs"

    /**注入默认的属性, 可以直接访问
     * ```
     * AppJs.androidId
     * ```
     * */
    @CallPoint
    fun init(jsObject: JSObject) {
        jsObject.set("androidId", Device.androidId)
        jsObject.set("packageName", lastContext.packageName)
        jsObject.set("appName", getAppName())
        jsObject.set("versionName", getAppVersionName())
        jsObject.set("versionCode", getAppVersionCode().toInt())
        jsObject.set("appSignatureMD5", lastContext.getAppSignatureMD5())
        //jsObject.set("Signature", lastContext.getAppSignatureSHA1())
    }

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
    fun openApp(packageName: String?) = lastContext.openApp(packageName)

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

}