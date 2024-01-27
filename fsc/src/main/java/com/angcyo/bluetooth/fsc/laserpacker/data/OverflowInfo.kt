package com.angcyo.bluetooth.fsc.laserpacker.data

import android.graphics.Rect
import com.angcyo.library.ex.have
import com.angcyo.library.model.RectPointF

/**
 * 矩形是否溢出
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/10
 */
data class OverflowInfo(
    /**当前矩形, 调整过后的矩形*/
    var resultRect: Rect? = null,
    var resultRectPoint: RectPointF? = null,
    /**溢出类型*/
    var overflowType: Int = 0,
) {
    companion object {
        /**物理边界溢出*/
        const val OVERFLOW_TYPE_BOUNDS = 0x01

        /**有效边界溢出*/
        const val OVERFLOW_TYPE_LIMIT = OVERFLOW_TYPE_BOUNDS shl 1

        /**高度边界溢出*/
        const val OVERFLOW_TYPE_HEIGHT = OVERFLOW_TYPE_LIMIT shl 1

        /**宽度溢出*/
        const val OVERFLOW_TYPE_WIDTH = OVERFLOW_TYPE_HEIGHT shl 1

        /**有效高度边界溢出*/
        const val OVERFLOW_TYPE_HEIGHT_LIMIT = OVERFLOW_TYPE_WIDTH shl 1

        /**有效宽度溢出*/
        const val OVERFLOW_TYPE_WIDTH_LIMIT = OVERFLOW_TYPE_HEIGHT_LIMIT shl 1
    }
}

fun Int.isOverflowBounds() =
    have(OverflowInfo.OVERFLOW_TYPE_BOUNDS) ||
            have(OverflowInfo.OVERFLOW_TYPE_WIDTH) ||
            have(OverflowInfo.OVERFLOW_TYPE_HEIGHT)

fun Int.isOverflowLimit() =
    have(OverflowInfo.OVERFLOW_TYPE_LIMIT) ||
            have(OverflowInfo.OVERFLOW_TYPE_WIDTH_LIMIT) ||
            have(OverflowInfo.OVERFLOW_TYPE_HEIGHT_LIMIT)