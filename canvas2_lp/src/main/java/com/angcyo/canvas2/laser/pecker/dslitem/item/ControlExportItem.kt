package com.angcyo.canvas2.laser.pecker.dslitem.item

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dialog.exportDataDialogConfig
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.canvas2.laser.pecker.engrave.LPEngraveHelper
import com.angcyo.library.ex._string
import com.angcyo.library.toastQQ

/**
 * 导出lpb文件数据
 *
 * [com.angcyo.canvas2.laser.pecker.manager.dslitem.LpbFileItem]
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023-9-18
 */
class ControlExportItem : CanvasIconItem() {

    init {
        itemIco = R.drawable.canvas_export_data_ico
        itemText = _string(R.string.canvas_export_data)
        itemEnable = true
        itemClick = {
            val list = LPEngraveHelper.getAllValidRendererList(itemRenderDelegate)
            if (list.isNullOrEmpty()) {
                toastQQ(_string(R.string.no_data_transfer))
            } else {
                it.context.exportDataDialogConfig(itemRenderDelegate)
            }
        }
    }
}