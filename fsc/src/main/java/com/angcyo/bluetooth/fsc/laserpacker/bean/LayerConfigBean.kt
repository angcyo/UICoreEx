package com.angcyo.bluetooth.fsc.laserpacker.bean

import com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel
import com.angcyo.bluetooth.fsc.laserpacker.data.PxInfo
import com.angcyo.core.vmApp

/**
 * 每个图层单独的配置信息, 此配置优先于[com.angcyo.bluetooth.fsc.laserpacker.bean.DeviceConfigBean]
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/05/19
 */
data class LayerConfigBean(
    /**当前图层, 单独支持的分辨率*/
    var dpiList: List<PxInfo>? = null
) {
    fun filterDpiList(
        moduleState: Int = vmApp<DeviceStateModel>().deviceStateData.value?.moduleState ?: 0
    ): LayerConfigBean {
        return LayerConfigBean(dpiList?.filterModuleDpiList(moduleState))
    }
}

fun List<PxInfo>.filterModuleDpiList(
    moduleState: Int = vmApp<DeviceStateModel>().deviceStateData.value?.moduleState ?: 0
): List<PxInfo> {
    val result = mutableListOf<PxInfo>()
    for (pxInfo in this) {
        if (pxInfo.moduleList.isNullOrEmpty()) {
            result.add(pxInfo)
        } else {
            if (pxInfo.moduleList.contains(moduleState)) {
                result.add(pxInfo)
            }
        }
    }
    return result
}
