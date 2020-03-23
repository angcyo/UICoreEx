package com.angcyo.github.dslitem

import android.app.Dialog
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.github.R
import com.angcyo.github.dialog.WheelDialogConfig
import com.angcyo.github.dialog.wheelDialog
import com.angcyo.item.DslBaseLabelItem
import com.angcyo.library.ex.string
import com.angcyo.widget.DslViewHolder

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/23
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
class DslLabelWheelItem : DslBaseLabelItem() {

    /**数据集合*/
    var itemWheelList = mutableListOf<Any>()

    /**设置选中项, -1不设置*/
    var itemSelectedIndex = -1

    /**选中回调*/
    var itemWheelSelector: (dialog: Dialog, index: Int, item: Any) -> Boolean =
        { dialog, index, item ->
            false
        }

    /**上屏显示转换回调*/
    var itemWheelToText: (item: Any) -> CharSequence = {
        it.string()
    }

    var itemConfigDialog: (WheelDialogConfig) -> Unit = {

    }

    init {
        itemLayoutId = R.layout.dsl_wheel_item

        itemClick = {
            it.context.wheelDialog {
                dialogTitle = itemLabelText

                wheelItems = itemWheelList

                onWheelItemToString = itemWheelToText

                onWheelItemSelector = { dialog, index, item ->
                    if (itemWheelSelector(dialog, index, item)) {
                        //拦截了
                        true
                    } else {
                        val old = itemSelectedIndex
                        itemSelectedIndex = index
                        itemChanging = old != index
                        false
                    }
                }

                wheelSelectedIndex = itemSelectedIndex

                itemConfigDialog(this)
            }
        }
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
        itemHolder.tv(R.id.lib_text_view)?.text = itemWheelList.getOrNull(itemSelectedIndex)?.run {
            itemWheelToText(this)
        }
    }
}