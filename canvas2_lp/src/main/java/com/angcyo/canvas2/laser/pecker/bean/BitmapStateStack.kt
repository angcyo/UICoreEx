package com.angcyo.canvas2.laser.pecker.bean

import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.core.Reason
import com.angcyo.canvas.render.core.Strategy
import com.angcyo.canvas.render.data.IStateStack
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas2.laser.pecker.element.LPBitmapElement
import com.angcyo.canvas2.laser.pecker.util.lpBitmapElement
import com.angcyo.canvas2.laser.pecker.util.lpElementBean

/**
 * 图片状态存储
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/09
 */
class BitmapStateStack(val renderer: BaseRenderer) : IStateStack {

    private val element: LPBitmapElement?
        get() = renderer.lpBitmapElement()

    private val elementBean: LPElementBean
        get() = renderer.lpElementBean()!!

    //region---需要存储的数据---

    val operateBitmap = element?.originBitmap
    val renderBitmap = element?.renderBitmap
    val pathList = element?.pathList

    val imageOriginal = elementBean.imageOriginal
    val src = elementBean.src

    val imageFilter = elementBean.imageFilter
    val inverse = elementBean.inverse
    val blackThreshold = elementBean.blackThreshold

    //endregion---需要存储的数据---

    override fun restoreState(reason: Reason, strategy: Strategy, delegate: CanvasRenderDelegate?) {
        element?.originBitmap = operateBitmap
        element?.renderBitmap = renderBitmap
        element?.pathList = pathList

        elementBean.imageOriginal = imageOriginal
        elementBean.src = src

        elementBean.imageFilter = imageFilter
        elementBean.inverse = inverse
        elementBean.blackThreshold = blackThreshold

        renderer.requestUpdateDrawable(reason, delegate)
        renderer.requestUpdateProperty(reason, delegate)
    }
}