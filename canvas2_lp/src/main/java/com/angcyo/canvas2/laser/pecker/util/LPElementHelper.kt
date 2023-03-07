package com.angcyo.canvas2.laser.pecker.util

import android.graphics.Bitmap
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.core.Strategy
import com.angcyo.canvas.render.renderer.CanvasElementRenderer
import com.angcyo.canvas2.laser.pecker.bean.LPElementBean
import com.angcyo.canvas2.laser.pecker.element.LPBitmapElement
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.library.L
import com.angcyo.library.LTime
import com.angcyo.library.ex.toBase64Data
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue

/**
 * 元素助手
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/06
 */
object LPElementHelper {

    /**添加一个图片元素到画板*/
    fun addBitmapElement(delegate: CanvasRenderDelegate?, bitmap: Bitmap?) {
        delegate ?: return
        bitmap ?: return
        val elementBean = LPElementBean().apply {
            mtype = LPConstant.DATA_TYPE_BITMAP
            imageFilter = LPConstant.DATA_MODE_BLACK_WHITE //默认黑白处理
            blackThreshold = HawkEngraveKeys.lastBWThreshold
            //src = bitmap.toBlackWhiteBitmap(HawkEngraveKeys.lastBWThreshold.toInt())
        }
        //bitmap.recycle()
        UMEvent.CANVAS_IMAGE.umengEventValue()

        delegate.renderManager.addRenderer(CanvasElementRenderer().apply {
            element = LPBitmapElement(elementBean).apply {
                updateBitmap(bitmap)
            }
            delegate.asyncManager.addAsyncTask(this@apply) {
                LTime.tick()
                elementBean.imageOriginal = bitmap.toBase64Data()
                L.i("base64耗时:${LTime.time()}")
            }
        }, true, Strategy.normal)
    }

}