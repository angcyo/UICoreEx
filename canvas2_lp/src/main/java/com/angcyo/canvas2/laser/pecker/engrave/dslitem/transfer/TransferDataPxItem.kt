package com.angcyo.canvas2.laser.pecker.engrave.dslitem.transfer

import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.data.PxInfo
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.EngraveSegmentScrollItem
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.style.itemCurrentIndex
import com.angcyo.laserpacker.device.data.EngraveLayerInfo
import com.angcyo.laserpacker.device.filterLayerDpi
import com.angcyo.library.ex._string
import com.angcyo.library.ex.clamp
import com.angcyo.library.ex.size
import com.angcyo.objectbox.laser.pecker.entity.TransferConfigEntity
import com.angcyo.tablayout.DslTabLayout
import com.angcyo.widget.DslViewHolder

/**
 * 数据分辨率选择, Dpi
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
            selectorCurrentDpi(value?.getLayerConfigDpi(HawkEngraveKeys.lastLayerId))
        }

    /**分辨率列表*/
    var itemPxList: List<PxInfo>? = null
        set(value) {
            field = value
            itemSegmentList = value ?: emptyList()
        }

    /**雕刻图层信息, 每个图层都有自己的分辨率*/
    var itemLayerInfo: EngraveLayerInfo? = null
        set(value) {
            field = value
            itemText = value?.label ?: itemText
            HawkEngraveKeys._lastLayerConfigList?.find { it.layerId == value?.layerId }?.apply {
                selectorCurrentDpi(dpi)
            }
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
        val layerId = (itemLayerInfo?.layerId ?: HawkEngraveKeys.lastLayerId)

        HawkEngraveKeys.updateLayerDpi(layerId, layerId.filterLayerDpi(dpi))
        itemTransferConfigEntity?.layerJson = HawkEngraveKeys.lastDpiLayerJson
    }

    fun selectorCurrentDpi(dpi: Float?) {
        itemCurrentIndex = if (dpi == null) {
            0
        } else {
            val index = itemPxList?.indexOfFirst { it.dpi == dpi } ?: 0
            clamp(index, 0, itemPxList.size())
        }
    }

}