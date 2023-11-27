package com.angcyo.laserpacker.device.ble.dslitem

import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.DslSegmentTabItem
import com.angcyo.item.style.itemCurrentIndex
import com.angcyo.laserpacker.device.R

/**
 * 抖动模式配置
 *  1: floyd
 *  2: atkinson
 *  3: stucki def
 *  4: burkes
 *  5: jarvis
 *  6: sierra3
 *  _: stucki
 * [com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys.ditherModeConfig]
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/11/18
 */
class DitherModeConfigItem : DslSegmentTabItem() {

    val ditherModeList = listOf(
        "floyd",
        "atkinson",
        "stucki",
        "burkes",
        "jarvis",
        "sierra3",
    )

    init {
        itemLayoutId = R.layout.dither_mode_config_item
        itemSegmentList = ditherModeList
        val index = ditherModeList.indexOf(HawkEngraveKeys.ditherModeConfig)
        if (index == -1) {
            itemCurrentIndex = 2
        } else {
            itemCurrentIndex = index
        }
    }

    override fun onItemChangeListener(item: DslAdapterItem) {
        super.onItemChangeListener(item)
        HawkEngraveKeys.ditherModeConfig = ditherModeList.getOrNull(itemCurrentIndex)
    }
}