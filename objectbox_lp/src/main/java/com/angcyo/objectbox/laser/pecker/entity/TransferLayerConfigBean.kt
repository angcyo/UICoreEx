package com.angcyo.objectbox.laser.pecker.entity

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
    val dpi: Float
)
