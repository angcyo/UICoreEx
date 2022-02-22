package com.angcyo.acc2.app.component

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Point
import android.graphics.RectF
import android.view.ViewConfiguration
import android.view.WindowManager
import com.angcyo.acc2.control.isControlPause
import com.angcyo.acc2.control.isControlStart
import com.angcyo.acc2.core.AccPermission
import com.angcyo.acc2.core.BaseAccService
import com.angcyo.acc2.core.click
import com.angcyo.acc2.core.double
import com.angcyo.core.R
import com.angcyo.http.rx.doBack
import com.angcyo.ilayer.ILayer
import com.angcyo.ilayer.container.DragRectFConstraint
import com.angcyo.ilayer.container.IContainer
import com.angcyo.ilayer.container.WindowContainer
import com.angcyo.library.*
import com.angcyo.library.ex.getColor
import com.angcyo.library.ex.isDebug
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.colorAnimator
import com.angcyo.widget.progress.CircleLoadingView


/**
 * 无障碍悬浮窗 任务状态提示
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/07/03
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */

@SuppressLint("StaticFieldLeak")
object AccWindowMiniLayer : ILayer() {

    val _windowContainer = WindowContainer(app()).apply {
        wmLayoutParams.flags = wmLayoutParams.flags or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
    }

    init {
        iLayerLayoutId = R.layout.lib_layout_accessibility_window
        enableDrag = true
        showCancelLayer = true
        dragContainer = DragRectFConstraint(
            RectF(
                0f,
                _statusBarHeight * 1f / _screenHeight,
                0f,
                0.0000001f
            )
        )
    }

    var _colorAnimator: ValueAnimator? = null

    /**[text] 需要提示的文本
     * [summary] 描述文本
     * [duration] 转圈时长, 毫秒. -1 保持原来的进度; 0 清空进度; 其他 进度动画时长*/
    fun show() {
        renderLayer = {
            //常亮
            itemView.keepScreenOn = true

            val circleLoadingView = v<CircleLoadingView>(R.id.progress_bar)

            if (AccWindow.updateProgress && !AccWindow.progressFlicker) {
                val duration = AccWindow.duration
                val _hideTime = AccWindow._hideTime
                circleLoadingView?.isIndeterminate = false
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
                if (_colorAnimator == null) {
                    val defColor = getColor(R.color.colorAccent)
                    _colorAnimator = colorAnimator(
                        defColor,
                        Color.TRANSPARENT,
                        true,
                        duration = 1000,
                        onEnd = {
                            circleLoadingView?.loadingColor = defColor
                        }
                    ) { animator, color ->
                        circleLoadingView?.progress = 100
                        circleLoadingView?.loadingColor = color
                    }
                }
            } else {
                _colorAnimator?.cancel()
                _colorAnimator = null
            }

            tv(R.id.text_view)?.apply {
                this.text = AccWindow.text
                setTextColor(AccWindow.textColor)
            }

            //pause
            tv(R.id.pause_button)?.apply {
                visible(this)
                visible(R.id.prev_button, isDebug())
                this.text = when {
                    Task.control.isControlPause -> "继续"
                    Task.control.isControlStart -> "暂停"
                    else -> {
                        gone(this)
                        gone(R.id.prev_button)
                        "..."
                    }
                }
            }

            visible(R.id.catch_button, AccWindow.showCatchButton)
            visible(R.id.fullscreen_button, AccWindow.showCatchButton)

            visible(R.id.summary_text_view, AccWindow.summary != null)

            tv(R.id.summary_text_view)?.apply {
                text = AccWindow.summary
                setTextColor(AccWindow.summaryColor)
            }

            //暂停状态切换
            throttleClick(R.id.pause_button) {
                AccWindow.onPauseAction?.invoke()
            }

            //上一个步骤
            throttleClick(R.id.prev_button) {
                Task.control.accSchedule.prev()
            }

            //打开本机程序
            throttleClickItem {
                AccWindow.onLayerClickAction?.invoke()
            }

            //切换至全屏
            throttleClick(R.id.fullscreen_button) {
                AccWindow.fullscreenLayer = true
                AccWindow.show()

                AccWindow.onStopAction = {
                    AccWindow.fullscreenLayer = false
                    //AccessibilityWindow.show()
                }
            }

            //捕捉界面信息
            throttleClick(R.id.catch_button) {
                AccWindow.onCatchAction?.invoke()
            }

            //测试按钮
            visible(R.id.double_button, AccWindow.showTestButton)
            throttleClick(R.id.double_button) {
                doBack {
                    L.w(
                        "双击:${
                            BaseAccService.lastService?.gesture?.double(
                                0.5f * _screenWidth,
                                0.5f * _screenHeight
                            ) { gestureDescription, dispatched, canceled ->
                                //no op
                            }
                        } ${ViewConfiguration.getDoubleTapTimeout()}"
                    )
                }
            }

            visible(R.id.click_button, AccWindow.showTestButton)
            throttleClick(R.id.click_button) {
                doBack {
                    L.w(
                        "点击:${
                            BaseAccService.lastService?.gesture?.click(
                                0.5f * _screenWidth,
                                0.5f * _screenHeight
                            )
                        }"
                    )
                }
            }

            visible(R.id.test_button, AccWindow.showTestButton)
            throttleClick(R.id.test_button) {
                BaseAccService.lastService?.pressLocation(
                    Point(
                        _screenWidth / 2,
                        _screenHeight / 2
                    )
                )
                AccPermission.getEnabledAccessibilityServiceList()
                AccPermission.getEnabledAccessibilityGesturesAppList()

                //TouchTipLayer.showTouch(0.2f, 0f)

                //测试手势线
                //TouchTipLayer.showTouch(0.9162f, 0.9762f)   //1.
                //AccTouchTipLayer.showTouch(0.9162f, 0.9562f)   //2.
                //TouchTipLayer.showTouch(0.9162f, 0.9362f)   //3.

                //TouchTipLayer.showMove(0.5f, 0.5f, 0.5f, 0.3f)
                //TouchTipLayer.showMove(0.3f, 0.5f, 0.5f, 0.5f)

                /*AccTouchTipLayer.accTouchTipLayer.showRect(
                    listOf(
                        Rect(
                            100 * dpi,
                            100 * dpi,
                            200 * dpi,
                            400 * dpi
                        )
                    )
                )*/

                //发送键盘
                /*doBack {
                    try {
                        val inst = Instrumentation()
                        inst.sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }*/

                /*//测试手势点击 "touch:0.9192,0.9842"
                    val p1 = PointF(_screenWidth * 0.9192f, _screenHeight * 0.9842f)
                    BaseAccessibilityService.lastService?.gesture?.click(p1.x, p1.y)
                    L.w(p1)*/

                /*//测试url直接打开抖音
                 val list = listOf(
                     "https://v.douyin.com/JBrY5g9/", //很久的直播 [直播已结束]
                     "https://v.douyin.com/JBxoeKL", //直播 [打开看看]
                     "https://v.douyin.com/J6mdAPq/", //视频
                     "https://v.kuaishou.com/8vnjyY" //快手视频
                 )
                 val url = list[0]

                 Intent().apply {
                     setPackage("com.ss.android.ugc.aweme")
                     //setPackage("com.smile.gifmaker")
                     data = Uri.parse(url)
                     baseConfig(it.context)

                     try {
                         it.context.startActivity(this)
                     } catch (e: Exception) {
                         e.printStackTrace()
                     }
                 }*/

                /*val imm = context.getSystemService(
                    Context.INPUT_METHOD_SERVICE
                ) as InputMethodManager

                doBack {
                    LTime.tick()
                    try {
                        val inst = Instrumentation()
                        inst.sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER)
                    } catch (e: Exception) {
                        L.e("Exception when sendKeyDownUpSync $e")
                    }
                    L.i(LTime.time())
                }

                try {
                    val keyCommand = "input keyevent " + KeyEvent.KEYCODE_ENTER
                    val runtime = Runtime.getRuntime()
                    val proc = runtime.exec(keyCommand)
                    //L.i(proc.waitFor(), " ", proc.exitValue())
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                doBack {
                    try {
                        val keyCommand = "input keyevent " + KeyEvent.KEYCODE_ENTER
                        val runtime = Runtime.getRuntime()
                        val proc = runtime.exec(keyCommand)
                        //L.i(proc.waitFor(), " ", proc.exitValue())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val inputConnection = BaseInputConnection(it, true)
                L.e(inputConnection.performEditorAction(EditorInfo.IME_ACTION_SEARCH))*/

                //发放免费时长
                /*bmobSave(FreeTimeBmob().apply {
                    freeStartTime = nowTime() - day(1)
                    freeEndTime = nowTime() + day(1)
                    debug = true
                }) {
                    saveAction = { objectId, ex ->
                        objectId?.let {
                            toastWX("成功$objectId")
                        }
                        ex?.let {
                            toastWX("失败${it.message}")
                        }
                    }
                }*/
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

    override fun onDestroy(fromContainer: IContainer, viewHolder: DslViewHolder) {
        super.onDestroy(fromContainer, viewHolder)
        if (Task.control.isControlStart) {
            //
        }
    }
}