package com.angcyo.canvas2.laser.pecker.element

import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.core.Reason
import com.angcyo.canvas.render.core.Strategy
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.state.PathStateStack
import com.angcyo.canvas2.laser.pecker.util.lpElementBean

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/16
 */
class LPPathStateStack(renderer: BaseRenderer) : PathStateStack(renderer) {

    var paintStyle: Int = 1

    override fun saveState(renderer: BaseRenderer) {
        super.saveState(renderer)
        paintStyle = renderer.lpElementBean()?.paintStyle ?: paintStyle
    }

    override fun restoreState(reason: Reason, strategy: Strategy, delegate: CanvasRenderDelegate?) {
        renderer.lpElementBean()?.paintStyle = paintStyle
        super.restoreState(reason, strategy, delegate)
    }

}