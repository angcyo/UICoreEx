package com.angcyo.canvas.laser.pecker.dslitem

import com.angcyo.canvas.CanvasDelegate
import com.angcyo.canvas.items.renderer.BaseItemRenderer
import com.angcyo.doodle.ui.dslitem.DoodleIconItem

/**
 * 新版操作界面
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/15
 */
open class CanvasControlItem2 : DoodleIconItem() {

    var itemRenderer: BaseItemRenderer<*>? = null

    var itemCanvasDelegate: CanvasDelegate? = null

}