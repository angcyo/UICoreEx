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
    var originBounds: RectF? = null,
    /**[originBounds]旋转后的矩形*/
    @Pixel
    var rotateBounds: RectF? = null,
    /**旋转信息, 如果有, 则优先使用4点预览*/
    var rotate: Float? = null,

    //---

    /**是否处于/需要4点预览状态*/
    var isFourPointPreview: Boolean = false,

    /**是否处于/需要中心点预览, 需要额外适配C1*/
    var isCenterPreview: Boolean = false,

    /**第三轴是否处于暂停状态
     * null 表示不处于第三轴预览状态*/
    var isZPause: Boolean? = null,

    /**是否开始了预览
     * [isZPause] 只有开始了预览, 才能直接发送z轴暂停/继续滚动指令*/
    var isStartPreview: Boolean = false
)