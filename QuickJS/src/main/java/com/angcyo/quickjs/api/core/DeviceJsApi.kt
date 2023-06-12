package com.angcyo.quickjs.api.core

import android.webkit.JavascriptInterface
import androidx.annotation.Keep
import com.angcyo.core.component.model.LanguageModel
import com.angcyo.library.utils.Device
import com.angcyo.quickjs.api.BaseJSInterface
import com.quickjs.JSObject

/**
 * 设备相关api
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2023/06/12
 */
@Keep
class DeviceJsApi : BaseJSInterface() {

    override val interfaceName: String = "device"

    override fun onInject(parent: JSObject) {
        jsObject?.set("androidId", Device.androidId)
    }

    @JavascriptInterface
    fun androidId() = Device.androidId

    @JavascriptInterface
    fun timeZoneId() = LanguageModel.timeZoneId

    /**zh_CN*/
    @JavascriptInterface
    fun getCurrentLanguage() = LanguageModel.getCurrentLanguage()

}