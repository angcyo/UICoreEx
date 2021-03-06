package com.angcyo.github.dialog

import android.app.Dialog
import android.content.Context
import com.angcyo.dialog.BaseDialogConfig
import com.angcyo.dialog.configBottomDialog
import com.angcyo.github.R
import com.angcyo.github.widget.wheel.ArrayWheelAdapter
import com.angcyo.library.L
import com.angcyo.widget.DslViewHolder
import com.contrarywind.view.WheelView

/**
 * 3D滚轮选择对话框配置
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/11
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */

open class WheelDialogConfig : BaseDialogConfig() {

    /**数据集合*/
    var wheelItems: MutableList<Any>? = null

    /**是否无限循环*/
    var wheelCyclic = false

    /**设置选中项, -1不设置*/
    var wheelSelectedIndex = -1

    /**选中回调*/
    var wheelItemSelectorAction: (dialog: Dialog, index: Int, item: Any) -> Boolean =
        { dialog, index, item ->
            L.i("选中->$index:${wheelItemToStringAction(item)}")
            false
        }

    /**上屏显示转换回调*/
    var wheelItemToStringAction: (item: Any) -> CharSequence = {
        if (it is CharSequence) {
            it
        } else {
            it.toString()
        }
    }

    //内部变量
    var _selectedIndex = -1

    init {
        dialogLayoutId = R.layout.lib_dialog_wheel_layout

        positiveButtonListener = { dialog, _ ->
            if (_selectedIndex in 0 until (wheelItems?.size ?: 0) &&
                wheelItemSelectorAction.invoke(dialog, _selectedIndex, wheelItems!![_selectedIndex])
            ) {
            } else {
                dialog.dismiss()
            }
        }
    }

    override fun initDialogView(dialog: Dialog, dialogViewHolder: DslViewHolder) {
        super.initDialogView(dialog, dialogViewHolder)

        dialogViewHolder.enable(R.id.positive_button, !wheelItems.isNullOrEmpty())

        dialogViewHolder.v<WheelView>(R.id.lib_wheel_view)?.apply {
            val stringList = mutableListOf<CharSequence>()

            for (item in wheelItems ?: emptyList()) {
                stringList.add(wheelItemToStringAction.invoke(item))
            }

            setOnItemSelectedListener {
                _selectedIndex = it
                L.v("wheel selected $it")
            }

            adapter = ArrayWheelAdapter(stringList)

            setCyclic(wheelCyclic)

            //wheel 在没有滑动的时候, 是不会触发[SelectedListener]的
            currentItem = if (wheelSelectedIndex in 0 until (wheelItems?.size ?: 0)) {
                wheelSelectedIndex
            } else {
                0
            }
            _selectedIndex = currentItem
        }

        //空数据提示
        dialogViewHolder.visible(R.id.lib_empty_view, wheelItems.isNullOrEmpty())
    }

    /**添加Item*/
    fun addDialogItem(any: Any) {
        if (wheelItems == null) {
            wheelItems = mutableListOf()
        }
        wheelItems?.add(any)
    }
}

/**
 * 3D滚轮选择对话框
 * */
fun Context.wheelDialog(config: WheelDialogConfig.() -> Unit): Dialog {
    return WheelDialogConfig().run {
        configBottomDialog(this@wheelDialog)
        config()
        show()
    }
}

