package com.angcyo.acc2.app.component

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Color
import android.view.WindowManager
import com.angcyo.core.R
import com.angcyo.ilayer.ILayer
import com.angcyo.ilayer.container.OffsetPosition
import com.angcyo.ilayer.container.WindowContainer
import com.angcyo.library._contentHeight
import com.angcyo.library.app
import com.angcyo.library.ex.getColor
import com.angcyo.widget.base.colorAnimator
import com.angcyo.widget.progress.CircleLoadingView


/**
 * 无障碍悬浮窗 任务状态提示
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/10/19
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
@SuppressLint("StaticFieldLeak")
object AccWindowFullLayer : ILayer() {

    val _windowContainer = WindowContainer(app()).apply {
        defaultOffsetPosition = OffsetPosition(offsetX = 0f, offsetY = 0f)
        wmLayoutParams.apply {
            width = -1
            height = _contentHeight //_contentHeight //-1
            flags = wmLayoutParams.flags or
                    /*WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or*/
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        }
    }

    init {
        iLayerLayoutId = R.layout.lib_layout_accessibility_full_window
        autoRestorePosition = false
    }

    var _colorAnimator: ValueAnimator? = null

    fun show() {
        renderLayer = {
            //常亮
            itemView.keepScreenOn = true

            val circleLoadingView = v<CircleLoadingView>(R.id.progress_bar)

            if (AccWindow.updateProgress && !AccWindow.progressFlicker) {
                val duration = AccWindow.duration
                val _hideTime = AccWindow._hideTime
                if (duration > 0) {
                    var animDuration = duration
                    var fromProgress = 0

                    if (_hideTime in 1 until duration) {
                        fromProgress =
                            (_hideTime * 1f / duration * 100).toInt()
                        animDuration =
                            duration - _hideTime
                    }
                    circleLoadingView?.setProgress(
                        100,
                        fromProgress,
                        animDuration
                    )
                } else if (duration == 0L) {
                    circleLoadingView?.setProgress(0)
                }
            }

            //闪烁动画
            if (AccWindow.progressFlicker) {
                if (AccWindowMiniLayer._colorAnimator == null) {
                    val defColor = getColor(R.color.colorAccent)
                    AccWindowMiniLayer._colorAnimator = colorAnimator(
                        defColor,
                        Color.TRANSPARENT,
                        true,
                        duration = 1000,
                        onEnd = {
                            circleLoadingView?.loadingColor = defColor
                        }
                    ) { animator, color ->
                        //circleLoadingView?.progress = 100
                        circleLoadingView?.loadingColor = color
                    }
                }
            } else {
                AccWindowMiniLayer._colorAnimator?.cancel()
                AccWindowMiniLayer._colorAnimator = null
            }

            tv(R.id.text_view)?.apply {
                this.text = AccWindow.text
                setTextColor(AccWindow.textColor)
            }

            visible(R.id.summary_text_view, AccWindow.summary != null)
            tv(R.id.summary_text_view)?.apply {
                text = "正在执行:${AccWindow.summary}"
                setTextColor(AccWindow.summaryColor)
            }
            tv(R.id.top_text_view)?.text = AccWindow.fullTopText
            tv(R.id.title_text_view)?.text = AccWindow.fullTitleText

            //打开本机程序
            throttleClickItem {
                AccWindow.onLayerClickAction?.invoke()
            }

            //停止按钮
            visible(R.id.stop_button, !AccWindow.notTouch)
            throttleClick(R.id.stop_button) {
                AccWindow.onStopAction?.invoke()
            }

            //捕捉界面信息
            visible(
                R.id.catch_button,
                AccWindow.showCatchButton && !AccWindow.notTouch
            )
            throttleClick(R.id.catch_button) {
                AccWindow.onCatchAction?.invoke()
            }
        }

        show(_windowContainer)
    }

    fun hide() {
        hide(_windowContainer)
    }

    //仅显示浮窗
    fun _show() {
        if (_rootView != null && _rootView?.parent == null) {
            show(_windowContainer)
        }
    }

    /**修改[WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE]*/
    fun notTouchable(value: Boolean) {
        _windowContainer.notTouchable(value, this)
        update()
    }
}