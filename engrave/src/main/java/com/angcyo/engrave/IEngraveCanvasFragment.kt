package com.angcyo.engrave

import com.angcyo.fragment.AbsFragment

/**
 * 雕刻/创作界面 接口
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/26
 */
interface IEngraveCanvasFragment {

    /**界面*/
    val fragment: AbsFragment

    /**雕刻预览布局*/
    val engravePreviewLayoutHelper: EngravePreviewLayoutHelper

    /**雕刻布局*/
    val engraveLayoutHelper: EngraveLayoutHelper

}