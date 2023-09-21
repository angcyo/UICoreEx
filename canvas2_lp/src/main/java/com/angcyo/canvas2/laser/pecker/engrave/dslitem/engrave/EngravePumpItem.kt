package com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave

import android.widget.ImageView
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.bean.PumpConfigBean
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.EngraveSegmentScrollItem
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.getSelectedSegmentBean
import com.angcyo.item.style.itemCurrentIndex
import com.angcyo.laserpacker.bean.LPElementBean
import com.angcyo.library.ex._string
import com.angcyo.library.ex.find
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity
import com.angcyo.objectbox.laser.pecker.lpSaveEntity
import com.angcyo.tablayout.DslTabLayout
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.resetChild
import kotlin.math.max

/**
 * 雕刻参数, 气泵风速选择
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/15
 */
class EngravePumpItem : EngraveSegmentScrollItem() {

    companion object {
        const val PAYLOAD_UPDATE_PUMP = 0x1000
    }

    /**参数配置实体*/
    var itemEngraveConfigEntity: EngraveConfigEntity? = null

    /**单元素参数配置*/
    var itemEngraveItemBean: LPElementBean? = null

    init {
        itemText = _string(R.string.engrave_pump_label)
        itemSegmentLayoutId = R.layout.layout_engrave_pump_segment

        itemUpdateAction = {
            if (it == PAYLOAD_UPDATE_PUMP) {
                val pumpList = itemSegmentList as? List<PumpConfigBean>
                if (pumpList != null) {
                    val pump = itemEngraveConfigEntity?.pump ?: itemEngraveItemBean?.pump
                    itemCurrentIndex = max(
                        0,
                        pumpList.indexOf(pumpList.find { it.value == pump })
                    )
                    onSelfPumpChange()
                }
            }
        }
    }

    override fun onSelfItemInitTabSegmentLayout(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        itemHolder.v<DslTabLayout>(tabLayoutItemConfig.itemTabLayoutViewId)?.apply {
            tabLayoutConfig?.tabEnableIcoColor = false
            resetChild(itemSegmentList, itemSegmentLayoutId) { itemView, item, itemIndex ->
                itemView.find<ImageView>(R.id.lib_image_view)?.setImageResource(
                    when (itemIndex) {
                        0 -> R.drawable.engrave_pump_level1_selector
                        1 -> R.drawable.engrave_pump_level2_selector
                        2 -> R.drawable.engrave_pump_level3_selector
                        else -> R.drawable.engrave_pump_level3_selector
                    }
                )
            }
        }
    }

    /**初始化默认的气泵参数*/
    fun initPumpIfNeed() {
        itemEngraveConfigEntity?.apply {
            if (pump < 0) {
                pump = LaserPeckerHelper.getLastPump(layerId)
                lpSaveEntity()
            }
        }
        itemEngraveItemBean?.apply {
            if ((pump ?: -1) < 0) {
                pump = LaserPeckerHelper.getLastPump(_layerId)
            }
        }
    }

    override fun onItemChangeListener(item: DslAdapterItem) {
        super.onItemChangeListener(item)
        onSelfPumpChange()
    }

    /**值改变时, 需要进行的操作*/
    fun onSelfPumpChange() {
        val value = getSelectedSegmentBean<PumpConfigBean>()?.value
        itemEngraveConfigEntity?.apply {
            pump = value ?: pump
            lpSaveEntity()
        }
        itemEngraveItemBean?.apply {
            pump = value ?: pump
        }
    }

}