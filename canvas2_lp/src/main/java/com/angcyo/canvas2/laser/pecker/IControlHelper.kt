package com.angcyo.canvas2.laser.pecker

import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.core.CanvasSelectorManager
import com.angcyo.engrave.EngraveFlowLayoutHelper
import com.angcyo.fragment.AbsLifecycleFragment
import com.angcyo.widget.DslViewHolder

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/25
 */
interface IControlHelper {

    val renderLayoutHelper: RenderLayoutHelper

    val canvasRenderDelegate: CanvasRenderDelegate?
        get() = renderLayoutHelper._rootViewHolder?.renderDelegate

    val selectorManager: CanvasSelectorManager?
        get() = canvasRenderDelegate?.selectorManager

    val fragment: AbsLifecycleFragment
        get() = renderLayoutHelper.canvasFragment.fragment

    val engraveFlowLayoutHelper: EngraveFlowLayoutHelper
        get() = renderLayoutHelper.canvasFragment.engraveFlowLayoutHelper

    val _rootViewHolder: DslViewHolder?
        get() = renderLayoutHelper._rootViewHolder

}