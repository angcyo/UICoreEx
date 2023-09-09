package com.angcyo.canvas2.laser.pecker.dslitem.item

import com.angcyo.canvas2.laser.pecker.dialog.variableTextDialog
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.canvas2.laser.pecker.util.LPElementHelper
import com.angcyo.laserpacker.LPDataConstant
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue

/**
 * 添加变量BarCode item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/08/30
 */
class AddVariableBarCodeItem : CanvasIconItem() {

    init {
        itemClick = {
            it.context.variableTextDialog {
                varElementType = LPDataConstant.DATA_TYPE_VARIABLE_BARCODE
                onApplyVariableListAction = {
                    LPElementHelper.addVariableTextElement(itemRenderDelegate, it, varElementType)
                    UMEvent.CANVAS_VARIABLE_BARCODE.umengEventValue()
                }
            }
        }
    }

}