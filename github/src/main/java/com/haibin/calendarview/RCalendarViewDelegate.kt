package com.haibin.calendarview

import android.content.Context
import android.graphics.Color
import android.text.TextUtils
import android.util.AttributeSet
import com.angcyo.github.R
import com.angcyo.library.ex._color

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/07/05
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
class RCalendarViewDelegate(context: Context, attributeSet: AttributeSet? = null) :
    CalendarViewDelegate(context, attributeSet) {

    var isInEditMode = false

    var outRangeTextColor = Color.parseColor("#CBCBCB")
    var outRangeTextLunarColor = Color.parseColor("#CBCBCB")

    override fun init() {
        //2021-10-18

        try {
            mWeekBarClass = if (TextUtils.isEmpty(mWeekBarClassPath))
                WeekBar::class.java
            else
                Class.forName(mWeekBarClassPath)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            mYearViewClass = if (TextUtils.isEmpty(mYearViewClassPath))
                DefaultYearView::class.java
            else
                Class.forName(mYearViewClassPath)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            mMonthViewClass = if (TextUtils.isEmpty(mMonthViewClassPath))
                RMonthView::class.java
            else
                Class.forName(mMonthViewClassPath)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            mWeekViewClass = if (TextUtils.isEmpty(mWeekViewClassPath))
                DefaultWeekView::class.java
            else
                Class.forName(mWeekViewClassPath)
        } catch (e: Exception) {
            e.printStackTrace()
        }


        super.init()

        monthViewClass
        weekBarClass
        weekViewClass
        yearViewClass

        //calendarItemHeight = 100 * dpi
        //monthViewShowMode = MODE_ALL_MONTH
        //selectMode = SELECT_MODE_DEFAULT

        mCurrentMonthTextColor = _color(R.color.text_general_color)
        mCurMonthLunarTextColor = _color(R.color.text_sub_color)
        mSelectedTextColor = Color.WHITE
        mSelectedLunarTextColor = Color.WHITE

        mWeekTextColor = Color.WHITE
        if (!isInEditMode) {
            mCurDayTextColor = _color(R.color.colorAccent)
            mYearViewCurDayTextColor = _color(R.color.colorAccent)
            mWeekBackground = _color(R.color.colorAccent)
            mSelectedThemeColor = _color(R.color.colorAccent)
        }
    }

    fun getSelectedStartRangeCalendar(): Calendar? {
        return mSelectedStartRangeCalendar
    }

    fun getSelectedEndRangeCalendar(): Calendar? {
        return mSelectedEndRangeCalendar
    }
}