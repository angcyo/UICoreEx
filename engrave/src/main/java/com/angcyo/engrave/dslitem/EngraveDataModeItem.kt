package com.angcyo.engrave.dslitem

import com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd
import com.angcyo.bluetooth.fsc.laserpacker.data.EngraveTypeInfo
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.engrave.data.EngraveReadyInfo
import com.angcyo.engrave.transition.convertDataModeToEngraveType
import com.angcyo.engrave.transition.convertEngraveTypeToDataMode
import com.angcyo.item.DslCheckFlowItem
import com.angcyo.item.style.itemCheckItems
import com.angcyo.item.style.itemCheckedItems
import com.angcyo.item.style.itemText
import com.angcyo.library.ex._string

/**
 * 雕刻数据模式调整item, 用来决定雕刻的数据
 *
 * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_BITMAP]
 * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_BITMAP_PATH]
 * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_BITMAP_DITHERING]
 * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_GCODE]
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/08/29
 */
class EngraveDataModeItem : DslCheckFlowItem() {

    /**数据模式列表*/
    var itemTypeList = mutableListOf(
        EngraveTypeInfo(DataCmd.ENGRAVE_TYPE_BITMAP, "图片"),
        EngraveTypeInfo(DataCmd.ENGRAVE_TYPE_BITMAP_PATH, "路径"),
        EngraveTypeInfo(DataCmd.ENGRAVE_TYPE_BITMAP_DITHERING, "抖动"),
        EngraveTypeInfo(DataCmd.ENGRAVE_TYPE_GCODE, "GCode")
    )

    /**待雕刻的准备数据*/
    var itemEngraveReadyInfo: EngraveReadyInfo? = null
        set(value) {
            field = value
            itemTypeList.find { it.type == value?.dataMode?.convertDataModeToEngraveType() }?.let {
                //默认选中
                itemCheckedItems = mutableListOf(it)
            }
        }

    init {
        itemText = _string(R.string.ui_slip_menu_model)
        itemCheckItems = itemTypeList

        itemLayoutId = R.layout.item_engrave_data_px
    }

    override fun onItemChangeListener(item: DslAdapterItem) {
        super.onItemChangeListener(item)
        val selected = itemCheckedItems.first()
        itemEngraveReadyInfo?.apply {
            dataMode = (selected as EngraveTypeInfo).type.convertEngraveTypeToDataMode()
        }
    }

}