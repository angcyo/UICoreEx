package com.angcyo.canvas2.laser.pecker.element

import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.core.Reason
import com.angcyo.canvas.render.core.Strategy
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.state.PathStateStack
import com.angcyo.canvas2.laser.pecker.util.lpElementBean
import com.angcyo.library.annotation.MM

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/16
 */
class LPPathStateStack : PathStateStack() {

    var paintStyle: Int = 1
    var side: Int = 3
    var depth: Int = 40

    @MM
    var rx: Float = 0f

    @MM
    var ry: Float = 0f

    override fun saveState(renderer: BaseRenderer, delegate: CanvasRenderDelegate?) {
        super.saveState(renderer, delegate)
        renderer.lpElementBean()?.let { elementBean ->
            paintStyle = elementBean.paintStyle
            side = elementBean.side
            depth = elementBean.depth
            rx = elementBean.rx
            ry = elementBean.ry
        }
    }

    override fun restoreState(
        renderer: BaseRenderer,
        reason: Reason,
        strategy: Strategy,
        delegate: CanvasRenderDelegate?
    ) {
        renderer.lpElementBean()?.let { elementBean ->
            elementBean.paintStyle = paintStyle
            elementBean.side = side
            elementBean.depth = depth
            elementBean.rx = rx
            elementBean.ry = ry
        }
        super.restoreState(renderer, reason, strategy, delegate)
    }

}