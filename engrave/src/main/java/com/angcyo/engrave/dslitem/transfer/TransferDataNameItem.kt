package com.angcyo.engrave.dslitem.transfer

import android.graphics.Color
import com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.engrave.data.TransferDataConfigInfo
import com.angcyo.item.style.EditItemConfig
import com.angcyo.item.style.IEditItem
import com.angcyo.item.style.itemEditText
import com.angcyo.library.ex._string
import com.angcyo.library.ex.byteSize
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.span.span

/**
 * 传输数据名字输入item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/01
 */
class TransferDataNameItem : DslAdapterItem(), IEditItem {

    /**数据配置信息*/
    var itemTransferDataConfigInfo: TransferDataConfigInfo? = null
        set(value) {
            field = value
            itemEditText = value?.name
        }

    override var editItemConfig: EditItemConfig = EditItemConfig().apply {
        itemEditTextStyle.hint = _string(R.string.filename_hint)
    }

    init {
        itemLayoutId = R.layout.item_engrave_data_name
        editItemConfig.itemTextChangeShakeDelay = 0//去掉抖动, 实时回调

        configEditTextStyle {
            editMaxInputLength = DataCmd.DEFAULT_NAME_BYTE_COUNT
        }
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
    }

    override fun initItemConfig(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.initItemConfig(itemHolder, itemPosition, adapterItem, payloads)
        onItemEditTextChange(itemHolder, itemEditText ?: "")
    }

    override fun onItemEditTextChange(itemHolder: DslViewHolder, text: CharSequence) {
        super.onItemEditTextChange(itemHolder, text)
        itemHolder.tv(R.id.lib_bytes_view)?.text = span {
            val bytes = text.toString().byteSize()
            append("$bytes") {
                if (bytes > DataCmd.DEFAULT_NAME_BYTE_COUNT) {
                    itemThrowable = IllegalArgumentException("Text out of limit!")
                    foregroundColor = Color.RED
                } else {
                    itemThrowable = null
                    itemTransferDataConfigInfo?.name = text.toString()
                }
            }
            append("/${DataCmd.DEFAULT_NAME_BYTE_COUNT} bytes")
        }
    }

    override fun onItemChangeListener(item: DslAdapterItem) {
        //super.onItemChangeListener(item)
    }

}