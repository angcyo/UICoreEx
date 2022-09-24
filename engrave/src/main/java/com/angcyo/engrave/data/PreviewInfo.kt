package com.angcyo.engrave.data

import android.graphics.RectF
import com.angcyo.library.annotation.Pixel

/**
 * 预览的一些信息
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/24
 */
data class PreviewInfo(
    /**未旋转时的矩形*/
    @Pixel
    val originBounds: RectF = RectF(),
    /**[originBounds]旋转后的矩形*/
    @Pixel
    val rotateBounds: RectF = RectF(),
    /**旋转信息, 如果有, 则优先使用4点预览*/
    var rotate: Float? = null,
    /**正在预览的item*/
    var itemUuid: String? = null,
)