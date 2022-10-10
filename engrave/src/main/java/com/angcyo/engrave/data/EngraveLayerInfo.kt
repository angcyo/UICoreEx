package com.angcyo.engrave.data

import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.library.extend.IToText
import com.angcyo.library.extend.IToValue

/**
 * 雕刻图层信息
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/27
 */
data class EngraveLayerInfo(
    /**
     * 数据模式
     * [CanvasConstant.DATA_MODE_DITHERING] 抖动数据格式
     * [CanvasConstant.DATA_MODE_GCODE] gcode数据格式
     * [CanvasConstant.DATA_MODE_BLACK_WHITE] 线段数据格式
     * */
    val mode: Int,
    /**界面显示的标签*/
    val label: CharSequence
) : IToText, IToValue {

    override fun toText(): CharSequence = label

    override fun toValue(): Any = mode

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EngraveLayerInfo

        if (mode != other.mode) return false

        return true
    }

    override fun hashCode(): Int {
        return mode
    }

}
