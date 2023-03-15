package com.angcyo.canvas2.laser.pecker.util

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.core.Reason
import com.angcyo.canvas.render.core.Strategy
import com.angcyo.canvas.render.renderer.CanvasElementRenderer
import com.angcyo.canvas2.laser.pecker.bean.LPElementBean
import com.angcyo.canvas2.laser.pecker.element.LPBitmapElement
import com.angcyo.canvas2.laser.pecker.element.LPTextElement
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.library.annotation.MM
import com.angcyo.library.annotation.Pixel
import com.angcyo.library.unit.toMm
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue

/**
 * 元素助手
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/06
 */
object LPElementHelper {

    /**如果配置了此属性, 则分配位置的时候, 会在此矩形的中心*/
    @Pixel
    var assignLocationBounds: RectF? = null

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

        val bounds = assignLocationBounds
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

    /**添加一个图片元素到画板
     * [LPConstant.DATA_TYPE_BITMAP]*/
    fun addBitmapElement(delegate: CanvasRenderDelegate?, bitmap: Bitmap?) {
        delegate ?: return
        bitmap ?: return
        val elementBean = LPElementBean().apply {
            mtype = LPConstant.DATA_TYPE_BITMAP
            imageFilter = LPConstant.DATA_MODE_BLACK_WHITE //默认黑白处理
            blackThreshold = HawkEngraveKeys.lastBWThreshold
        }
        assignLocation(elementBean)
        UMEvent.CANVAS_IMAGE.umengEventValue()

        val renderer = CanvasElementRenderer()
        renderer.renderElement = LPBitmapElement(elementBean).apply {
            updateBeanToElement(renderer)
            updateOriginBitmapSrc(delegate, renderer, bitmap)
        }
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
        UMEvent.CANVAS_TEXT.umengEventValue()

        val renderer = CanvasElementRenderer()
        renderer.renderElement = LPTextElement(elementBean).apply {
            updateBeanToElement(renderer)
        }
        delegate.renderManager.addElementRenderer(renderer, true, Reason.user, Strategy.normal)
        LPRendererHelper.generateName(delegate)
    }

}