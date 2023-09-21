package com.angcyo.canvas2.laser.pecker.dslitem.item

import com.angcyo.canvas2.laser.pecker.dialog.variableTextDialog
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.canvas2.laser.pecker.dslitem.control.initBarcodeIfNeed
import com.angcyo.canvas2.laser.pecker.util.LPElementHelper
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.bean.LPElementBean
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue

/**
 * 添加变量QrCode item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/08/30
 */
class AddVariableQrCodeItem : CanvasIconItem() {

    init {
        itemClick = {
            it.context.variableTextDialog {
                varElementBean =
                    LPElementBean(mtype = LPDataConstant.DATA_TYPE_VARIABLE_QRCODE).apply {
                        initBarcodeIfNeed()
                    }
                onApplyVariableListAction = {
                    LPElementHelper.addElementRender(itemRenderDelegate, it)
                    UMEvent.CANVAS_VARIABLE_QRCODE.umengEventValue()
                }
            }
        }
    }

}