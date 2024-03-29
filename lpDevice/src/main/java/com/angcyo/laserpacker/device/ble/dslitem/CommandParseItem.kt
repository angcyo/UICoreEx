package com.angcyo.laserpacker.device.ble.dslitem

import com.angcyo.bluetooth.fsc.laserpacker.command.parseResultPacketLog
import com.angcyo.component.hawkInstallAndRestore
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.laserpacker.device.R
import com.angcyo.library.ex.toHexInt
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.string
import com.orhanobut.hawk.HawkValueParserHelper

/**
 * 指令返回值解析
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/01/03
 */
class CommandParseItem : DslAdapterItem() {

    init {
        itemLayoutId = R.layout.item_command_parse_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.hawkInstallAndRestore("CommandParse_")

        itemHolder.click(R.id.parse_result_button) {
            val text = itemHolder.tv(R.id.lib_edit_view).string()
            val func = itemHolder.tv(R.id.func_edit_view).string().toHexInt()
            val state = itemHolder.tv(R.id.state_edit_view).string().toHexInt()
            itemHolder.tv(R.id.lib_text_view)?.text = "${text.parseResultPacketLog(func, state)}"
        }

        itemHolder.click(R.id.parse_hawk_button) {
            val text = itemHolder.tv(R.id.lib_edit_view).string()
            val key = itemHolder.tv(R.id.func_edit_view).string()
            itemHolder.tv(R.id.lib_text_view)?.text = HawkValueParserHelper.parse(
                key,
                text
            )
        }
    }

    override fun onItemChangeListener(item: DslAdapterItem) {
        //super.onItemChangeListener(item)
    }

}