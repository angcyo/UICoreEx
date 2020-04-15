package com.haibin.calendarview

import android.content.Context
import android.graphics.Canvas
import kotlin.math.min

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/07/04
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */

open class RMonthView(context: Context) : MonthView(context) {

    /**显示阴历*/
    var showLunar = true

    override fun initPaint() {
        super.initPaint()
        mCurMonthTextPaint
    }

    override fun onDrawSelected(
        canvas: Canvas,
        calendar: Calendar,
        x: Int,
        y: Int,
        hasScheme: Boolean
    ): Boolean {
        val cx = x + mItemWidth / 2
        val cy = y + mItemHeight / 2
        canvas.drawCircle(
            cx.toFloat(),
            cy.toFloat(),
            min(mItemWidth / 2, mItemHeight / 2).toFloat(),
            mSelectedPaint
        )
        return true
    }

    override fun onDrawScheme(canvas: Canvas, calendar: Calendar, x: Int, y: Int) {

    }

    override fun onDrawText(
        canvas: Canvas,
        calendar: Calendar,
        x: Int,
        y: Int,
        hasScheme: Boolean,
        isSelected: Boolean
    ) {
        val cx = x + mItemWidth / 2
        val cy = y + mItemHeight / 2
        val top = y - mItemHeight / 6

        val textPaint = when {
            isSelected -> mSelectTextPaint
            calendar.isCurrentMonth -> mCurMonthTextPaint
            else -> mOtherMonthTextPaint
        }

        //阳历
        canvas.drawText(calendar.day.toString(), cx.toFloat(), mTextBaseLine + top, textPaint)

        //阴历
        if (showLunar) {
            val lunarPaint = when {
                isSelected -> mSelectedLunarTextPaint
                calendar.isCurrentMonth -> mCurMonthLunarTextPaint
                else -> mOtherMonthLunarTextPaint
            }

            canvas.drawText(
                calendar.lunar,
                cx.toFloat(),
                mTextBaseLine + y.toFloat() + (mItemHeight / 10).toFloat(),
                lunarPaint
            )
        }
    }
}