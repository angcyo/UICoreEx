package com.angcyo.canvas2.laser.pecker.engrave.dslitem.transfer

import android.graphics.Color
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd
import com.angcyo.bluetooth.fsc.laserpacker.filterFileName
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.style.EditItemConfig
import com.angcyo.item.style.IEditItem
import com.angcyo.item.style.itemEditText
import com.angcyo.library.ex._string
import com.angcyo.library.ex.byteSize
import com.angcyo.objectbox.laser.pecker.entity.TransferConfigEntity
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.span.span

/**
 * 传输数据名字输入item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/01
 */
class TransferDataNameItem : DslAdapterItem(), IEditItem {

    /**数据配置信息*/
    var itemTransferConfigEntity: TransferConfigEntity? = null
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
        onSelfItemEditTextChange(itemHolder, itemEditText ?: "")
    }

    override fun onSelfItemEditTextChange(itemHolder: DslViewHolder, text: CharSequence) {
        super.onSelfItemEditTextChange(itemHolder, text)
        val fileName = text.toString().filterFileName()
        itemHolder.tv(R.id.lib_bytes_view)?.text = span {
            val bytes = fileName.byteSize()
            append("$bytes") {
                if (bytes > DataCmd.DEFAULT_NAME_BYTE_COUNT) {
                    itemThrowable = IllegalArgumentException("Text out of limit!")
                    foregroundColor = Color.RED
                } else {
                    itemThrowable = null
                    itemTransferConfigEntity?.name = fileName
                }
            }
            append("/${DataCmd.DEFAULT_NAME_BYTE_COUNT} bytes")
        }
        //
        itemTransferConfigEntity?.name = fileName
        HawkEngraveKeys.lastTransferName = fileName
    }

    override fun onItemChangeListener(item: DslAdapterItem) {
        //super.onItemChangeListener(item)
    }

}