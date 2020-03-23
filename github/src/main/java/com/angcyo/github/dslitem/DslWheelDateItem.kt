package com.angcyo.github.dslitem

import android.app.Dialog
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.github.R
import com.angcyo.github.dialog.WheelDateDialogConfig
import com.angcyo.github.dialog.wheelDateDialog
import com.angcyo.item.DslBaseLabelItem
import com.angcyo.library.component.toCalendar
import com.angcyo.library.ex.nowTime
import com.angcyo.library.ex.toTime
import com.angcyo.widget.DslViewHolder
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/23
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
class DslWheelDateItem : DslBaseLabelItem() {

    companion object {
        //选择 年月日 时分秒
        const val TYPE_ALL = 0

        //选择 年月日
        const val TYPE_DATE = 1

        //时分秒
        const val TYPE_TIME = 2

        //时分
        const val TYPE_TIME_2 = 3
    }

    /**文本*/
    var itemWheelText: CharSequence? = null

    var itemDateTypeArray = booleanArrayOf(true, true, true, false, false, false)

    var itemDateType = TYPE_DATE
        set(value) {
            field = value
            itemDateTypeArray = when (value) {
                TYPE_ALL -> {
                    itemPattern = "$itemDatePattern $itemTimePattern"
                    booleanArrayOf(true, true, true, true, true, true)
                }
                TYPE_TIME -> {
                    itemPattern = itemTimePattern
                    booleanArrayOf(false, false, false, true, true, true)
                }
                TYPE_TIME_2 -> {
                    itemPattern = itemTimePattern
                    booleanArrayOf(false, false, false, true, true, false)
                }
                else -> {
                    itemPattern = itemDatePattern
                    booleanArrayOf(true, true, true, false, false, false)
                }
            }
        }

    /**分区配置时间格式*/
    var itemDatePattern = "yyyy-MM-dd"
    var itemTimePattern = "HH:mm:ss"

    /**日期 时间 全格式, 最终使用格式*/
    var itemPattern = itemDatePattern

    /**开始和结束时间*/
    var itemDateStartTime = 0L
    var itemDateEndTime = nowTime()

    /**点击确定后回调*/
    var itemDateSelectListener: (dialog: Dialog, date: Date) -> Boolean = { _, _ ->
        false
    }

    var itemConfigDialog: (WheelDateDialogConfig) -> Unit = {

    }

    init {
        itemLayoutId = R.layout.dsl_wheel_date_item

        itemClick = {
            it.context.wheelDateDialog {
                dialogTitle = itemLabelText

                dateType = itemDateTypeArray

                if (itemDateStartTime > 0) {
                    dateStartDate =
                        itemDateStartTime.toTime(itemPattern).toCalendar(itemPattern)
                }
                if (itemDateEndTime > 0) {
                    dateEndDate = itemDateEndTime.toTime(itemPattern).toCalendar(itemPattern)
                }

                onDateSelectListener = { dialog, date ->
                    if (itemDateSelectListener(dialog, date)) {
                        //拦截了
                        true
                    } else {
                        val dateFormat: DateFormat = SimpleDateFormat(itemPattern)
                        itemWheelText = dateFormat.format(date)
                        itemChanging = true
                        false
                    }
                }

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
        itemHolder.tv(R.id.lib_text_view)?.text = itemWheelText
    }
}