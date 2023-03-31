package com.angcyo.canvas2.laser.pecker.engrave.dslitem.transfer

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.data.PxInfo
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.EngraveSegmentScrollItem
import com.angcyo.item.style.itemCurrentIndex
import com.angcyo.library.ex._string
import com.angcyo.objectbox.laser.pecker.entity.TransferConfigEntity
import com.angcyo.tablayout.DslTabLayout
import com.angcyo.widget.DslViewHolder
import kotlin.math.max

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
    var itemTransferConfigEntity: TransferConfigEntity? = null
        set(value) {
            field = value
            val index = itemPxList?.indexOfFirst { it.dpi == value?.dpi } ?: 0
            //默认选中
            itemCurrentIndex = max(index, 0)
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

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
        itemHolder.v<DslTabLayout>(tabLayoutItemConfig.itemTabLayoutViewId)?.apply {
            itemEquWidthCountRange = null
        }
    }

    override fun onItemChangeListener(item: DslAdapterItem) {
        //super.onItemChangeListener(item)
        val dpi = itemPxList?.get(itemCurrentIndex)?.dpi ?: LaserPeckerHelper.DPI_254
        itemTransferConfigEntity?.dpi = dpi
    }

}