package com.angcyo.engrave

import com.angcyo.canvas.CanvasDelegate
import com.angcyo.fragment.AbsFragment

/**
 * 雕刻/创作界面 接口
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/26
 */
interface IEngraveCanvasFragment {

    /**界面*/
    val fragment: AbsFragment

    /**创作界面*/
    val canvasDelegate: CanvasDelegate?

    /**雕刻流程布局*/
    val engraveFlowLayoutHelper: EngraveFlowLayoutHelper

}