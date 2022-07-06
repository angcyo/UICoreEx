package com.angcyo.engrave

import com.angcyo.canvas.CanvasDelegate
import com.angcyo.iview.BaseRecyclerIView

/**
 * 雕刻相关布局助手
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/07
 */
abstract class BaseEngraveLayoutHelper : BaseRecyclerIView() {

    var canvasDelegate: CanvasDelegate? = null
}