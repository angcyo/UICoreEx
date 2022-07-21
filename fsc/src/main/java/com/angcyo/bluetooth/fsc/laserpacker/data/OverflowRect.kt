package com.angcyo.bluetooth.fsc.laserpacker.data

import android.graphics.Rect

/**
 * 是否预览范围溢出
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/21
 */
data class OverflowRect(
    val rect: Rect,
    val isOverflow: Boolean = false,
)
