package com.angcyo.github.dslitem

import android.app.Dialog
import android.content.Context
import android.view.View
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.github.R
import com.angcyo.github.dialog.WheelDialogConfig
import com.angcyo.github.dialog.wheelDialog
import com.angcyo.item.DslBaseLabelItem
import com.angcyo.item.TextStyleConfig
import com.angcyo.library.ex.string
import com.angcyo.widget.DslViewHolder

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/23
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
open class DslLabelWheelItem : DslBaseLabelItem() {

    /**数据集合*/
    var itemWheelList = mutableListOf<Any>()

    /**设置选中项, -1不设置*/
    var itemSelectedIndex = -1

    /**统一样式配置*/
    var itemTextStyle = TextStyleConfig()

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

    /**点击item之前拦截处理, 返回true拦截默认处理*/
    var itemClickBefore: (clickView: View) -> Boolean = { false }

    init {
        itemLayoutId = R.layout.dsl_wheel_item

        itemClick = {
            if (itemEnable && !itemClickBefore(it)) {
                showWheelDialog(it.context)
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
        itemHolder.tv(R.id.lib_text_view)?.apply {
            itemTextStyle.updateStyle(this)
            text = itemWheelList.getOrNull(itemSelectedIndex)?.run {
                itemWheelToText(this)
            }
        }
    }

    /**显示dialog*/
    open fun showWheelDialog(context: Context) {
        context.wheelDialog {
            dialogTitle = itemLabelText

            wheelItems = itemWheelList

            wheelItemToStringAction = itemWheelToText

            wheelItemSelectorAction = { dialog, index, item ->
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

    open fun configTextStyle(action: TextStyleConfig.() -> Unit) {
        itemTextStyle.action()
    }
}