package com.angcyo.canvas2.laser.pecker.dialog.dslitem

import android.view.View
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.style.EditItemConfig
import com.angcyo.item.style.IEditItem
import com.angcyo.item.style.itemEditText
import com.angcyo.item.style.itemMaxEditLines
import com.angcyo.item.style.itemMaxInputLength
import com.angcyo.library.ex.hawkGetList
import com.angcyo.library.ex.hawkPutList
import com.angcyo.library.ex.reverseCharSequenceIfRtl
import com.angcyo.library.ex.toStr
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.dslViewHolder
import com.angcyo.widget.base.resetChild
import com.angcyo.widget.base.setInputText
import com.angcyo.widget.flow
import com.angcyo.widget.pager.TextIndicator

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/04
 */
class VarTextFixedItem : BaseVarItem(), IEditItem {

    companion object {
        const val VAR_KEY_ADD_TEXT = "var_key_add_text"
    }

    override var editItemConfig: EditItemConfig = EditItemConfig()

    /**输入历史*/
    var inputHistoryList: List<CharSequence>? = null

    /**最大显示的输入历史数量*/
    var inputHistoryMaxLimit: Int = 10

    /**hawk key, 自动读取到[inputHistoryList]和保存*/
    var inputHistoryHawkKey: String? = null
        set(value) {
            field = value
            value?.hawkGetList(maxCount = inputHistoryMaxLimit)?.let {
                inputHistoryList = it
            }
        }

    init {
        itemLayoutId = R.layout.item_var_text_fixed_layout
        itemMaxInputLength = HawkEngraveKeys.maxInputTextLengthLimit
        itemMaxEditLines = Int.MAX_VALUE

        inputHistoryHawkKey = VAR_KEY_ADD_TEXT
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
        val editText = itemHolder.ev(editItemConfig.itemEditTextViewId)

        //history
        itemHolder.invisible(R.id.lib_flow_layout, inputHistoryList.isNullOrEmpty())
        itemHolder.flow(R.id.lib_flow_layout)?.apply {
            resetChild(
                inputHistoryList,
                R.layout.lib_input_history_layout
            ) { itemView, item, itemIndex ->
                itemView.dslViewHolder().apply {
                    tv(R.id.lib_text_view)?.text = item

                    //删除
                    click(R.id.lib_delete_view) {
                        inputHistoryList?.filterTo(mutableListOf()) { it != item }?.let {
                            inputHistoryHawkKey?.hawkPutList(it)
                        }
                        removeView(itemView)
                    }

                    //上屏
                    clickItem {
                        editText?.setInputText(item)
                    }
                }
            }
        }

        //字符指示器
        val indicatorView = itemHolder.v<TextIndicator>(R.id.single_text_indicator_view)
        if (itemMaxInputLength >= 0) {
            indicatorView?.visibility = View.VISIBLE
            indicatorView?.setupEditText(editText, itemMaxInputLength)
        } else {
            indicatorView?.visibility = View.INVISIBLE
        }
    }

    override fun onSelfSetItemData(data: Any?) {
        super.onSelfSetItemData(data)
        itemEditText = _itemVariableBean?.content
    }

    override fun onItemChangeListener(item: DslAdapterItem) {
        //super.onItemChangeListener(item)
        _itemVariableBean?.content = itemEditText?.reverseCharSequenceIfRtl()?.toStr()
    }
}