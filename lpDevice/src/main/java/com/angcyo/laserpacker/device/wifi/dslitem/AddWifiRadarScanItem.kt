package com.angcyo.laserpacker.device.wifi.dslitem

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.laserpacker.device.R
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.loading.RadarScanLoadingView

/**
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2023/07/30
 */
class AddWifiRadarScanItem : DslAdapterItem() {

    /**是否开始扫描动画*/
    var itemScan: Boolean = true

    init {
        itemLayoutId = R.layout.item_add_wifi_radar_scan
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        //radar
        itemHolder.v<RadarScanLoadingView>(R.id.radar_scan_loading_view)?.loading(itemScan)

        /*itemHolder.visible(
            R.id.radar_scan_loading_view,
            it == FscBleApiModel.BLUETOOTH_STATE_SCANNING
        )*/

    }

}