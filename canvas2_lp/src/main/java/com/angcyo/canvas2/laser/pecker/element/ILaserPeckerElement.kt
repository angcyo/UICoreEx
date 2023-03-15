package com.angcyo.canvas2.laser.pecker.element

import com.angcyo.canvas.render.core.component.CanvasRenderProperty
import com.angcyo.canvas.render.element.BaseElement
import com.angcyo.canvas.render.element.IElement
import com.angcyo.canvas2.laser.pecker.bean.LPElementBean
import com.angcyo.library.annotation.Pixel
import com.angcyo.library.unit.toMm
import com.angcyo.library.unit.toPixel

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/08
 */
interface ILaserPeckerElement : IElement {

    /**元素数据结构*/
    val elementBean: LPElementBean

    /**更新原始数据的宽高*/
    fun updateBeanWidthHeight(@Pixel width: Float, @Pixel height: Float) {
        elementBean.width = width.toMm()
        elementBean.height = height.toMm()
        if (this is BaseElement) {
            renderProperty.width = width
            renderProperty.height = height
        }
    }

    /**将[elementBean] 数据更新到 [CanvasRenderProperty]*/
    fun updateBeanToElement() {
        if (this is BaseElement) {
            renderProperty.anchorX = elementBean.left.toPixel()
            renderProperty.anchorY = elementBean.top.toPixel()
            renderProperty.width = elementBean.width.toPixel()
            renderProperty.height = elementBean.height.toPixel()

            renderProperty.angle = elementBean.angle
            renderProperty.scaleX = elementBean.scaleX ?: renderProperty.scaleX
            renderProperty.scaleY = elementBean.scaleY ?: renderProperty.scaleY
            renderProperty.skewX = elementBean.skewX ?: renderProperty.skewX
            renderProperty.skewY = elementBean.skewY ?: renderProperty.skewY
            renderProperty.flipX = elementBean.flipX ?: renderProperty.flipX
            renderProperty.flipY = elementBean.flipY ?: renderProperty.flipY
        }
    }

    /**将[CanvasRenderProperty] 数据同步到 [elementBean]*/
    fun updateBeanFromElement() {
        if (this is BaseElement) {
            elementBean.left = renderProperty.anchorX.toMm()
            elementBean.top = renderProperty.anchorY.toMm()
            elementBean.width = renderProperty.width.toMm()
            elementBean.height = renderProperty.height.toMm()

            elementBean.angle = renderProperty.angle
            elementBean.scaleX = renderProperty.scaleX
            elementBean.scaleY = renderProperty.scaleY
            elementBean.skewX = renderProperty.skewX
            elementBean.skewY = renderProperty.skewY
            elementBean.flipX = renderProperty.flipX
            elementBean.flipY = renderProperty.flipY
        }
    }

}