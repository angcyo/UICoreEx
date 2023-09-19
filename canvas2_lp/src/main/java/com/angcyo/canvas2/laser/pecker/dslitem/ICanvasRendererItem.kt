package com.angcyo.canvas2.laser.pecker.dslitem

import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas2.laser.pecker.util.lpElementBean
import com.angcyo.laserpacker.bean.LPElementBean
import com.angcyo.library.unit.IRenderUnit

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/08
 */
interface ICanvasRendererItem {

    /**渲染器*/
    var itemRenderer: BaseRenderer?

    /**画板代理*/
    var itemRenderDelegate: CanvasRenderDelegate?

    val _elementBean: LPElementBean?
        get() = itemRenderer?.lpElementBean()

    val renderUnit: IRenderUnit?
        get() = itemRenderDelegate?.axisManager?.renderUnit
}