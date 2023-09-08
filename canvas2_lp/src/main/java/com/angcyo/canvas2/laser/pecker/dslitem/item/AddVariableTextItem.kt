package com.angcyo.canvas2.laser.pecker.dslitem.item

import com.angcyo.canvas2.laser.pecker.dialog.variableTextDialog
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.canvas2.laser.pecker.util.LPElementHelper
import com.angcyo.laserpacker.LPDataConstant
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue

/**
 * 添加变量文本item
 *
 * [AddTextItem]
 * [AddVariableTextItem]
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/08/30
 */
class AddVariableTextItem : CanvasIconItem() {

    init {
        itemClick = {
            it.context.variableTextDialog {
                onApplyVariableListAction = {
                    LPElementHelper.addVariableTextElement(
                        itemRenderDelegate,
                        it,
                        LPDataConstant.DATA_TYPE_VARIABLE_TEXT
                    )
                    UMEvent.CANVAS_VARIABLE_TEXT.umengEventValue()
                }
            }
        }
    }

}