package com.angcyo.quickjs.api.core

import android.os.Build
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

    /**[com.angcyo.library.utils.Device.deviceInfoLess]*/
    override fun onInject(parent: JSObject) {
        jsObject?.set("androidId", Device.androidId)
        jsObject?.set("deviceName", Device.deviceName)
        //厂家 Google Pixel
        jsObject?.set("manufacturer", Build.MANUFACTURER)
        //型号 6
        jsObject?.set("model", Build.MODEL)
        //产品名称 oriole
        jsObject?.set("product", Build.PRODUCT)
    }

    @JavascriptInterface
    fun androidId() = Device.androidId

    @JavascriptInterface
    fun timeZoneId() = LanguageModel.timeZoneId

    /**zh_CN*/
    @JavascriptInterface
    fun getCurrentLanguage() = LanguageModel.getCurrentLanguage()

}