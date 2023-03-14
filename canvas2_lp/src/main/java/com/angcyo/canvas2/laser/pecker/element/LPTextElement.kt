package com.angcyo.canvas2.laser.pecker.element

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.angcyo.canvas.render.data.RenderParams
import com.angcyo.canvas.render.element.TextElement
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.state.IStateStack
import com.angcyo.canvas2.laser.pecker.bean.LPElementBean
import com.angcyo.canvas2.laser.pecker.bean.LPTextStateStack
import com.angcyo.canvas2.laser.pecker.util.*
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
        updateBeanToBaseElement()
        updateOriginText(elementBean.text)
    }

    override fun createStateStack(renderer: BaseRenderer): IStateStack = LPTextStateStack(renderer)

    override fun requestElementRenderDrawable(renderParams: RenderParams?): Drawable? {
        return if (elementBean.mtype == LPConstant.DATA_TYPE_TEXT) {
            super.requestElementRenderDrawable(renderParams)
        } else {
            codeBitmap?.run {
                createBitmapDrawable(
                    this,
                    textPaint,
                    renderParams?.overrideWidth,
                    renderParams?.overrideWidth
                )
            }
        }
    }

    override fun updateBeanToBaseElement() {
        super.updateBeanToBaseElement()
        textProperty.text = elementBean.text
        textProperty.fontFamily = elementBean.fontFamily
        textProperty.orientation = elementBean.orientation
        textProperty.charSpacing = elementBean.charSpacing.toPixel()
        textProperty.lineSpacing = elementBean.lineSpacing.toPixel()
        textProperty.fontSize = elementBean.fontSize.toPixel()
        textProperty.isCompactText = elementBean.isCompactText
        textProperty.textAlign = elementBean.textAlign
        textProperty.textColor = elementBean.textColor

        textProperty.isUnderlineText = elementBean.underline
        textProperty.isStrikeThruText = elementBean.linethrough
        textProperty.isFakeBoldText = elementBean.isBold()
        textProperty.isItalic = elementBean.isItalic()
        textProperty.paintStyle = elementBean.paintStyle.toPaintStyle()

        updatePaint()
    }

    override fun updateBeanFromBaseElement() {
        super.updateBeanFromBaseElement()
        elementBean.text = textProperty.text
        elementBean.fontFamily = textProperty.fontFamily
        elementBean.orientation = textProperty.orientation
        elementBean.charSpacing = textProperty.charSpacing.toMm()
        elementBean.lineSpacing = textProperty.lineSpacing.toMm()
        elementBean.fontSize = textProperty.fontSize.toMm()
        elementBean.isCompactText = textProperty.isCompactText
        elementBean.textAlign = textProperty.textAlign
        elementBean.textColor = textProperty.textColor

        elementBean.underline = textProperty.isUnderlineText
        elementBean.linethrough = textProperty.isStrikeThruText
        elementBean.fontWeight = if (textProperty.isFakeBoldText) "bold" else null
        elementBean.fontStyle = if (textProperty.isItalic) "italic" else null
        elementBean.paintStyle = textProperty.paintStyle.toPaintStyleInt()
    }

    override fun updateOriginText(text: String?, keepVisibleSize: Boolean) {
        if (elementBean.mtype == LPConstant.DATA_TYPE_TEXT) {
            super.updateOriginText(text, keepVisibleSize)
        } else {
            textProperty.text = text
            if (elementBean.mtype == LPConstant.DATA_TYPE_QRCODE) {
                elementBean.coding = "${BarcodeFormat.QR_CODE}".lowercase()
                text?.createQRCode()?.let {
                    codeBitmap = it
                    updateOriginWidthHeight(
                        it.width.toFloat(),
                        it.height.toFloat(),
                        keepVisibleSize
                    )
                }
            } else if (elementBean.mtype == LPConstant.DATA_TYPE_BARCODE) {
                elementBean.coding = "${BarcodeFormat.CODE_128}".lowercase()
                text?.createBarCode()?.let {
                    codeBitmap = it
                    updateOriginWidthHeight(
                        it.width.toFloat(),
                        it.height.toFloat(),
                        keepVisibleSize
                    )
                }
            }
        }
    }
}