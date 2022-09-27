package com.angcyo.engrave.dslitem.engrave

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.engrave.data.EngraveOptionInfo
import com.angcyo.item.DslCheckFlowItem
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
            //默认选中
            itemCheckedItems.add(value?.type ?: LaserPeckerHelper.LASER_TYPE_BLUE)
        }

    init {
        itemText = "${_string(R.string.laser_type)}:"
        itemLayoutId = R.layout.item_engrave_data_px
        itemCheckItems = vmApp<LaserPeckerModel>().productInfoData.value?.laserTypeList ?: emptyList()
        checkGroupItemConfig.itemCheckItemToText = {
            if (it == LaserPeckerHelper.LASER_TYPE_WHITE) {
                _string(R.string.laser_type_white)
            } else {
                _string(R.string.laser_type_blue)
            }
        }
    }

    override fun onItemChangeListener(item: DslAdapterItem) {
        super.onItemChangeListener(item)
        val byte = itemCheckedItems.firstOrNull() as? Byte
        itemEngraveOptionInfo?.apply {
            type = byte ?: LaserPeckerHelper.LASER_TYPE_BLUE
        }
    }
}