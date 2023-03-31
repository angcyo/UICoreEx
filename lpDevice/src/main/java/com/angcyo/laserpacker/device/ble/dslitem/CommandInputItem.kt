package com.angcyo.laserpacker.device.ble.dslitem

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.checksum
import com.angcyo.bluetooth.fsc.laserpacker.command.CustomCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.sendCommand
import com.angcyo.bluetooth.fsc.laserpacker.parse.NoDeviceException
import com.angcyo.component.hawkInstallAndRestore
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.laserpacker.device.ble.bluetoothSearchListDialog
import com.angcyo.item.style.EditItemConfig
import com.angcyo.item.style.IEditItem
import com.angcyo.item.style.itemEditText
import com.angcyo.laserpacker.device.R
import com.angcyo.library.ex.beautifyHex
import com.angcyo.library.ex.removeAll
import com.angcyo.library.ex.toHexString
import com.angcyo.library.toast
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.span.span

/**
 * 指令输入Item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/25
 */
class CommandInputItem : DslAdapterItem(), IEditItem {

    override var editItemConfig: EditItemConfig = EditItemConfig()

    private val inputValue: String
        get() = "${itemEditText ?: ""}".removeAll().uppercase()

    /**过滤回调*/
    var itemFilterAction: (CharSequence) -> Unit = {}

    init {
        itemLayoutId = R.layout.item_command_input_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.hawkInstallAndRestore("CommandInput_")

        itemHolder.click(R.id.trim_button) {
            //移除指令长度和校验和
            val input = inputValue
            if (input.length > 6 + 4) {
                itemEditText = input.slice(6..input.length - 4)
                updateAdapterItem()
            }
        }

        itemHolder.click(R.id.pad_button) {
            //补齐指令长度和校验和
            val input = inputValue
            val head = "AABB"
            val len = (input.length / 2 + 2).toHexString()
            val sum = input.checksum(hasSpace = false)
            val value = "$head$len$input$sum"
            itemEditText = value

            itemHolder.tv(R.id.lib_text_view)?.text = span {
                append(value)
                appendln()
                append(value.beautifyHex(" $0"))
            }

            updateAdapterItem()
        }

        itemHolder.click(R.id.send_button) {
            val input = inputValue
            if (input.length < 6) {
                toast("无效的指令")
            } else {
                CustomCmd(input).sendCommand { bean, error ->
                    if (error is NoDeviceException) {
                        it.context.bluetoothSearchListDialog {
                            connectedDismiss = true
                        }
                    } else {
                        toast(error?.message ?: "指令发送成功")
                    }
                }
            }
        }

        itemHolder.click(R.id.filter_button) {
            itemFilterAction(itemEditText ?: "")
        }
    }

    override fun onItemChangeListener(item: DslAdapterItem) {
        //super.onItemChangeListener(item)
    }

}