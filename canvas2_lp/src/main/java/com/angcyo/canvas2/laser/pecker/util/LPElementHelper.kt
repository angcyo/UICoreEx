package com.angcyo.canvas2.laser.pecker.util

import android.graphics.Bitmap
import android.graphics.Paint
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.core.Strategy
import com.angcyo.canvas.render.renderer.CanvasElementRenderer
import com.angcyo.canvas2.laser.pecker.bean.LPElementBean
import com.angcyo.canvas2.laser.pecker.element.LPBitmapElement
import com.angcyo.canvas2.laser.pecker.element.LPTextElement
import com.angcyo.engrave.data.HawkEngraveKeys
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue

/**
 * 元素助手
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/06
 */
object LPElementHelper {

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
        UMEvent.CANVAS_IMAGE.umengEventValue()

        delegate.renderManager.addRenderer(CanvasElementRenderer().apply {
            val renderer = this
            renderElement = LPBitmapElement(elementBean).apply {
                updateOriginBitmap(delegate, renderer, bitmap)
            }
        }, true, Strategy.normal)
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
        UMEvent.CANVAS_TEXT.umengEventValue()

        delegate.renderManager.addRenderer(CanvasElementRenderer().apply {
            renderElement = LPTextElement(elementBean)
        }, true, Strategy.normal)
    }

}