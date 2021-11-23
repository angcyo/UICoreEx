package com.angcyo.github.dslitem

import android.app.Dialog
import android.content.Context
import android.view.View
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.github.R
import com.angcyo.github.dialog.WheelDialogConfig
import com.angcyo.github.dialog.wheelDialog
import com.angcyo.item.DslBaseLabelItem
import com.angcyo.item.extend.IToText
import com.angcyo.item.style.ILoadItem
import com.angcyo.item.style.ITextItem
import com.angcyo.item.style.LoadItemConfig
import com.angcyo.item.style.TextItemConfig
import com.angcyo.library.ex.string
import com.angcyo.widget.DslViewHolder

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/23
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
open class DslLabelWheelItem : DslBaseLabelItem(), ITextItem, ILoadItem {

    /**数据集合*/
    var itemWheelList: List<Any>? = null

    /**设置选中项, -1不设置*/
    var itemSelectedIndex = -1

    /**选中回调*/
    var itemWheelSelector: (dialog: Dialog, index: Int, item: Any) -> Boolean =
        { dialog, index, item ->
            false
        }

    /**上屏显示转换回调*/
    var itemWheelToText: (item: Any) -> CharSequence? = {
        if (it is IToText) {
            it.toText()
        } else {
            it.string()
        }
    }

    var itemConfigDialog: (WheelDialogConfig) -> Unit = {

    }

    /**点击item之前拦截处理, 返回true拦截默认处理*/
    var itemClickBefore: (clickView: View) -> Boolean = { false }

    override var textItemConfig: TextItemConfig = TextItemConfig()
    override var loadItemConfig: LoadItemConfig = LoadItemConfig()

    init {
        itemLayoutId = R.layout.dsl_wheel_item

        itemClick = { view ->
            if (itemEnable && !itemClickBefore(view)) {

                if (loadItemConfig.itemLoadAction == null) {
                    showWheelDialog(view.context)
                } else {
                    if (loadItemConfig.itemUseLoadCache && itemWheelList?.isNotEmpty() == true) {
                        showWheelDialog(view.context)
                    } else {
                        //异步加载
                        loadItemConfig.itemLoadAction?.invoke {
                            if (it == null) {
                                showWheelDialog(view.context)
                            }
                        }
                    }
                }
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
        itemHolder.tv(textItemConfig.itemTextViewId)?.apply {
            text = itemWheelList?.getOrNull(itemSelectedIndex)?.run {
                itemWheelToText(this)
            }
        }
        itemHolder.visible(R.id.lib_right_ico_view, itemEnable)
    }

    /**显示dialog*/
    open fun showWheelDialog(context: Context) {
        context.wheelDialog {
            dialogTitle = labelItemConfig.itemLabelText

            wheelItems = itemWheelList?.toMutableList()

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
}

/**快速获取对应Item的值*/
fun DslAdapterItem.itemWheelValue(): Any? {
    return if (this is DslLabelWheelItem) {
        itemWheelList?.getOrNull(itemSelectedIndex)
    } else {
        null
    }
}

fun <T> DslAdapterItem.itemWheelBean(): T? {
    return if (this is DslLabelWheelItem) {
        itemWheelList?.getOrNull(itemSelectedIndex) as T?
    } else {
        null
    }
}

inline fun <reified DATA> DslAdapterItem.itemWheelData(): DATA? {
    return if (this is DslLabelWheelItem) {
        itemWheelList?.getOrNull(itemSelectedIndex) as DATA?
    } else {
        null
    }
}