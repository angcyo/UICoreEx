package com.angcyo.engrave.ble.dslitem

import com.angcyo.bluetooth.fsc.laserpacker.command.parsePacketLog
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.library.ex.fullTime
import com.angcyo.library.ex.toMsTime
import com.angcyo.objectbox.laser.pecker.entity.CommandEntity
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.span.span

/**
 * 指令记录item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/25
 */
class CommandHistoryItem : DslAdapterItem() {

    /**指令记录*/
    var itemCommandEntity: CommandEntity? = null

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
            val sendTime = itemCommandEntity?.sendTime ?: 0
            val resultTime = itemCommandEntity?.resultTime ?: 0
            append(sendTime.fullTime())
            if (resultTime > 0) {
                append(" 耗时:${(resultTime - sendTime).toMsTime()}")
            }
        }

        itemHolder.tv(R.id.command_view)?.text = span {
            append("->${itemCommandEntity?.des ?: "?"}")
        }

        itemHolder.tv(R.id.result_view)?.text = span {
            append("<-${itemCommandEntity?.result ?: "?"}")
            itemCommandEntity?.result?.parsePacketLog()?.let {
                appendln()
                append("$it")
            }
        }
    }
}