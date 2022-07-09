package com.angcyo.engrave.dslitem

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.engrave.data.EngraveOptionInfo
import com.angcyo.item.DslCheckFlowItem
import com.angcyo.item.style._itemCheckedIndexList
import com.angcyo.item.style.itemCheckItems
import com.angcyo.item.style.itemCheckedItems
import com.angcyo.item.style.itemText
import com.angcyo.library.ex._string

/**
 * 雕刻类型, 蓝光 白光
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/09
 */
class EngraveOptionTypeItem : DslCheckFlowItem() {

    //0为1064nm激光 (白光-雕)，1为450nm激光 (蓝光-烧)。(L3max新增)
    /**雕刻选项数据*/
    var itemEngraveOptionInfo: EngraveOptionInfo? = null
        set(value) {
            field = value
            if (value?.type == 0.toByte()) {
                //白光
                itemCheckedItems.add(itemCheckItems[0])
            } else {
                itemCheckedItems.add(itemCheckItems[1])
            }
        }

    init {
        itemText = "${_string(R.string.laser_type)}:"
        itemLayoutId = R.layout.item_engrave_data_px

        itemCheckItems =
            listOf(_string(R.string.laser_type_white), _string(R.string.laser_type_blue))
    }

    override fun onItemChangeListener(item: DslAdapterItem) {
        super.onItemChangeListener(item)
        val selected = _itemCheckedIndexList.firstOrNull()
        itemEngraveOptionInfo?.apply {
            type = if (selected == 0) {
                //白光
                LaserPeckerHelper.LASER_TYPE_WHITE
            } else {
                //蓝光
                LaserPeckerHelper.LASER_TYPE_BLUE
            }
        }
    }
}