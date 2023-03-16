package com.angcyo.canvas2.laser.pecker.element

import android.graphics.Bitmap
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.core.Reason
import com.angcyo.canvas.render.core.Strategy
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.state.TextStateStack
import com.angcyo.canvas2.laser.pecker.element.LPTextElement

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/14
 */
class LPTextStateStack(renderer: BaseRenderer) : TextStateStack(renderer) {

    /**二维码编码格式, 编码格式（qrcode）, 编码格式（code128）*/
    var coding: String? = null

    /**纠错级别*/
    var eclevel: String? = null

    /**数据类型*/
    var type: Int = 0

    /**生成的缓存图片*/
    var codeBitmap: Bitmap? = null

    override fun saveState(renderer: BaseRenderer) {
        super.saveState(renderer)

        val element = textElement
        if (element is LPTextElement) {
            type = element.elementBean.mtype
            coding = element.elementBean.coding
            eclevel = element.elementBean.eclevel
            codeBitmap = element.codeBitmap
        }
    }

    override fun restoreState(reason: Reason, strategy: Strategy, delegate: CanvasRenderDelegate?) {
        val element = textElement
        if (element is LPTextElement) {
            element.elementBean.mtype = type
            element.elementBean.coding = coding
            element.elementBean.eclevel = eclevel
            element.codeBitmap = codeBitmap
        }
        super.restoreState(reason, strategy, delegate)
    }

}