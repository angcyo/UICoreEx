package com.angcyo.engrave.ble.dslitem

import com.angcyo.bluetooth.fsc.laserpacker.command.parseResultPacketLog
import com.angcyo.dialog.itemsDialog
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.library.ex.copy
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

        itemLongClick = {
            it.context.itemsDialog {
                addDialogItem {
                    itemText = "复制指令"
                    itemClick = {
                        itemCommandEntity?.des?.copy(it.context)
                    }
                }
                addDialogItem {
                    itemText = "复制返回值"
                    itemClick = {
                        itemCommandEntity?.result?.copy(it.context)
                    }
                }
            }
            true
        }
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
            itemCommandEntity?.let { entity ->
                append("<-${entity.result ?: "?"}")
                entity.result?.parseResultPacketLog(entity.func, entity.state)?.let {
                    appendln()
                    append("$it")
                }
            }
        }
    }
}