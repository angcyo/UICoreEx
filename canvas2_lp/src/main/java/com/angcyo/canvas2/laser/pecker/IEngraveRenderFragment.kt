package com.angcyo.canvas2.laser.pecker

import android.view.ViewGroup
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas2.laser.pecker.engrave.BaseFlowLayoutHelper
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
    val flowLayoutHelper: BaseFlowLayoutHelper

    /**流程布局的容器*/
    val flowLayoutContainer: ViewGroup?

    /**警示动画布局的容器*/
    val dangerLayoutContainer: ViewGroup?
}