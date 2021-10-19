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