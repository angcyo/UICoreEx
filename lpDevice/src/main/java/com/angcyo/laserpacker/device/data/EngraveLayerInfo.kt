package com.angcyo.laserpacker.device.data

import com.angcyo.library.extend.IToText
import com.angcyo.library.extend.IToValue

/**
 * 雕刻图层信息
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/27
 */
data class EngraveLayerInfo(
    /**图层id, 图层的唯一标识符*/
    val layerId: String,
    /**界面显示的标签*/
    val label: CharSequence,
    /**分组状态保持
     * [com.angcyo.canvas2.laser.pecker.RenderLayoutHelper.renderLayerListLayout]*/
    var isGroupExtend: Boolean = false,
    /**当前图层,是否要显示dpi配置选择*/
    var showDpiConfig: Boolean = false,
) : IToText, IToValue {

    override fun toText(): CharSequence = label

    override fun toValue(): Any = layerId

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EngraveLayerInfo

        if (layerId != other.layerId) return false

        return true
    }

    override fun hashCode(): Int {
        return layerId.hashCode()
    }

}
