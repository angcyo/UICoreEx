package com.angcyo.github.dialog

import android.app.Dialog
import android.content.Context
import com.angcyo.dialog.BaseDialogConfig
import com.angcyo.dialog.configBottomDialog
import com.angcyo.github.R
import com.angcyo.library.L
import com.angcyo.widget.DslViewHolder
import com.haibin.calendarview.*

/**
 * 日历选择对话框
 *
 * https://github.com/huanghaibin-dev/CalendarView
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/04/14
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
open class CalendarDialogConfig : BaseDialogConfig() {

    companion object {

        fun ymd(builder: StringBuilder, calendar: Calendar) {
            ymd(builder, calendar.year, calendar.month, calendar.day)
        }

        fun ymd(builder: StringBuilder, year: Int, month: Int, day: Int) {
            builder.apply {
                append(year)
                append("-")
                if (month < 10) {
                    append("0")
                }
                append(month)
                append("-")
                if (day < 10) {
                    append("0")
                }
                append(day)
            }
        }
    }

    /**默认选中的日历*/
    var calendarList = mutableListOf<Calendar>()

    /**最大的年月日*/
    var maxYear = 2200
    var maxYearMonth = 12
    var maxYearDay = -1

    /**最小的年月日*/
    var minYear = 1970
    var minYearMonth = 1
    var minYearDay = 1

    /**
     * 日历返回, 想要拿到日期 范围. 使用 list.first list.last就行
     * 返回 true, 则不会自动 调用 dismiss
     * @param calendarList 一天一天的集合
     * */
    var dialogResult: (dialog: Dialog, calendarList: List<Calendar>) -> Boolean =
        { _, _ ->
            false
        }

    init {
        dialogLayoutId = R.layout.lib_dialog_calendar_layout

        positiveButtonListener = { dialog, _ ->
            if (dialogResult.invoke(dialog, calendarList)) {

            } else {
                dialog.dismiss()
            }
        }
    }

    /**设置日期范围*/
    fun setCalendarRange(
        minYear: Int = today().year,
        maxYear: Int = today().year,
        minYearMonth: Int = 1,
        maxYearMonth: Int = 12,
        minYearDay: Int = 1,
        maxYearDay: Int = CalendarUtil.getMonthDaysCount(maxYear, maxYearMonth)
    ) {
        this.maxYear = maxYear
        this.minYear = minYear

        this.maxYearMonth = maxYearMonth
        this.minYearMonth = minYearMonth

        this.maxYearDay = maxYearDay
        this.minYearDay = minYearDay
    }

    //当前视图, 对应的日期
    private lateinit var calendar: Calendar

    override fun initDialogView(dialog: Dialog, dialogViewHolder: DslViewHolder) {
        super.initDialogView(dialog, dialogViewHolder)

        //当前时间提示控件
        val currentCalendarTipView = dialogViewHolder.tv(R.id.current_calendar_tip)

        val calendarView = dialogViewHolder.v<RCalendarView>(R.id.calendar_view)

        calendarView?.apply {

            val calendarViewDelegate = calendarView.getCalendarViewDelegate()
            calendar = calendarViewDelegate.createCurrentDate() ?: today()

            //监听月份左右滑动
            calendarView.setOnCalendarSelectListener(object :
                CalendarView.OnCalendarSelectListener {
                override fun onCalendarOutOfRange(calendar: Calendar) {
                    L.w("超范围:$calendar")
                }

                override fun onCalendarSelect(calendar: Calendar, isClick: Boolean) {
                    this@CalendarDialogConfig.calendar = calendar
                    currentCalendarTipView?.text = buildString {
                        append(calendar.year)
                        append("年")
                        append(calendar.month)
                        append("月")
                    }
                }
            })

            //监听日期点击选择
            calendarView.setOnCalendarRangeSelectListener(object :
                CalendarView.OnCalendarRangeSelectListener {
                override fun onCalendarSelectOutOfRange(calendar: Calendar) {
                    L.w("选择超范围:$calendar")
                }

                override fun onSelectOutOfRange(calendar: Calendar, isOutOfMinRange: Boolean) {
                    L.w("选择超范围:$calendar $isOutOfMinRange")
                }

                override fun onCalendarRangeSelect(calendar: Calendar, isEnd: Boolean) {

                    if (isEnd) {
                        //结束选中
                        if (calendarList.size > 1) {
                            calendarList[1] = calendar
                        } else {
                            calendarList.add(calendar)
                        }
                    } else {
                        //选中开始, 第一个选中
                        calendarList.clear()
                        //追加2次, 当只选择了开始开始时, 结束时间就是当天
                        calendarList.add(calendar)
                        calendarList.add(calendar)
                    }

                    checkCalendar(dialogViewHolder, calendarViewDelegate)
                }
            })

            //监听年视图可见变化
            calendarView.setOnYearViewChangeListener { isClose ->
                if (!isClose) {
                    currentCalendarTipView?.text = buildString {
                        append(calendar.year)
                        append("年")
                    }
                }
            }

            //监听年切换
            calendarView.setOnYearChangeListener {
                if (calendarView.isYearSelectLayoutVisible) {
                    currentCalendarTipView?.text = buildString {
                        append(it)
                        append("年")
                    }
                }
            }

            calendarView.setRange(
                minYear,
                minYearMonth,
                minYearDay,
                maxYear,
                maxYearMonth,
                maxYearDay
            )

            //切换年视图
            dialogViewHolder.click(R.id.current_calendar_tip) {
                if (calendarView.isYearSelectLayoutVisible) {
                    calendarView.closeYearSelectLayout()
                } else {
                    calendarView.showYearSelectLayout(calendar.year)
                }
            }

            //默认展示日历
            if (calendarList.isEmpty()) {
                //滚动到今天
                calendarView.scrollToCurrent()
            } else {
                if (calendarList.size < 2) {
                    calendarList.add(calendarList.first())
                }

                if (calendarList.size > 1) {
                    calendarView.getCalendarViewDelegate().mSelectedEndRangeCalendar =
                        calendarList[1]
                }

                calendarList.first().apply {
                    calendarView.getCalendarViewDelegate().mSelectedStartRangeCalendar = this
                    calendarView.scrollToCalendar(year, month, day)
                }
            }

            checkCalendar(dialogViewHolder, calendarViewDelegate)
        }
    }

    private fun checkCalendar(
        dialogViewHolder: DslViewHolder,
        calendarViewDelegate: RCalendarViewDelegate
    ) {
        dialogViewHolder.tv(R.id.selector_calendar_tip)?.text = buildString {
            val selectedStartRangeCalendar = calendarViewDelegate.getSelectedStartRangeCalendar()
            if (selectedStartRangeCalendar == null) {

            } else {
                selectedStartRangeCalendar.apply {
                    append("始:")
                    ymd(this@buildString, this)
                }
                (calendarViewDelegate.getSelectedEndRangeCalendar()
                    ?: selectedStartRangeCalendar).apply {
                    appendln()
                    append("止:")
                    ymd(this@buildString, this)
                }
            }
        }
        dialogViewHolder.enable(R.id.positive_button, calendarList.isNotEmpty())
    }
}


/**
 * 日历选择对话框
 * */
fun Context.calendarDialog(config: CalendarDialogConfig.() -> Unit): Dialog {
    return CalendarDialogConfig().run {
        configBottomDialog(this@calendarDialog)
        config()
        show()
    }
}
