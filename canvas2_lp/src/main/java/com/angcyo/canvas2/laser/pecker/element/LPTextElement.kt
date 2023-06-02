package com.angcyo.canvas2.laser.pecker.element

import android.graphics.Bitmap
import android.graphics.Canvas
import com.angcyo.canvas.render.data.RenderParams
import com.angcyo.canvas.render.element.TextElement
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.state.IStateStack
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.bean.LPElementBean
import com.angcyo.laserpacker.isBold
import com.angcyo.laserpacker.isItalic
import com.angcyo.laserpacker.toPaintStyle
import com.angcyo.laserpacker.toPaintStyleInt
import com.angcyo.library.unit.toMm
import com.angcyo.library.unit.toPixel
import com.angcyo.qrcode.createBarCode
import com.angcyo.qrcode.createQRCode
import com.google.zxing.BarcodeFormat

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/13
 */
class LPTextElement(override val elementBean: LPElementBean) : TextElement(), ILaserPeckerElement {

    /**条码/二维码图片*/
    var codeBitmap: Bitmap? = null

    init {
        updateBeanToElement(null)
    }

    override fun createStateStack(): IStateStack = LPTextStateStack()

    override fun onRenderInside(renderer: BaseRenderer?, canvas: Canvas, params: RenderParams) {
        if (elementBean.mtype == LPDataConstant.DATA_TYPE_TEXT) {
            super.onRenderInside(renderer, canvas, params)
        } else {
            if (codeBitmap == null) {
                parseElementBean()
            }
            codeBitmap?.also {
                renderBitmap(canvas, paint, it, params._renderMatrix)
            }
        }
    }

    override fun onUpdateElementAfter() {
        super.onUpdateElementAfter()
        updateBeanFromElement(null)
    }

    override fun updateBeanToElement(renderer: BaseRenderer?) {
        super.updateBeanToElement(renderer)
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

    override fun updateOriginText(text: String?, keepVisibleSize: Boolean) {
        if (elementBean.mtype == LPDataConstant.DATA_TYPE_TEXT) {
            super.updateOriginText(text, keepVisibleSize)
        } else {
            textProperty.text = text
            parseElementBean()

            codeBitmap?.let {
                updateOriginWidthHeight(
                    it.width.toFloat(),
                    it.height.toFloat(),
                    keepVisibleSize
                )
            }
        }
    }

    override fun parseElementBean() {
        val text = textProperty.text
        if (elementBean.mtype == LPDataConstant.DATA_TYPE_QRCODE) {
            elementBean.coding = "${BarcodeFormat.QR_CODE}".lowercase()
            codeBitmap = text?.createQRCode()
            updateOriginWidthHeight(codeBitmap)
        } else if (elementBean.mtype == LPDataConstant.DATA_TYPE_BARCODE) {
            elementBean.coding = "${BarcodeFormat.CODE_128}".lowercase()
            codeBitmap = text?.createBarCode()
            updateOriginWidthHeight(codeBitmap)
        }
    }

    private fun updateOriginWidthHeight(bitmap: Bitmap?) {
        bitmap ?: return
        elementBean.width = bitmap.width.toMm()
        elementBean.height = bitmap.height.toMm()
    }

    override fun updateOriginWidthHeight(
        newWidth: Float,
        newHeight: Float,
        keepVisibleSize: Boolean
    ) {
        super.updateOriginWidthHeight(newWidth, newHeight, keepVisibleSize)
        elementBean.width = renderProperty.width.toMm()
        elementBean.height = renderProperty.height.toMm()
    }
}