package com.angcyo.canvas2.laser.pecker.dslitem.item

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dialog.arithmeticHandleDialogConfig
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.canvas2.laser.pecker.util.lpElement
import com.angcyo.library.ex._string

/**
 * 操作item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/15
 */
class ControlOperateItem : CanvasIconItem() {

    init {
        itemIco = R.drawable.canvas_actions_ico
        itemText = _string(R.string.canvas_operate)
        itemEnable = true
        itemClick = {
            //2023-4-15 显示算法测试对话框
            val list = itemRenderDelegate?.selectorManager?.getSelectorRendererList(true, false)
            val renderer = list?.firstOrNull()
            val element = renderer?.lpElement()
            it.context.arithmeticHandleDialogConfig(element) {
                canvasRenderDelegate = itemRenderDelegate
            }
        }
    }
}