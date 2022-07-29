package com.angcyo.engrave.data

import android.graphics.RectF

/**
 * 预览Bounds的信息, 带旋转信息
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/29
 */
data class PreviewBoundsInfo(
    /**未旋转时的矩形*/
    val originRectF: RectF = RectF(),
    /**旋转信息, 如果有, 则优先使用4点预览*/
    var originRotate: Float? = null
)
