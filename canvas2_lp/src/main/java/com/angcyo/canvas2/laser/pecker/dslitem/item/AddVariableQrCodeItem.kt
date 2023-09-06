package com.angcyo.canvas2.laser.pecker.dslitem.item

import com.angcyo.canvas2.laser.pecker.dialog.variableTextDialog
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
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
            UMEvent.CANVAS_VARIABLE_QRCODE.umengEventValue()
            it.context.variableTextDialog {

            }
        }
    }

}