package com.angcyo.canvas2.laser.pecker.element

import com.angcyo.canvas.render.element.TextElement
import com.angcyo.canvas2.laser.pecker.bean.LPElementBean
import com.angcyo.canvas2.laser.pecker.util.isBold
import com.angcyo.canvas2.laser.pecker.util.isItalic
import com.angcyo.canvas2.laser.pecker.util.toPaintStyle
import com.angcyo.library.unit.toPixel

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/13
 */
class LPTextElement(override val elementBean: LPElementBean) : TextElement(), ILaserPeckerElement {

    init {
        updateBeanToBaseElement()
        updateOriginText(elementBean.text)
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
}