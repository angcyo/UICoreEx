package com.angcyo.engrave.transition

import android.graphics.RectF
import com.angcyo.canvas.items.renderer.BaseItemRenderer

/**转换需要的一些额外参数
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/10
 */
data class TransitionParam(
    /**
     * 数据渲染开始的坐标位置, 用来实现线段数据合并时需要的偏移量
     * [com.angcyo.canvas.CanvasDelegate.getSelectedStartBounds]*/
    val startBounds: RectF? = null,

    /**合并数据时, GCode数据开始的渲染器, 此时的GCode数据需要
     * writeFirst 但是, 不需要 writeFinish*/
    val gCodeStartRenderer: BaseItemRenderer<*>? = null,

    /**合并数据时, GCode数据结束的渲染
     * 如果是则需要 writeFinish
     * */
    val gCodeEndRenderer: BaseItemRenderer<*>? = null,
)
