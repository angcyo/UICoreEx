package com.angcyo.canvas2.laser.pecker.util

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.core.Reason
import com.angcyo.canvas.render.core.Strategy
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.renderer.CanvasElementRenderer
import com.angcyo.canvas2.laser.pecker.bean.LPElementBean
import com.angcyo.canvas2.laser.pecker.element.LPBitmapElement
import com.angcyo.canvas2.laser.pecker.element.LPPathElement
import com.angcyo.canvas2.laser.pecker.element.LPTextElement
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.engrave.model.FscDeviceModel
import com.angcyo.library.annotation.MM
import com.angcyo.library.unit.toMm

/**
 * 元素助手
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/06
 */
object LPElementHelper {

    /**最小位置分配, 应该为设备最佳预览范围的左上角
     * [com.angcyo.engrave.model.FscDeviceModel.initDevice]*/
    @MM
    var _minLeft = 0f

    @MM
    var _minTop = 0f

    /**最后一次分配的坐标*/
    @MM
    var _lastLeft = 0f

    @MM
    var _lastTop = 0f

    //

    var _lastTopIndex = 0

    @MM
    const val POSITION_STEP = 5f

    //当位置增加到此值时, 进行换行

    /**[com.angcyo.engrave.EngraveProductLayoutHelper.bindCanvasView]*/
    @MM
    var POSITION_CUT_LEFT = 30f

    @MM
    var POSITION_CUT_TOP = 30f * 5

    /**分配一个位置, 和智能调整缩放*/
    fun assignLocation(bean: LPElementBean) {
        if (_lastLeft > POSITION_CUT_LEFT) {
            //换行
            _lastLeft = 0f
            _lastTopIndex++
            _lastTop = POSITION_STEP * _lastTopIndex
        }
        if (_lastTop > POSITION_CUT_TOP) {
            _lastTopIndex = 0
        }
        _lastLeft += POSITION_STEP
        _lastTop += POSITION_STEP

        val bounds = FscDeviceModel.productAssignLocationBounds
        if (bounds == null) {
            bean.left = _minLeft + _lastLeft
            bean.top = _minTop + _lastTop
        } else {
            bean.left = bounds.centerX().toMm() - bean._width / 2
            bean.top = bounds.centerY().toMm() - bean._height / 2
        }
    }

    /**当界面关闭后, 恢复分配的默认位置*/
    fun restoreLocation() {
        _lastLeft = 0f
        _lastTop = 0f
        _lastTopIndex = 0
    }

    /**当前元素, 是否进行了路径填充*/
    fun isPathFill(bean: LPElementBean?): Boolean {
        bean ?: return false
        return !bean.isLineShape &&
                bean.gcodeFillStep > 0
    }

    /**栅格化渲染器
     * [renderer] 需要被栅格化的渲染器*/
    fun rasterizeRenderer(renderer: BaseRenderer?, delegate: CanvasRenderDelegate?) {
        val bitmap = renderer?.requestRenderBitmap(null) ?: return
        val newRenderer = createBitmapRenderer(bitmap, delegate, false) {
            //保持位置不变
            renderer.renderProperty?.getRenderBounds()?.let { bounds ->
                left = bounds.left.toMm()
                top = bounds.top.toMm()
            }
        }

        delegate?.renderManager?.replaceElementRenderer(
            renderer,
            listOf(newRenderer),
            true,
            Reason.user,
            Strategy.normal
        )
    }

    /**创建一个图片渲染器*/
    private fun createBitmapRenderer(
        bitmap: Bitmap,
        delegate: CanvasRenderDelegate?,
        assignLocation: Boolean,
        init: LPElementBean.() -> Unit = {}
    ): CanvasElementRenderer {
        val elementBean = LPElementBean().apply {
            mtype = LPConstant.DATA_TYPE_BITMAP
            imageFilter = LPConstant.DATA_MODE_BLACK_WHITE //默认黑白处理
            blackThreshold = HawkEngraveKeys.lastBWThreshold
        }

        if (assignLocation) {
            assignLocation(elementBean)//分配位置
        }

        elementBean.init()//init

        val renderer = CanvasElementRenderer()
        renderer.renderElement = LPBitmapElement(elementBean).apply {
            updateBeanToElement(renderer)
            updateOriginBitmapSrc(delegate, renderer, bitmap)
        }
        return renderer
    }

    /**添加一个图片元素到画板
     * [LPConstant.DATA_TYPE_BITMAP]*/
    fun addBitmapElement(delegate: CanvasRenderDelegate?, bitmap: Bitmap?) {
        delegate ?: return
        bitmap ?: return
        val renderer = createBitmapRenderer(bitmap, delegate, true)
        delegate.renderManager.addElementRenderer(renderer, true, Reason.user, Strategy.normal)
        LPRendererHelper.generateName(delegate)
    }

    /**添加一个文本/二维码/条形码元素到画板
     * [LPConstant.DATA_TYPE_TEXT]
     * [LPConstant.DATA_TYPE_QRCODE]
     * [LPConstant.DATA_TYPE_BARCODE]
     * */
    fun addTextElement(
        delegate: CanvasRenderDelegate?,
        text: CharSequence?,
        type: Int = LPConstant.DATA_TYPE_TEXT
    ) {
        delegate ?: return
        val elementBean = LPElementBean().apply {
            mtype = type
            this.text = "$text"
            paintStyle = Paint.Style.FILL.toPaintStyleInt()
        }
        assignLocation(elementBean)

        val renderer = CanvasElementRenderer()
        renderer.renderElement = LPTextElement(elementBean).apply {
            updateBeanToElement(renderer)
        }
        delegate.renderManager.addElementRenderer(renderer, true, Reason.user, Strategy.normal)
        LPRendererHelper.generateName(delegate)
    }

    /**添加一个路径元素到画板
     * [LPConstant.DATA_TYPE_GCODE]
     * [LPConstant.DATA_TYPE_SVG]
     * [LPConstant.DATA_TYPE_PATH]
     *
     * [data] 原始数据
     * */
    fun addPathElement(
        delegate: CanvasRenderDelegate?,
        type: Int,
        data: String?,
        pathList: List<Path>?
    ) {
        delegate ?: return
        val elementBean = LPElementBean().apply {
            mtype = type
            this.data = data
            paintStyle = Paint.Style.STROKE.toPaintStyleInt()
        }
        assignLocation(elementBean)

        val renderer = CanvasElementRenderer()
        renderer.renderElement = LPPathElement(elementBean).apply {
            this.pathList = pathList
            updateBeanToElement(renderer)
        }
        delegate.renderManager.addElementRenderer(renderer, true, Reason.user, Strategy.normal)
        LPRendererHelper.generateName(delegate)
    }

}