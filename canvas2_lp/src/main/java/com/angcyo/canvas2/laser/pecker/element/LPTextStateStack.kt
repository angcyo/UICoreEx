package com.angcyo.canvas2.laser.pecker.element

import android.graphics.Bitmap
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.element.TextElement
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.state.TextStateStack
import com.angcyo.canvas.render.util.element
import com.angcyo.library.canvas.core.Reason
import com.angcyo.library.component.Strategy

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/14
 */
class LPTextStateStack : TextStateStack() {

    /**二维码编码格式, 编码格式（qrcode）, 编码格式（code128）*/
    var coding: String? = null

    /**纠错级别*/
    var eclevel: String? = null
    var textShowStyle: String? = null

    var qrMaskPattern: Int? = -1
    var errorLevel: Int? = -1

    /**数据类型*/
    var type: Int = 0

    /**生成的缓存图片*/
    var codeBitmap: Bitmap? = null

    override fun saveState(renderer: BaseRenderer, delegate: CanvasRenderDelegate?) {
        super.saveState(renderer, delegate)

        val element = renderer.element<TextElement>()
        if (element is LPTextElement) {
            type = element.elementBean.mtype
            coding = element.elementBean.coding
            eclevel = element.elementBean.eclevel
            codeBitmap = element.codeBitmap

            textShowStyle = element.elementBean.textShowStyle
            errorLevel = element.elementBean.errorLevel
            qrMaskPattern = element.elementBean.qrMaskPattern
        }
    }

    override fun restoreState(
        renderer: BaseRenderer,
        reason: Reason,
        strategy: Strategy,
        delegate: CanvasRenderDelegate?
    ) {
        val element = renderer.element<TextElement>()
        if (element is LPTextElement) {
            element.elementBean.mtype = type
            element.elementBean.coding = coding
            element.elementBean.eclevel = eclevel
            element.codeBitmap = codeBitmap
            element.elementBean.curvature = textProperty?.curvature ?: element.elementBean.curvature

            element.elementBean.textShowStyle = textShowStyle
            element.elementBean.errorLevel = errorLevel
            element.elementBean.qrMaskPattern = qrMaskPattern
        }
        super.restoreState(renderer, reason, strategy, delegate)
    }

}