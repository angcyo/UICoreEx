package com.angcyo.canvas2.laser.pecker.element

import android.graphics.Path
import android.graphics.drawable.Drawable
import com.angcyo.canvas.render.data.RenderParams
import com.angcyo.canvas.render.element.BitmapElement
import com.angcyo.canvas2.laser.pecker.bean.LPElementBean

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/07
 */
class LPBitmapElement(override val elementBean: LPElementBean) : BitmapElement(),
    ILaserPeckerElement {

    /**图片转成的[Path]数据, 原始数据*/
    var pathList: List<Path>? = null

    override fun requestElementRenderDrawable(renderParams: RenderParams?): Drawable? {
        return super.requestElementRenderDrawable(renderParams)
    }

}