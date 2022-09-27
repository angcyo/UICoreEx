package com.angcyo.engrave.dslitem.engrave

import com.angcyo.bluetooth.fsc.laserpacker.data.PxInfo
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.engrave.dslitem.EngraveSegmentScrollItem
import com.angcyo.item.style.itemCurrentIndex
import com.angcyo.library.ex._string

/**
 * 数据分辨率选择
 *
 * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.PX_1K]
 * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.PX_1_3K]
 * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.PX_2K]
 * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.PX_4K]
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/01
 */
class EngraveDataPxItem : EngraveSegmentScrollItem() {

    /**待雕刻的数据*/
    /*var itemEngraveDataInfo: EngraveDataInfo? = null
        set(value) {
            field = value
            LaserPeckerHelper.findPxInfo(value?.px)?.let {
                //默认选中
                itemCheckedItems = mutableListOf(it)
            }
        }*/

    /**分辨率列表*/
    var itemPxList: List<PxInfo>? = null
        set(value) {
            field = value
            itemSegmentList = value ?: emptyList()
        }

    init {
        itemText = _string(R.string.resolution_ratio)
    }

    override fun onItemChangeListener(item: DslAdapterItem) {
        super.onItemChangeListener(item)
        /*val selected = itemCheckedItems.first()
        itemEngraveDataInfo?.apply {
            px = (selected as PxInfo).px
        }*/
        itemCurrentIndex
    }

}