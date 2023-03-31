package com.angcyo.laserpacker.device.ble.dslitem

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.laserpacker.device.R
import com.angcyo.library.ex._color
import com.angcyo.library.ex.fullTime
import com.angcyo.library.ex.toMsTime
import com.angcyo.objectbox.laser.pecker.entity.DeviceConnectEntity
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.span.span

/**
 * 设备连接记录item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/01/05
 */
class ConnectHistoryItem : DslAdapterItem() {

    /**连接记录*/
    var itemConnectEntity: DeviceConnectEntity? = null

    init {
        itemLayoutId = R.layout.item_command_history_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.tv(R.id.time_view)?.text = span {
            val sendTime = itemConnectEntity?.connectTime ?: 0
            val resultTime = itemConnectEntity?.disconnectTime
            append(sendTime.fullTime())
            if (resultTime == null) {
                append(" 未断开")
            } else {
                append(" 连接时长:${(resultTime - sendTime).toMsTime()}")
            }
            if (itemConnectEntity?.isAutoConnect == true) {
                append(" √")
            }
        }

        itemHolder.tv(R.id.command_view)?.text = span {
            append("->${itemConnectEntity?.deviceName ?: "?"} ")
            append(itemConnectEntity?.deviceAddress) {
                foregroundColor = _color(R.color.text_sub_color)
            }
        }

        itemHolder.gone(R.id.result_view)
    }
}