package com.haibin.calendarview

import android.graphics.Color
import java.util.*

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/10/18
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */

//<editor-fold desc="日历扩展方法">

/**今天的日历*/
fun today(): Calendar {
    val calendar = Calendar()
    val d = Date()
    calendar.year = CalendarUtil.getDate("yyyy", d)
    calendar.month = CalendarUtil.getDate("MM", d)
    calendar.day = CalendarUtil.getDate("dd", d)
    calendar.isCurrentDay = true
    return calendar
}

/**创建一个日历*/
fun createCalendar(year: Int, month: Int, day: Int): Calendar {
    val calendar = Calendar()
    calendar.year = year
    calendar.month = month
    calendar.day = day
    return calendar
}

//</editor-fold desc="日历扩展方法">

//<editor-fold desc="日历视图扩展方法">

var CalendarViewDelegate.outRangeTextColor: Int
    get() = if (this is RCalendarViewDelegate) {
        this.outRangeTextColor
    } else {
        Color.parseColor("#CBCBCB")
    }
    set(value) {
        if (this is RCalendarViewDelegate) {
            this.outRangeTextColor = value
        }
    }

var CalendarViewDelegate.outRangeTextLunarColor: Int
    get() = if (this is RCalendarViewDelegate) {
        this.outRangeTextLunarColor
    } else {
        Color.parseColor("#CBCBCB")
    }
    set(value) {
        if (this is RCalendarViewDelegate) {
            this.outRangeTextLunarColor = value
        }
    }

/**切换到[年]选择视图
 * [year] 哪一年*/
fun CalendarView.toYearSelect(year: Int = today().year) {
    showYearSelectLayout(year)
}

/**返回到今天*/
fun CalendarView.toToday(
    year: Int = today().year,
    month: Int = today().month,
    day: Int = today().day,
    smoothScroll: Boolean = false,
    invokeListener: Boolean = true
) {
    scrollToCalendar(year, month, day, smoothScroll, invokeListener)
}

/**滚动到上一个月*/
fun CalendarView.toPreMonth(smoothScroll: Boolean = true) {
    scrollToPre(smoothScroll)
}

/**滚动到下一个月*/
fun CalendarView.toNextMonth(smoothScroll: Boolean = true) {
    scrollToNext(smoothScroll)
}

/**添加一个事务标记*/
fun CalendarView.addSchemeDate(
    year: Int,
    month: Int,
    day: Int,
    text: String?,
    color: Int = 0,
    obj: Any? = null
) {
    val calendar = Calendar()
    calendar.year = year
    calendar.month = month
    calendar.day = day
    calendar.schemeColor = color //如果单独标记颜色、则会使用这个颜色
    calendar.scheme = text
    val scheme = Calendar.Scheme(color, text)
    scheme.obj = obj
    calendar.schemes = listOf(scheme)
    addSchemeDate(calendar)

    //setSchemeDate()   //设置标记
    //clearSchemeDate() //清理所有标记
}

/**https://github.com/huanghaibin-dev/CalendarView/blob/master/QUESTION_ZH.md*/
fun CalendarView.test() {
    //限制日期范围
    //setRange()

    //选中的日期
    selectedCalendar
    //选中的日期范围
    selectCalendarRange

    currentWeekCalendars

    minRangeCalendar
    maxRangeCalendar

    minSelectRange
    maxSelectRange

    setWeekStarWithMon() //设置星期一周起始
    setWeekStarWithSat() //设置星期六周起始
    setWeekStarWithSun() //设置星期日周起始

    setSelectSingleMode() //单选模式
    setSelectMultiMode() //多选模式
    setSelectRangeMode() //范围模式
    setSelectDefaultMode() //默认选择模式

    setAllMode() //月份显示所有
    setOnlyCurrentMode() //仅显示当前月
    setFixMode() //月份显示所有, 不撑开高度
}


//</editor-fold desc="日历视图扩展方法">

//<editor-fold desc="日历事件方法">

/**日历选择拦截事件监听, 在有选择模式的情况下才会生效
 * [CalendarViewDelegate.SELECT_MODE_SINGLE]
 * [CalendarViewDelegate.SELECT_MODE_RANGE]
 * [CalendarViewDelegate.SELECT_MODE_MULTI]*/
fun CalendarView.onCalendarInterceptListener(action: (calendar: Calendar, isClick: Boolean, isIntercept: Boolean) -> Boolean) {
    setOnCalendarInterceptListener(object : CalendarView.OnCalendarInterceptListener {

        //是否需要拦截当前日期的事件
        override fun onCalendarIntercept(calendar: Calendar): Boolean {
            return action(calendar, false, true)
        }

        //拦截后的处理
        override fun onCalendarInterceptClick(calendar: Calendar, isClick: Boolean) {
            action(calendar, isClick, false)
        }
    })
}

/**日历选择事件监听*/
fun CalendarView.onCalendarSelectListener(action: (calendar: Calendar, isClick: Boolean, isOutOfRange: Boolean) -> Unit) {
    setOnCalendarSelectListener(object : CalendarView.OnCalendarSelectListener {
        override fun onCalendarOutOfRange(calendar: Calendar) {
            action(calendar, false, true)
        }

        override fun onCalendarSelect(calendar: Calendar, isClick: Boolean) {
            action(calendar, isClick, false)
        }
    })
}

/**日历范围选择事件监听*/
fun CalendarView.onCalendarRangeSelectListener(action: (calendar: Calendar, isOutOfRange: Boolean, isOutOfMinRange: Boolean, isEnd: Boolean) -> Unit) {
    setOnCalendarRangeSelectListener(object : CalendarView.OnCalendarRangeSelectListener {
        override fun onCalendarSelectOutOfRange(calendar: Calendar) {
            action(calendar, true, false, false)
        }

        override fun onSelectOutOfRange(calendar: Calendar, isOutOfMinRange: Boolean) {
            action(calendar, false, isOutOfMinRange, false)
        }

        override fun onCalendarRangeSelect(calendar: Calendar, isEnd: Boolean) {
            action(calendar, false, false, isEnd)
        }
    })
}

/**日历多选选择事件监听*/
fun CalendarView.onCalendarMultiSelectListener(action: (calendar: Calendar, isOutOfRange: Boolean, maxSize: Int, curSize: Int) -> Unit) {
    setOnCalendarMultiSelectListener(object : CalendarView.OnCalendarMultiSelectListener {
        override fun onCalendarMultiSelectOutOfRange(calendar: Calendar) {
            action(calendar, true, -1, -1)
        }

        override fun onMultiSelectOutOfSize(calendar: Calendar, maxSize: Int) {
            action(calendar, false, maxSize, -1)
        }

        override fun onCalendarMultiSelect(calendar: Calendar, curSize: Int, maxSize: Int) {
            action(calendar, false, maxSize, curSize)
        }
    })
}

/**月视图改变监听*/
fun CalendarView.onMonthChangeListener(action: (year: Int, month: Int) -> Unit) {
    setOnMonthChangeListener { year, month -> action(year, month) }
}

fun CalendarView.onYearViewChangeListener(action: (isClose: Boolean) -> Unit) {
    setOnYearViewChangeListener { isClose -> action(isClose) }
}

fun CalendarView.onViewChangeListener(action: (isMonthView: Boolean) -> Unit) {
    setOnViewChangeListener { isMonthView -> action(isMonthView) }
}

/**周视图切换监听,
 * 回调的数据是当前显示7天的日期*/
fun CalendarView.onWeekChangeListener(action: (weekCalendars: List<Calendar>) -> Unit) {
    setOnWeekChangeListener { weekCalendars -> action(weekCalendars) }
}

//</editor-fold desc="日历事件方法">