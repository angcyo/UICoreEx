package com.angcyo.canvas2.laser.pecker.bean

import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.core.Reason
import com.angcyo.canvas.render.core.Strategy
import com.angcyo.canvas.render.data.IStateStack
import com.angcyo.canvas.render.data.PropertyStateStack
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas2.laser.pecker.element.LPBitmapElement
import com.angcyo.canvas2.laser.pecker.util.lpBitmapElement
import com.angcyo.canvas2.laser.pecker.util.lpElementBean

/**
 * 图片状态存储
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/09
 */
class BitmapStateStack(val renderer: BaseRenderer) : PropertyStateStack(), IStateStack {

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
    val contrast = elementBean.contrast
    val brightness = elementBean.brightness
    val blackThreshold = elementBean.blackThreshold
    val printsThreshold = elementBean.printsThreshold

    val data = elementBean.data
    val gcodeDirection = elementBean.gcodeDirection
    val gcodeLineSpace = elementBean.gcodeLineSpace
    val gcodeAngle = elementBean.gcodeAngle
    val gcodeOutline = elementBean.gcodeOutline
    val gcodeFillStep = elementBean.gcodeFillStep
    val gcodeFillAngle = elementBean.gcodeFillAngle

    val minDiameter = elementBean.minDiameter
    val meshShape = elementBean.meshShape
    val maxDiameter = elementBean.maxDiameter
    val isMesh = elementBean.isMesh

    init {
        saveState(renderer)
    }

    //endregion---需要存储的数据---

    override fun restoreState(reason: Reason, strategy: Strategy, delegate: CanvasRenderDelegate?) {
        element?.originBitmap = operateBitmap
        element?.renderBitmap = renderBitmap
        element?.pathList = pathList

        elementBean.imageOriginal = imageOriginal
        elementBean.src = src

        elementBean.imageFilter = imageFilter
        elementBean.inverse = inverse
        elementBean.contrast = contrast
        elementBean.brightness = brightness
        elementBean.blackThreshold = blackThreshold
        elementBean.printsThreshold = printsThreshold

        elementBean.data = data
        elementBean.gcodeDirection = gcodeDirection
        elementBean.gcodeLineSpace = gcodeLineSpace
        elementBean.gcodeAngle = gcodeAngle
        elementBean.gcodeOutline = gcodeOutline
        elementBean.gcodeFillStep = gcodeFillStep
        elementBean.gcodeFillAngle = gcodeFillAngle

        elementBean.minDiameter = minDiameter
        elementBean.maxDiameter = maxDiameter
        elementBean.meshShape = meshShape
        elementBean.isMesh = isMesh

        super.restoreState(reason, strategy, delegate)
        renderer.requestUpdateDrawable(reason, delegate)
    }
}