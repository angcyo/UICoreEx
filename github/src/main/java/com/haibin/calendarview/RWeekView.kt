package com.haibin.calendarview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.min

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/10/20
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class RWeekView(context: Context) : DefaultWeekView(context) {

    companion object {

        /**圆*/
        const val STYLE_CIRCLE = 1

        /**矩形*/
        const val STYLE_RECT = 2
    }

    /**显示阴历*/
    var showLunar = true

    /**选中时, 背景高亮的样式*/
    var showStyle = STYLE_CIRCLE

    override fun onDrawSelected(
        canvas: Canvas,
        calendar: Calendar,
        x: Int,
        hasScheme: Boolean
    ): Boolean {
        if (showStyle == RMonthView.STYLE_RECT) {
            return super.onDrawSelected(canvas, calendar, x, hasScheme)
        } else {
            mSelectedPaint.style = Paint.Style.FILL
            val cx = x + mItemWidth / 2
            val cy = mItemHeight / 2
            canvas.drawCircle(
                cx.toFloat(),
                cy.toFloat(),
                min(mItemWidth / 2, mItemHeight / 2).toFloat() - mPadding,
                mSelectedPaint
            )
        }
        return true
    }

    override fun onDrawText(
        canvas: Canvas,
        calendar: Calendar,
        x: Int,
        hasScheme: Boolean,
        isSelected: Boolean
    ) {
        val cx = x + mItemWidth / 2
        val cy = mItemHeight / 2
        val top = -mItemHeight / 6

        val textPaint = when {
            isSelected -> mSelectTextPaint //选中的笔
            calendar.isCurrentMonth -> mCurMonthTextPaint //当前月的笔
            else -> mOtherMonthTextPaint //其他月的笔
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
                mTextBaseLine + (mItemHeight / 10).toFloat(),
                lunarPaint
            )
        }
    }

}