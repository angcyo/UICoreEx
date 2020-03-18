package com.angcyo.github.widget.recycler

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.angcyo.github.R
import com.angcyo.widget.recycler.DslRecyclerView
import com.leochuan.AutoPlaySnapHelper

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/18
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
open class LoopRecyclerView(context: Context, attributeSet: AttributeSet? = null) :
    DslRecyclerView(context, attributeSet) {

    var autoStartLoop: Boolean = true

    var loopSnapHelper: LoopSnapHelper =
        LoopSnapHelper(AutoPlaySnapHelper.TIME_INTERVAL, AutoPlaySnapHelper.RIGHT)

    init {
        val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.LoopRecyclerView)
        val timeInterval = typedArray.getInt(
            R.styleable.LoopRecyclerView_r_loop_interval,
            AutoPlaySnapHelper.TIME_INTERVAL
        )
        val direction =
            typedArray.getInt(
                R.styleable.LoopRecyclerView_r_loop_direction,
                AutoPlaySnapHelper.RIGHT
            )
        autoStartLoop =
            typedArray.getBoolean(R.styleable.LoopRecyclerView_r_auto_start, autoStartLoop)
        typedArray.recycle()

        loopSnapHelper.setTimeInterval(timeInterval)
        loopSnapHelper.setDirection(direction)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        _startInner()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pause()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == View.VISIBLE) {
            _startInner()
        } else {
            pause()
        }
    }

    override fun setLayoutManager(layout: LayoutManager?) {
        super.setLayoutManager(layout)

        if (!isInEditMode) {
            //如果在super的构造方法里面调用了setLayoutManager, 此时[loopSnapHelper]还未初始化, 就会报空指针
            loopSnapHelper?.attachToRecyclerView(this) //会自动开启无线循环
            _startInner()
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val result = super.dispatchTouchEvent(ev)
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> pause()
            MotionEvent.ACTION_UP -> _startInner()
        }
        return result
    }

    fun _startInner() {
        if (autoStartLoop) {
            start()
        }
    }

    open fun start() {
        loopSnapHelper.start()
    }

    open fun pause() {
        loopSnapHelper.pause()
    }
}
