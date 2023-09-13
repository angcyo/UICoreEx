package com.angcyo.canvas2.laser.pecker.util

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.angcyo.bluetooth.fsc.laserpacker._deviceSettingBean
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.renderer.CanvasElementRenderer
import com.angcyo.canvas.render.renderer.CanvasGroupRenderer.Companion.getRendererGroupBounds
import com.angcyo.canvas.render.util.element
import com.angcyo.canvas2.laser.pecker.element.LPBitmapElement
import com.angcyo.canvas2.laser.pecker.element.LPPathElement
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.bean.LPElementBean
import com.angcyo.laserpacker.bean.LPVariableBean
import com.angcyo.laserpacker.device.model.FscDeviceModel
import com.angcyo.laserpacker.toBitmapElementBeanV2
import com.angcyo.laserpacker.toPaintStyleInt
import com.angcyo.library.annotation.MM
import com.angcyo.library.canvas.core.Reason
import com.angcyo.library.component.Strategy
import com.angcyo.library.ex.toStr
import com.angcyo.library.unit.toMm
import com.google.zxing.BarcodeFormat

/**
 * 元素助手
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/06
 */
object LPElementHelper {

    /**最小位置分配, 应该为设备最佳预览范围的左上角
     * [com.angcyo.laserpacker.device.model.FscDeviceModel.initDevice]*/
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
    var POSITION_CUT_TOP = POSITION_CUT_LEFT * 5

    private fun initLocation() {
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
    }

    /**分配一个位置, 和智能调整缩放*/
    fun assignLocation(bean: LPElementBean) {
        initLocation()

        val bounds = FscDeviceModel.productAssignLocationBounds
        if (bounds == null) {
            bean.left = _minLeft + _lastLeft
            bean.top = _minTop + _lastTop
        } else {
            bean.left = bounds.centerX().toMm() - (bean._width * bean._scaleX) / 2
            bean.top = bounds.centerY().toMm() - (bean._height * bean._scaleY) / 2
        }
    }

    /**分配位置*/
    fun assignLocation(rendererList: List<BaseRenderer>) {
        val groupBounds = rendererList.getRendererGroupBounds() ?: return
        initLocation()

        val bounds = FscDeviceModel.productAssignLocationBounds
        var tx = 0f
        var ty = 0f
        if (bounds == null) {
            tx = _minLeft + _lastLeft - groupBounds.left
            ty = _minTop + _lastTop - groupBounds.top
        } else {
            tx = bounds.centerX() - groupBounds.width() / 2
            ty = bounds.centerY() - groupBounds.height() / 2
        }

        for (renderer in rendererList) {
            renderer.translate(tx, ty, Reason.code, Strategy.preview, null)
        }
    }

    /**当界面关闭后, 恢复分配的默认位置*/
    fun restoreLocation() {
        _lastLeft = 0f
        _lastTop = 0f
        _lastTopIndex = 0
    }

    /**栅格化渲染器
     * [renderer] 需要被栅格化的渲染器*/
    fun rasterizeRenderer(
        renderer: BaseRenderer?,
        delegate: CanvasRenderDelegate?,
        backgroundColor: Int = Color.WHITE
    ) {
        val bitmap = renderer?.requestRenderBitmap(backgroundColor = backgroundColor) ?: return
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
        val elementBean = bitmap.toBitmapElementBeanV2()!!
        elementBean.init()//init
        return LPRendererHelper.parseElementRenderer(elementBean)!!.apply {
            val renderer = this
            element<LPBitmapElement>()?.apply {
                updateOriginBitmap(bitmap, true)
                if (assignLocation) {
                    assignLocation(elementBean)
                    updateBeanToElement(renderer)
                    updateRenderProperty()
                }
            }
        }
    }

    /**添加一个图片元素到画板
     * [LPDataConstant.DATA_TYPE_BITMAP]*/
    fun addBitmapElement(delegate: CanvasRenderDelegate?, bitmap: Bitmap?) {
        delegate ?: return
        bitmap ?: return
        val renderer = createBitmapRenderer(bitmap, delegate, true)
        delegate.renderManager.addElementRenderer(renderer, true, Reason.user, Strategy.normal)
        LPRendererHelper.generateName(delegate)
    }

    /**添加一个文本/二维码/条形码元素到画板
     * [LPDataConstant.DATA_TYPE_TEXT]
     * [LPDataConstant.DATA_TYPE_QRCODE]
     * [LPDataConstant.DATA_TYPE_BARCODE]
     * */
    fun addTextElement(
        delegate: CanvasRenderDelegate?,
        text: CharSequence?,
        type: Int = LPDataConstant.DATA_TYPE_TEXT
    ): CanvasElementRenderer? {
        val elementBean = LPElementBean().apply {
            mtype = type
            this.text = "$text"
            paintStyle = Paint.Style.FILL.toPaintStyleInt()
        }
        return LPRendererHelper.parseElementRenderer(elementBean, true)?.apply {
            delegate?.renderManager?.addElementRenderer(this, true, Reason.user, Strategy.normal)
            LPRendererHelper.generateName(delegate)
        }
    }

    /**添加一个变量文本元素*/
    fun addVariableTextElement(
        delegate: CanvasRenderDelegate?,
        variables: List<LPVariableBean>?,
        type: Int = LPDataConstant.DATA_TYPE_VARIABLE_TEXT,
        config: LPElementBean.() -> Unit = {}
    ): CanvasElementRenderer? {
        val elementBean = LPElementBean().apply {
            mtype = type
            this.variables = variables
            paintStyle = Paint.Style.FILL.toPaintStyleInt()

            if (type == LPDataConstant.DATA_TYPE_VARIABLE_BARCODE) {
                textShowStyle = LPDataConstant.TEXT_SHOW_STYLE_BOTTOM
                fontSize = _deviceSettingBean?.barcode1DTextSize ?: fontSize
                textAlign = _deviceSettingBean?.barcode1DTextAlign
            }

            if (type == LPDataConstant.DATA_TYPE_VARIABLE_BARCODE ||
                type == LPDataConstant.DATA_TYPE_VARIABLE_QRCODE
            ) {
                if (coding.isNullOrBlank()) {
                    if (type == LPDataConstant.DATA_TYPE_VARIABLE_QRCODE) {
                        coding = BarcodeFormat.QR_CODE.toStr()
                    } else {
                        coding = BarcodeFormat.CODE_128.toStr()
                    }
                }
            }

            config()
        }
        return LPRendererHelper.parseElementRenderer(elementBean, true)?.apply {
            delegate?.renderManager?.addElementRenderer(this, true, Reason.user, Strategy.normal)
            LPRendererHelper.generateName(delegate)
        }
    }

    /**添加一个路径元素到画板
     * [LPDataConstant.DATA_TYPE_GCODE]
     * [LPDataConstant.DATA_TYPE_SVG]
     * [LPDataConstant.DATA_TYPE_PATH]
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
        val renderer = LPRendererHelper.parseElementRenderer(elementBean, true)!!.apply {
            element<LPPathElement>()?.pathList = pathList
        }
        delegate.renderManager.addElementRenderer(renderer, true, Reason.user, Strategy.normal)
        LPRendererHelper.generateName(delegate)
    }

    /**添加一个形状元素到画板
     * [LPDataConstant.DATA_TYPE_LINE]
     * [LPDataConstant.DATA_TYPE_RECT]
     * [LPDataConstant.DATA_TYPE_OVAL]
     * [LPDataConstant.DATA_TYPE_POLYGON]
     * [LPDataConstant.DATA_TYPE_PENTAGRAM]
     * [LPDataConstant.DATA_TYPE_LOVE]
     * */
    fun addShapesElement(delegate: CanvasRenderDelegate?, type: Int) {
        delegate ?: return
        val elementBean = LPElementBean().apply {
            mtype = type
            width = LPPathElement.SHAPE_DEFAULT_WIDTH
            height = LPPathElement.SHAPE_DEFAULT_HEIGHT
            when (mtype) {
                LPDataConstant.DATA_TYPE_OVAL -> {
                    rx = width!! / 2
                    ry = height!! / 2
                }

                LPDataConstant.DATA_TYPE_PENTAGRAM -> {
                    side = 5
                }

                LPDataConstant.DATA_TYPE_POLYGON -> {
                    side = 3
                }
            }
            paintStyle = if (isLineShape) {
                Paint.Style.FILL.toPaintStyleInt()
            } else {
                Paint.Style.STROKE.toPaintStyleInt()
            }
        }
        val renderer = LPRendererHelper.parseElementRenderer(elementBean, true)!!
        delegate.renderManager.addElementRenderer(renderer, true, Reason.user, Strategy.normal)
        LPRendererHelper.generateName(delegate)
    }

    /**添加元素集合到画板*/
    fun addElementList(delegate: CanvasRenderDelegate?, beanList: List<LPElementBean>?) {
        delegate ?: return
        beanList ?: return
        val rendererList = LPRendererHelper.parseElementRendererList(beanList, true)
        delegate.renderManager.addElementRenderer(rendererList, true, Reason.user, Strategy.normal)
    }
}