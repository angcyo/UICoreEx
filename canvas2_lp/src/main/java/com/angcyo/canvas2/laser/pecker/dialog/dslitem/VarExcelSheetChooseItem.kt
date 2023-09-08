package com.angcyo.canvas2.laser.pecker.dialog.dslitem

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.dialog2.dslitem.LPLabelWheelItem
import com.angcyo.dialog2.dslitem.itemWheelList
import com.angcyo.dialog2.dslitem.updateWheelSelectedIndex
import com.angcyo.item.style.itemLabel
import com.angcyo.laserpacker.bean.LPVariableBean
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.ex._string

/**
 * Excel sheet 选择item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/07
 */
class VarExcelSheetChooseItem : LPLabelWheelItem() {

    init {
        itemLabel = _string(R.string.variable_file_sheet)
    }

    override fun onSetItemData(data: Any?) {
        super.onSetItemData(data)
        _itemVariableBean?.let { bean ->
            itemWheelList = bean.sheetList
            if (bean.sheet.isNullOrEmpty()) {
                bean.sheet = bean.sheetList.firstOrNull()
            }
            updateWheelSelectedIndex(bean.sheet)
        }
    }

    @CallPoint
    fun updateFileChoose(bean: LPVariableBean) {
        itemWheelList = bean.sheetList
        bean.sheet = bean.sheetList.firstOrNull()
        updateWheelSelectedIndex(bean.sheet)
        updateAdapterItem()
    }

}