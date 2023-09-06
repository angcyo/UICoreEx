package com.angcyo.canvas2.laser.pecker.dslitem.item

import com.angcyo.canvas2.laser.pecker.dialog.variableTextDialog
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue

/**
 * 添加变量文本item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/08/30
 */
class AddVariableTextItem : CanvasIconItem() {

    init {
        itemClick = {
            UMEvent.CANVAS_VARIABLE_TEXT.umengEventValue()
            it.context.variableTextDialog {

            }
        }
    }

}