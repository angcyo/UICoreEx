package com.angcyo.bluetooth.fsc.laserpacker.data

import android.graphics.Rect
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
    /**当前矩形是否超出了设备物理范围*/
    var isOverflowBounds: Boolean = false,
    /**当前矩形是否超出了推荐范围*/
    var isOverflowLimit: Boolean = false,
)
