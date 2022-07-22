package com.angcyo.server

import com.angcyo.core.dslitem.DslLastDeviceInfoItem
import com.angcyo.library.app
import com.yanzhenjie.andserver.annotation.GetMapping
import com.yanzhenjie.andserver.annotation.RestController

/**
 * 提供一些设备的基础信息
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/22
 */

@RestController
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

}