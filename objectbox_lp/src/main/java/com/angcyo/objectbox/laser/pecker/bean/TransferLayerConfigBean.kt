package com.angcyo.objectbox.laser.pecker.bean

import com.angcyo.http.base.fromJson
import com.angcyo.http.base.listType
import com.angcyo.http.base.toJson

/**
 * 每个图层单独对应的传输配置结构
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/07/20
 */
data class TransferLayerConfigBean(

    /**
     * 图层id
     * [com.angcyo.laserpacker.device.LayerHelper]*/
    val layerId: String,

    /**对应的dpi*/
    var dpi: Float
)

fun String?.getLayerConfigList(): List<TransferLayerConfigBean>? {
    return this?.fromJson<List<TransferLayerConfigBean>>(listType(TransferLayerConfigBean::class))
}

fun String?.getLayerConfig(layerId: String?): TransferLayerConfigBean? {
    return getLayerConfigList()?.find { it.layerId == layerId }
}

/**[com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys.updateLayerDpi]*/
fun String?.updateLayerConfig(layerId: String, dpi: Float): String? {
    val result = mutableListOf<TransferLayerConfigBean>()
    getLayerConfigList()?.let {
        result.addAll(it)
    }
    val item = result.find { it.layerId == layerId } ?: TransferLayerConfigBean(layerId, dpi)
    item.dpi = dpi
    result.removeAll { it.layerId == layerId }
    result.add(item)
    return result.toJson()
}