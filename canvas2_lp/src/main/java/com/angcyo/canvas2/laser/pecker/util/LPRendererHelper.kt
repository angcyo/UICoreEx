package com.angcyo.canvas2.laser.pecker.util

import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.util.renderElement
import com.angcyo.canvas2.laser.pecker.bean.LPElementBean
import com.angcyo.canvas2.laser.pecker.element.ILaserPeckerElement
import com.angcyo.canvas2.laser.pecker.element.LPBitmapElement

/**
 * LP渲染器操作助手
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/08
 */
object LPRendererHelper {
}

//region---LpRenderer---

/**[ILaserPeckerElement]*/
fun BaseRenderer.lpElement(): ILaserPeckerElement? {
    val element = renderElement
    if (element is ILaserPeckerElement) {
        return element
    }
    return null
}

/**[LPElementBean]*/
fun BaseRenderer.lpElementBean(): LPElementBean? = lpElement()?.elementBean

/**[LPBitmapElement]*/
fun BaseRenderer.lpBitmapElement(): LPBitmapElement? {
    val element = lpElement()
    if (element is LPBitmapElement) {
        return element
    }
    return null
}

//endregion---LpRenderer---

//region---LPElementBean---

/**是否加粗*/
fun LPElementBean.isBold() = fontWeight == "bold"

/**是否斜体*/
fun LPElementBean.isItalic() = fontStyle == "italic"

//endregion---LPElementBean---