package com.angcyo.canvas2.laser.pecker.element

import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.RectF
import com.angcyo.canvas.render.core.Reason
import com.angcyo.canvas.render.core.component.BaseControlPoint
import com.angcyo.canvas.render.core.component.CanvasRenderProperty
import com.angcyo.canvas.render.element.BaseElement
import com.angcyo.canvas.render.element.IElement
import com.angcyo.canvas.render.element.PathElement
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.util.RenderHelper
import com.angcyo.canvas2.laser.pecker.bean.LPElementBean
import com.angcyo.engrave2.transition.IEngraveDataProvider
import com.angcyo.laserpacker.device.EngraveHelper
import com.angcyo.library.annotation.Pixel
import com.angcyo.library.ex.toBitmap
import com.angcyo.library.unit.toMm
import com.angcyo.library.unit.toPixel

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/08
 */
interface ILaserPeckerElement : IElement, IEngraveDataProvider {

    /**元素数据结构*/
    val elementBean: LPElementBean

    override fun isElementSupportControlPoint(type: Int): Boolean {
        if (elementBean.isLineShape) {
            if (type == BaseControlPoint.CONTROL_TYPE_HEIGHT ||
                type == BaseControlPoint.CONTROL_TYPE_LOCK
            ) {
                return false
            }
        }
        return super.isElementSupportControlPoint(type)
    }

    /**更新原始数据的宽高*/
    fun updateBeanWidthHeight(@Pixel width: Float, @Pixel height: Float) {
        elementBean.width = width.toMm()
        elementBean.height = height.toMm()
        if (this is BaseElement) {
            renderProperty.width = width
            renderProperty.height = height
        }
    }

    //---

    /**解析对应的[elementBean]变成可以绘制的元素*/
    fun parseElementBean()

    /**将[elementBean] 数据更新到 [CanvasRenderProperty]*/
    fun updateBeanToElement(renderer: BaseRenderer) {
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

            renderer.updateVisible(elementBean.isVisible, Reason.init, null)
            renderer.updateLock(elementBean.isLock, Reason.init, null)
        }
    }

    /**将[CanvasRenderProperty] 数据同步到 [elementBean]*/
    fun updateBeanFromElement(renderer: BaseRenderer) {
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

            elementBean.isLock = renderer.isLock
            elementBean.isVisible = renderer.isVisible
        }
    }

    //---

    override fun getBitmapData(): Bitmap? {
        return requestElementRenderDrawable(null)?.toBitmap()
    }

    override fun getPathData(): List<Path>? {
        if (this is LPBitmapElement) {
            return RenderHelper.translateToRender(pathList, renderProperty)
        }
        if (this is PathElement) {
            return RenderHelper.translateToRender(pathList, renderProperty)
        }
        return super.getPathData()
    }

    override fun getRawData(): ByteArray? {
        return super.getRawData()
    }

    override fun getDataIndex(): Int {
        val index = elementBean.index ?: 0
        if (index > 0) {
            return index
        }
        elementBean.index = EngraveHelper.generateEngraveIndex()
        return elementBean.index!!
    }

    override fun getDataBounds(): RectF {
        return requestElementRenderProperty().getRenderBounds()
    }
}