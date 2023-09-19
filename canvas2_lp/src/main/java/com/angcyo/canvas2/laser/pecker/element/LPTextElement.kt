package com.angcyo.canvas2.laser.pecker.element

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout.LayoutParams
import androidx.core.view.updateLayoutParams
import com.angcyo.bluetooth.fsc.laserpacker._deviceSettingBean
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.core.component.BaseControlPoint
import com.angcyo.canvas.render.data.RenderParams
import com.angcyo.canvas.render.element.TextElement
import com.angcyo.canvas.render.element.rendererToBitmap
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.state.IStateStack
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.util.lpElementBean
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.bean.LPElementBean
import com.angcyo.laserpacker.bean.LPVariableBean
import com.angcyo.laserpacker.bean.initFileCacheIfNeed
import com.angcyo.laserpacker.isBold
import com.angcyo.laserpacker.isItalic
import com.angcyo.laserpacker.toPaintAlign
import com.angcyo.laserpacker.toPaintStyle
import com.angcyo.laserpacker.toPaintStyleInt
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.canvas.core.Reason
import com.angcyo.library.component.Strategy
import com.angcyo.library.component.SupportUndo
import com.angcyo.library.component.lastContext
import com.angcyo.library.ex._dpi
import com.angcyo.library.ex.find
import com.angcyo.library.ex.toColor
import com.angcyo.library.ex.toDpi
import com.angcyo.library.ex.toStr
import com.angcyo.library.unit.toMm
import com.angcyo.library.unit.toPixel
import com.angcyo.qrcode.code.AztecConfig
import com.angcyo.qrcode.code.BaseCodeConfig
import com.angcyo.qrcode.code.CodaBarConfig
import com.angcyo.qrcode.code.Code128Config
import com.angcyo.qrcode.code.Code39Config
import com.angcyo.qrcode.code.Code93Config
import com.angcyo.qrcode.code.DataMatrixConfig
import com.angcyo.qrcode.code.Ean13Config
import com.angcyo.qrcode.code.Ean8Config
import com.angcyo.qrcode.code.ITFConfig
import com.angcyo.qrcode.code.PDF417Config
import com.angcyo.qrcode.code.QrCodeConfig
import com.angcyo.qrcode.code.UPCAConfig
import com.angcyo.qrcode.code.UPCEConfig
import com.angcyo.qrcode.code.is1DCodeType
import com.angcyo.widget.base.saveView
import com.google.zxing.BarcodeFormat
import kotlin.math.roundToInt

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/13
 */
class LPTextElement(override val elementBean: LPElementBean) : TextElement(), ILaserPeckerElement {

    companion object {

        /**类型转换[BarcodeFormat]*/
        fun LPElementBean.toBarcodeFormat(): BarcodeFormat? {
            return try {
                BarcodeFormat.valueOf(coding ?: "")
            } catch (_: Exception) {
                null
            }
        }

        /**类型转换[BaseCodeConfig]*/
        fun LPElementBean.toBarcodeConfig(): BaseCodeConfig? {
            val format = toBarcodeFormat()
            return when (format) {
                BarcodeFormat.AZTEC -> AztecConfig(errorCorrection = errorLevel).apply {
                    initBaseConfig()
                    width = _deviceSettingBean?.barcodeSize?.toDpi() ?: width
                    height = width
                }

                BarcodeFormat.DATA_MATRIX -> DataMatrixConfig().apply {
                    initBaseConfig()
                    width = _deviceSettingBean?.barcodeSize?.toDpi() ?: width
                    height = width
                }

                BarcodeFormat.PDF_417 -> PDF417Config(
                    errorCorrection = errorLevel,
                    margin = _deviceSettingBean?.barcode2DMargin?.toDpi()
                ).apply {
                    initBaseConfig()
                    width = _deviceSettingBean?.barcodeSize?.toDpi() ?: width
                    height = width
                }

                BarcodeFormat.QR_CODE -> QrCodeConfig(
                    errorLevel = eclevel,
                    qrMaskPattern = qrMaskPattern,
                    margin = _deviceSettingBean?.barcode2DMargin?.toDpi()
                ).apply {
                    initBaseConfig()
                    width = _deviceSettingBean?.barcodeSize?.toDpi() ?: width
                    height = width
                }

                BarcodeFormat.CODE_128 -> Code128Config(_deviceSettingBean?.barcode1DMargin?.toDpi()).initBaseConfig()
                BarcodeFormat.CODE_39 -> Code39Config(_deviceSettingBean?.barcode1DMargin?.toDpi()).initBaseConfig()
                BarcodeFormat.CODE_93 -> Code93Config(_deviceSettingBean?.barcode1DMargin?.toDpi()).initBaseConfig()
                BarcodeFormat.CODABAR -> CodaBarConfig(_deviceSettingBean?.barcode1DMargin?.toDpi()).initBaseConfig()
                BarcodeFormat.EAN_13 -> Ean13Config(_deviceSettingBean?.barcode1DMargin?.toDpi()).initBaseConfig()
                BarcodeFormat.EAN_8 -> Ean8Config(_deviceSettingBean?.barcode1DMargin?.toDpi()).initBaseConfig()
                BarcodeFormat.ITF -> ITFConfig(_deviceSettingBean?.barcode1DMargin?.toDpi()).initBaseConfig()
                BarcodeFormat.UPC_A -> UPCAConfig(_deviceSettingBean?.barcode1DMargin?.toDpi()).initBaseConfig()
                BarcodeFormat.UPC_E -> UPCEConfig(_deviceSettingBean?.barcode1DMargin?.toDpi()).initBaseConfig()

                //BarcodeFormat.MAXICODE -> MaxiCodeConfig()
                //BarcodeFormat.RSS_14 -> RSS14Config()
                //BarcodeFormat.RSS_EXPANDED -> RSSExpandedConfig()
                //BarcodeFormat.UPC_EAN_EXTENSION -> UPC_EANExtensionConfig()
                else -> null
            }
        }

        /**默认配置*/
        fun BaseCodeConfig.initBaseConfig(): BaseCodeConfig {
            width = _deviceSettingBean?.barcodeWidth?.toDpi() ?: width
            height = _deviceSettingBean?.barcodeHeight?.toDpi() ?: height
            backgroundColor =
                _deviceSettingBean?.barcodeBackgroundColor?.toColor() ?: backgroundColor
            foregroundColor =
                _deviceSettingBean?.barcodeForegroundColor?.toColor() ?: foregroundColor
            return this
        }

        /**创建条形码/二维码图片*/
        fun LPElementBean.createBarcodeBitmap(content: String? = text): Bitmap? = try {
            val format = toBarcodeFormat()!! //直接报错返回
            val barcodeConfig = toBarcodeConfig()!! //直接报错返回

            var _1dCodeTextBitmap: Bitmap? = null
            if (format.is1DCodeType()) {
                //条形码宽度优化
                _1dCodeTextBitmap = copyTextProperty().renderTextBitmap()
                if (_1dCodeTextBitmap != null) {
                    val textWidth = _1dCodeTextBitmap.width + 4 * 24 * _dpi
                    barcodeConfig.width = maxOf(barcodeConfig.width, textWidth)
                }
            }
            barcodeConfig.encode(content)?.run {
                if (format.is1DCodeType() && isShowBarcodeText) {
                    //条形码
                    lastContext.saveView(R.layout.layout_text_show_style) {
                        _deviceSettingBean?.barcodeBackgroundColor?.toColor()?.let { color ->
                            it.setBackgroundColor(color)
                        }
                        val textImageView: ImageView? =
                            if (textShowStyle == LPDataConstant.TEXT_SHOW_STYLE_TOP) {
                                it.find(R.id.image1_view)
                            } else {
                                it.find(R.id.image2_view)
                            }
                        val codeImageView: ImageView? =
                            if (textShowStyle == LPDataConstant.TEXT_SHOW_STYLE_TOP) {
                                it.find(R.id.image2_view)
                            } else {
                                it.find(R.id.image1_view)
                            }
                        textImageView?.setImageBitmap(_1dCodeTextBitmap)
                        codeImageView?.setImageBitmap(this)

                        textImageView?.updateLayoutParams {
                            val layoutParams = this as? LayoutParams

                            //对齐样式
                            when (textAlign.toPaintAlign()) {
                                Paint.Align.RIGHT -> layoutParams?.gravity = Gravity.RIGHT
                                Paint.Align.CENTER -> layoutParams?.gravity = Gravity.CENTER
                                else -> layoutParams?.gravity = Gravity.LEFT
                            }

                            //间隙调整
                            val margin =
                                _deviceSettingBean?.barcode1DTextMargin?.toPixel()?.roundToInt()
                            if (textShowStyle == LPDataConstant.TEXT_SHOW_STYLE_TOP) {
                                layoutParams?.bottomMargin = margin ?: 0
                            } else {
                                layoutParams?.topMargin = margin ?: 0
                            }
                        }
                    }
                } else {
                    this
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**条码/二维码图片*/
    var codeBitmap: Bitmap? = null

    init {
        updateBeanToElement(null)
    }

    override fun createStateStack(): IStateStack = LPTextStateStack()

    override fun onRenderInside(renderer: BaseRenderer?, canvas: Canvas, params: RenderParams) {
        if (elementBean.isRenderTextElement) {
            super.onRenderInside(renderer, canvas, params)
        } else {
            if (codeBitmap == null) {
                parseElementBean()
            }
            codeBitmap?.also {
                renderBitmap(canvas, paint, it, params._renderMatrix)
            }
            if (codeBitmap == null && params.renderDst is CanvasRenderDelegate) {
                renderNoData(canvas, params)
            }
        }
    }

    override fun onUpdateElementAfter() {
        super.onUpdateElementAfter()
        updateBeanFromElement(null)
    }

    override fun updateBeanToElement(renderer: BaseRenderer?) {
        super.updateBeanToElement(renderer)
        if (elementBean.isVariableElement) {
            elementBean.updateVariableText()
        }
        textProperty.text = elementBean.text
        textProperty.fontFamily = elementBean.fontFamily
        textProperty.orientation = elementBean.orientation
        textProperty.charSpacing = elementBean.charSpacing.toPixel()
        textProperty.lineSpacing = elementBean.lineSpacing.toPixel()
        textProperty.fontSize = elementBean.fontSize.toPixel()
        textProperty.isCompactText = elementBean.isCompactText
        textProperty.textAlign = elementBean.textAlign
        textProperty.textColor = elementBean.textColor
        textProperty.curvature = elementBean.curvature

        textProperty.isUnderlineText = elementBean.underline
        textProperty.isStrikeThruText = elementBean.linethrough
        textProperty.isFakeBoldText = elementBean.isBold()
        textProperty.isItalic = elementBean.isItalic()
        textProperty.paintStyle = elementBean.paintStyle.toPaintStyle()

        updatePaint()
        updateOriginText(elementBean.text)
    }

    override fun updateBeanFromElement(renderer: BaseRenderer?) {
        super.updateBeanFromElement(renderer)
        elementBean.text = textProperty.text
        elementBean.fontFamily = textProperty.fontFamily
        elementBean.orientation = textProperty.orientation
        elementBean.charSpacing = textProperty.charSpacing.toMm()
        elementBean.lineSpacing = textProperty.lineSpacing.toMm()
        elementBean.fontSize = textProperty.fontSize.toMm()
        elementBean.isCompactText = textProperty.isCompactText
        elementBean.textAlign = textProperty.textAlign
        elementBean.textColor = textProperty.textColor
        elementBean.curvature = textProperty.curvature

        elementBean.underline = textProperty.isUnderlineText
        elementBean.linethrough = textProperty.isStrikeThruText
        elementBean.fontWeight = if (textProperty.isFakeBoldText) "bold" else null
        elementBean.fontStyle = if (textProperty.isItalic) "italic" else null
        elementBean.paintStyle = textProperty.paintStyle.toPaintStyleInt()
    }

    override fun onUpdateTextPropertyAfter(renderer: BaseRenderer?) {
        super.onUpdateTextPropertyAfter(renderer)
        if (elementBean.isVariableElement) {
            updateBeanFromElement(renderer)
        }
    }

    override fun updateOriginText(text: String?, keepVisibleSize: Boolean) {
        if (elementBean.isRenderTextElement) {
            super.updateOriginText(text, keepVisibleSize)
        } else {
            textProperty.text = text
            parseElementBean()

            codeBitmap?.let {
                updateRenderWidthHeight(
                    it.width.toFloat(),
                    it.height.toFloat(),
                    keepVisibleSize
                )
            }
        }
    }

    override fun updateCurvature(
        curvature: Float,
        renderer: BaseRenderer?,
        delegate: CanvasRenderDelegate?
    ) {
        super.updateCurvature(curvature, renderer, delegate)
        elementBean.curvature = curvature
    }

    override fun parseElementBean() {
        val text = textProperty.text
        if (elementBean.is2DCodeElement) {
            if (elementBean.coding == null) {
                elementBean.coding = BarcodeFormat.QR_CODE.toStr()
            }
            codeBitmap = elementBean.createBarcodeBitmap(text)
            updateOriginWidthHeight(codeBitmap)
        } else if (elementBean.is1DCodeElement) {
            if (elementBean.coding == null) {
                elementBean.coding = BarcodeFormat.CODE_128.toStr()
            }
            codeBitmap = elementBean.createBarcodeBitmap(text)
            updateOriginWidthHeight(codeBitmap)
        }
    }

    private fun updateOriginWidthHeight(bitmap: Bitmap?) {
        bitmap ?: return
        elementBean.width = bitmap.width.toMm()
        elementBean.height = bitmap.height.toMm()
    }

    override fun updateRenderWidthHeight(
        newWidth: Float,
        newHeight: Float,
        keepVisibleSize: Boolean
    ) {
        super.updateRenderWidthHeight(newWidth, newHeight, keepVisibleSize)
        elementBean.width = renderProperty.width.toMm()
        elementBean.height = renderProperty.height.toMm()
    }

    /**更新变量文本元素属性*/
    @SupportUndo
    fun updateVariableTextProperty(
        renderer: BaseRenderer?,
        delegate: CanvasRenderDelegate?,
        reason: Reason = Reason.user.apply {
            controlType = BaseControlPoint.CONTROL_TYPE_DATA
        },
        block: LPTextElement.() -> Unit
    ) {
        updateElementAction(renderer, delegate, reason) {
            this@LPTextElement.block()//do
            if (elementBean.isVariableElement) {
                elementBean.updateVariableText()
            }
            updateOriginText(elementBean.text)
        }
    }

    /**更新变量模板集合*/
    @SupportUndo
    fun updateVariables(
        list: List<LPVariableBean>?,
        renderer: BaseRenderer? = null,
        delegate: CanvasRenderDelegate? = null,
        reason: Reason = Reason.user.apply {
            controlType = BaseControlPoint.CONTROL_TYPE_DATA
        },
        strategy: Strategy = Strategy.normal
    ) {
        val newList = list
        val oldList = elementBean.variables
        delegate?.undoManager?.addAndRedo(strategy, true, {
            elementBean.variables = oldList
            oldList?.initFileCacheIfNeed(false)
            updateBeanToElement(renderer)
            renderer?.requestUpdatePropertyFlag(reason, delegate)
        }, {
            elementBean.variables = newList
            newList?.initFileCacheIfNeed(false)
            updateBeanToElement(renderer)
            renderer?.requestUpdatePropertyFlag(reason, delegate)
        })
    }

    /**雕刻完成之后, 更新元素内容*/
    @CallPoint
    fun updateElementAfterEngrave(
        renderer: BaseRenderer? = null,
        delegate: CanvasRenderDelegate? = null,
        reason: Reason = Reason.user.apply {
            controlType = BaseControlPoint.CONTROL_TYPE_DATA
        }
    ) {
        if (elementBean.isVariableElement) {
            elementBean.updateVariableTextAfterEngrave()
            updateOriginText(elementBean.text)

            //更新属性
            renderer?.requestUpdatePropertyFlag(reason, delegate)
        }
    }
}

/**将[LPElementBean]描述的元素, 使用[LPTextElement]渲染成[Bitmap]
 * 支持文本所有属性以及样式*/
fun LPElementBean.renderTextBitmap(): Bitmap? = LPTextElement(this).rendererToBitmap()

/**是否是条形码文本元素*/
fun List<BaseRenderer>?.haveBarcodeElement(): Boolean {
    this?.forEach {
        if (it.lpElementBean()?.is1DCodeElement == true || it.lpElementBean()?.is2DCodeElement == true) {
            return true
        }
    }
    return false
}

/**是否有变量文本元素*/
fun List<BaseRenderer>?.haveVariableElement(): Boolean {
    this?.forEach {
        if (it.lpElementBean()?.isVariableElement == true) {
            return true
        }
    }
    return false
}