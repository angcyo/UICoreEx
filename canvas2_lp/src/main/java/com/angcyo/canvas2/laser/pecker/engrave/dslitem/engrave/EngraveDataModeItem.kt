package com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave

import com.angcyo.bluetooth.fsc.laserpacker.data.DataTypeInfo
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.DslCheckFlowItem
import com.angcyo.item.style.itemCheckItems
import com.angcyo.item.style.itemCheckedItems
import com.angcyo.item.style.itemText
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.library.ex._string
import com.angcyo.objectbox.laser.pecker.entity.TransferConfigEntity

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
        DataTypeInfo(LPDataConstant.DATA_MODE_GREY, "图片"),
        DataTypeInfo(LPDataConstant.DATA_MODE_BLACK_WHITE, "路径"),
        DataTypeInfo(LPDataConstant.DATA_MODE_DITHERING, "抖动"),
        DataTypeInfo(LPDataConstant.DATA_MODE_GCODE, "GCode")
    )

    /**待雕刻的准备数据*/
    var itemEngraveReadyInfo: TransferConfigEntity? = null
        set(value) {
            field = value
            itemTypeList.find { it.type == value?.dataMode }?.let {
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
            dataMode = (selected as DataTypeInfo).type
        }
    }

}