package com.angcyo.canvas2.laser.pecker.element

import android.graphics.Bitmap
import android.graphics.Paint
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
import com.angcyo.engrave2.transition.IEngraveDataProvider
import com.angcyo.laserpacker.bean.LPElementBean
import com.angcyo.laserpacker.device.EngraveHelper
import com.angcyo.laserpacker.toPaintStyleInt
import com.angcyo.library.annotation.Pixel
import com.angcyo.library.ex.toBitmap
import com.angcyo.library.unit.toMm
import com.angcyo.library.unit.toPixel

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/08
 */
interface ILaserPeckerElement : IElement, IEngraveDataProvider {

    companion object {

        /**数据类型转换*/
        fun LPElementBean.toRenderProperty(result: CanvasRenderProperty = CanvasRenderProperty()): CanvasRenderProperty {
            result.anchorX = left.toPixel()
            result.anchorY = top.toPixel()
            result.width = width.toPixel()
            result.height = height.toPixel()

            result.angle = angle
            result.scaleX = scaleX ?: result.scaleX
            result.scaleY = scaleY ?: result.scaleY
            result.skewX = skewX ?: result.skewX
            result.skewY = skewY ?: result.skewY
            result.flipX = flipX ?: result.flipX
            result.flipY = flipY ?: result.flipY
            return result
        }

        /**数据类型转换*/
        fun CanvasRenderProperty.toElementBean(result: LPElementBean): LPElementBean {
            result.left = anchorX.toMm()
            result.top = anchorY.toMm()
            result.width = width.toMm()
            result.height = height.toMm()

            result.angle = angle
            result.scaleX = scaleX
            result.scaleY = scaleY
            result.skewX = skewX
            result.skewY = skewY
            result.flipX = flipX
            result.flipY = flipY
            return result
        }
    }

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
    fun updateBeanToElement(renderer: BaseRenderer?) {
        if (this is BaseElement) {
            elementBean.toRenderProperty(renderProperty)
            renderer?.updateVisible(elementBean.isVisible, Reason.init, null)
            renderer?.updateLock(elementBean.isLock, Reason.init, null)
        }
    }

    /**将[CanvasRenderProperty] 数据同步到 [elementBean]*/
    fun updateBeanFromElement(renderer: BaseRenderer?) {
        if (this is BaseElement) {
            renderProperty.toElementBean(elementBean)
            if (renderer != null) {
                elementBean.isLock = renderer.isLock
                elementBean.isVisible = renderer.isVisible
            }
        }
    }

    //---

    override fun getEngraveBitmapData(): Bitmap? {
        return requestElementDrawable(null, null)?.toBitmap()
    }

    override fun getEngravePathData(): List<Path>? {
        if (this is LPBitmapElement) {
            return RenderHelper.translateToRender(getDrawPathList(), renderProperty)
        }
        if (this is PathElement) {
            if (elementBean.isLineShape) {
                if (elementBean.paintStyle == Paint.Style.STROKE.toPaintStyleInt()) {
                    //描边的线, 用pixel生成GCode.
                    return null
                }
            } else if (elementBean.paintStyle != Paint.Style.STROKE.toPaintStyleInt()) {
                //填充的图形, 用pixel生成GCode.
                return null
            } else {
                //其他都用path转GCode
            }
            return RenderHelper.translateToRender(getDrawPathList(), renderProperty)
        }
        return super.getEngravePathData()
    }

    override fun getEngraveRawData(): ByteArray? {
        return super.getEngraveRawData()
    }

    override fun getEngraveDataIndex(): Int {
        val index = elementBean.index ?: 0
        if (index > 0) {
            return index
        }
        elementBean.index = EngraveHelper.generateEngraveIndex()
        return elementBean.index!!
    }

    override fun getEngraveDataBounds(): RectF {
        return requestElementRenderProperty().getRenderBounds()
    }

    override fun getEngraveDataName(): String? = elementBean.name
}