package com.angcyo.canvas2.laser.pecker.element

import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.drawable.Drawable
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.data.RenderParams
import com.angcyo.canvas.render.element.BitmapElement
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.state.IStateStack
import com.angcyo.canvas.render.util.CanvasRenderHelper
import com.angcyo.canvas2.laser.pecker.bean.LPBitmapStateStack
import com.angcyo.canvas2.laser.pecker.bean.LPElementBean
import com.angcyo.canvas2.laser.pecker.util.LPBitmapHandler
import com.angcyo.canvas2.laser.pecker.util.LPConstant
import com.angcyo.library.ex.toBase64Data

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/07
 */
class LPBitmapElement(override val elementBean: LPElementBean) : BitmapElement(),
    ILaserPeckerElement {

    /**图片转成的[Path]数据, 原始数据*/
    var pathList: List<Path>? = null

    init {
        updateBeanToBaseElement()
    }

    override fun createStateStack(renderer: BaseRenderer): IStateStack =
        LPBitmapStateStack(renderer)

    override fun requestElementRenderDrawable(renderParams: RenderParams?): Drawable? {
        if (elementBean.imageFilter == LPConstant.DATA_MODE_GCODE) {
            return createPathDrawable(
                pathList,
                paint,
                renderParams?.overrideWidth,
                renderParams?.overrideHeight
            )
        }
        return super.requestElementRenderDrawable(renderParams)
    }

    /**更新GCode数据*/
    fun updateOriginBitmap(pathList: List<Path>?, keepVisibleSize: Boolean = true) {
        this.pathList = pathList
        val bounds = CanvasRenderHelper.computePathBounds(pathList)
        updateOriginWidthHeight(bounds.width(), bounds.height(), keepVisibleSize)
    }

    /**更新原始图片, 并且自动处理成默认的黑白数据, 以及转成对应的base64数据*/
    fun updateOriginBitmap(
        delegate: CanvasRenderDelegate?,
        renderer: BaseRenderer,
        bitmap: Bitmap,
        keepVisibleSize: Boolean = true
    ) {
        updateOriginBitmap(bitmap, keepVisibleSize)
        renderBitmap = LPBitmapHandler.toBlackWhiteHandle(bitmap, elementBean)
        delegate?.asyncManager?.addAsyncTask(renderer) {
            elementBean.imageOriginal = bitmap.toBase64Data()
        }
    }
}