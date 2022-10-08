package com.angcyo.engrave.dslitem.transfer

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.data.PxInfo
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.engrave.data.TransferDataConfigInfo
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
class TransferDataPxItem : EngraveSegmentScrollItem() {

    /**数据配置信息*/
    var itemTransferDataConfigInfo: TransferDataConfigInfo? = null
        set(value) {
            field = value
            val index = itemPxList?.indexOfFirst { it.px == value?.px }
            //默认选中
            itemCurrentIndex = index ?: 0
        }

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
        //super.onItemChangeListener(item)
        val px = itemPxList?.get(itemCurrentIndex)?.px ?: LaserPeckerHelper.DEFAULT_PX
        itemTransferDataConfigInfo?.px = px
    }

}