package com.angcyo.server.def

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import com.angcyo.base.dslAHelper
import com.angcyo.core.dslitem.DslLastDeviceInfoItem
import com.angcyo.library.app
import com.angcyo.library.component.appBean
import com.angcyo.library.component.dslIntentQuery
import com.angcyo.library.component.lastContext
import com.angcyo.library.ex.decode
import com.angcyo.library.ex.urlIntent
import com.angcyo.server.bean.SchemeReqBean
import com.yanzhenjie.andserver.annotation.*

/**
 * 提供一些设备的基础信息
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/22
 */

@RestController
@CrossOrigin
class DeviceController {

    /**获取设备的基础信息*/
    @GetMapping("/device")
    fun device(): String {
        return DslLastDeviceInfoItem.deviceInfo {
            appendln()
            append(app().packageName)
            appendln()
        }.toString()
    }

    /**查看和打开对应的Scheme*/
    @PostMapping("/scheme")
    fun scheme(@RequestBody bean: SchemeReqBean): String {
        val scheme = bean.scheme
        if (scheme.isNullOrBlank()) {
            return "scheme 不正确"
        } else {
            dslIntentQuery {
                queryData = Uri.parse(scheme)
                queryCategory = listOf(Intent.CATEGORY_BROWSABLE)
            }.apply {
                if (isNotEmpty()) {
                    //找到了
                    val actionInfo = first().activityInfo
                    lastContext.dslAHelper {
                        val componentName =
                            ComponentName(actionInfo.packageName, actionInfo.name)
                        start(scheme.decode().urlIntent(componentName))
                    }
                    return "正在使用${actionInfo.packageName.appBean()?.appName}打开"
                } else {
                    return "未找到对应的应用"
                }
            }
        }
    }

}