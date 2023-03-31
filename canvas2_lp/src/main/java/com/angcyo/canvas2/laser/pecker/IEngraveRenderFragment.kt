package com.angcyo.canvas2.laser.pecker

import android.view.ViewGroup
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas2.laser.pecker.engrave.EngraveFlowLayoutHelper
import com.angcyo.fragment.AbsLifecycleFragment

/**
 * 雕刻/创作界面 接口
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/26
 */
interface IEngraveRenderFragment {

    /**界面*/
    val fragment: AbsLifecycleFragment

    /**创作界面*/
    val renderDelegate: CanvasRenderDelegate?

    /**雕刻流程布局*/
    val engraveFlowLayoutHelper: EngraveFlowLayoutHelper

    /**流程布局的容器*/
    val flowLayoutContainer: ViewGroup?

}