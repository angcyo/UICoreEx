package com.angcyo.canvas2.laser.pecker.element

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.angcyo.canvas.render.core.component.BaseControlPoint
import com.angcyo.canvas.render.core.component.CanvasRenderProperty
import com.angcyo.canvas.render.element.BaseElement
import com.angcyo.canvas.render.element.IElement
import com.angcyo.canvas.render.element.PathElement
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.util.RenderHelper
import com.angcyo.engrave2.transition.IEngraveDataProvider
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.bean.LPElementBean
import com.angcyo.laserpacker.device.EngraveHelper
import com.angcyo.laserpacker.toPaintStyleInt
import com.angcyo.library.annotation.MM
import com.angcyo.library.canvas.core.Reason
import com.angcyo.library.component.pool.acquireTempRectF
import com.angcyo.library.component.pool.release
import com.angcyo.library.ex.computePathBounds
import com.angcyo.library.ex.getTranslateX
import com.angcyo.library.ex.getTranslateY
import com.angcyo.library.ex.updateTranslate
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

    //---

    /**当[elementBean]属性改变后, 请调用此方法更新*/
    override fun updateElementFromBean(renderer: BaseRenderer?) {
        parseElementBean()
        updateBeanToElement(renderer)
        renderer?.updateRenderProperty()
    }

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
        return requestElementBitmap(null, null)
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

    override fun getEngraveDataBounds(bounds: RectF): RectF {
        return requestElementRenderProperty().getRenderBounds(bounds)
    }

    override fun getEngraveDataName(): String? = elementBean.name

    override fun getEngraveGCode(): String? {
        if (this is LPBitmapElement && elementBean.imageFilter == LPDataConstant.DATA_MODE_GCODE) {
            return elementBean.data
        }
        if (this is LPPathElement && !LPPathElement.isPathFill(elementBean)) {
            if (elementBean.mtype == LPDataConstant.DATA_TYPE_GCODE) {
                return elementBean.data
            }
        }
        return null
    }

    @MM
    override fun getEngraveGCodeMatrix(): Matrix {
        val renderMatrix = requestElementRenderProperty().getRenderMatrix()
        if (this is BaseElement) {
            val pathList = getDrawPathList()
            if (!pathList.isNullOrEmpty()) {
                val bounds = pathList.computePathBounds(acquireTempRectF())
                val dx = -bounds.left
                val dy = -bounds.top

                val matrix = Matrix()
                matrix.setTranslate(dx, dy)
                bounds.release()

                matrix.postConcat(renderMatrix)

                //强制使用mm单位
                matrix.updateTranslate(matrix.getTranslateX().toMm(), matrix.getTranslateY().toMm())

                return matrix
            }
        }
        return renderMatrix
    }
}