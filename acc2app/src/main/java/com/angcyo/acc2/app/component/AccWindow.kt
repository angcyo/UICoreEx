package com.angcyo.acc2.app.component

import android.graphics.Color
import android.os.Build
import android.widget.Toast
import com.angcyo.acc2.app.saveAccLog
import com.angcyo.acc2.control.isControlPause
import com.angcyo.acc2.control.isControlStart
import com.angcyo.acc2.control.toControlStateStr
import com.angcyo.acc2.core.AccNodeLog
import com.angcyo.acc2.core.AccPermission
import com.angcyo.core.R
import com.angcyo.core.component.addGistFile
import com.angcyo.core.component.pushToGist
import com.angcyo.http.rx.doBack
import com.angcyo.http.rx.doMain
import com.angcyo.library.*
import com.angcyo.library.component.MainExecutor
import com.angcyo.library.ex.*
import com.angcyo.library.utils.Device
import com.angcyo.widget.span.span


/**
 * 无障碍悬浮窗 任务状态提示
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/07/03
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
object AccWindow {

    //<editor-fold desc="可操作属性">

    /**切换全屏布局*/
    var fullscreenLayer: Boolean = false
        set(value) {
            field = value
            if (value) {
                AccWindowMiniLayer.hide()
                if (!_isNeedHide()) {
                    AccWindowFullLayer._show()
                }
            } else {
                AccWindowFullLayer.hide()
                if (!_isNeedHide()) {
                    AccWindowMiniLayer._show()
                }
            }
        }

    /**去掉touch事件*/
    var notTouch: Boolean = false
        set(value) {
            field = value
            AccWindowMiniLayer.notTouchable(value)
            AccWindowFullLayer.notTouchable(value)
        }

    /**是否更新进度条*/
    var updateProgress: Boolean = false

    /**进度闪烁, 颜色渐变*/
    var progressFlicker: Boolean = false

    /**全屏时, 顶部文本*/
    var fullTopText: CharSequence? = span {
        append(getAppName()) {
            foregroundColor = _color(R.color.colorPrimary)
            style = android.graphics.Typeface.BOLD
        }
        appendln()
        append("全自动操作中...")
        appendln()
        append("!...请勿手动操作...!") {
            foregroundColor = Color.RED
            style = android.graphics.Typeface.BOLD
        }
    }

    var fullTitleText: CharSequence? = span {
        append(Task.control._taskBean?.title.or())
    }

    /**步骤进度提示文本
     * 小屏时: 显示在进度中心的文本
     * 全屏时: 进度下面的标题
     * */
    var text: CharSequence? = null

    /**颜色*/
    var textColor: Int = Color.WHITE
        set(value) {
            field = value
            update()
        }

    /**描述概要文本
     * 小屏时: 显示在进度下面的文本
     * 全屏时: 进度下面的标题下面的操作提示
     * */
    var summary: CharSequence? = null

    /**颜色*/
    var summaryColor: Int = Color.WHITE
        get() {
            return if (notTouch) {
                //可穿透事件时的提示颜色
                Color.RED
            } else {
                field
            }
        }
        set(value) {
            field = value
            update()
        }

    /**描述文本, 暂无作用*/
    var des: CharSequence? = null

    /**转圈时长, 毫秒. -1 保持原来的进度; 0 清空进度; 其他 进度动画时长*/
    var duration: Long = -1
        set(value) {
            field = value
            updateProgress = true
        }

    var showCatchButton: Boolean = isDebug()
        set(value) {
            val old = field
            field = value
            if (old != value && value) {
                AccWindowFullLayer.update()
                AccWindowMiniLayer.update()
            }
        }

    var showTestButton: Boolean = isDebugType()
        set(value) {
            val old = field
            field = value
            if (old != value && value) {
                AccWindowFullLayer.update()
                AccWindowMiniLayer.update()
            }
        }

    //</editor-fold desc="可操作属性">

    /**浮窗需要隐藏到什么时间, 13位时间戳*/
    var _hideToTime: Long = -1

    //隐藏时长
    var _hideTime: Long = -1

    /**还需要隐藏的次数, >0生效*/
    var _hideToCount: Long = -1

    //显示浮窗
    val _showRunnable: Runnable? = Runnable {
        show()
    }

    //<editor-fold desc="回调">

    /**触发的保存窗口日志*/
    var onSaveWindowLog: ((log: String) -> Unit)? = null

    /**抓取界面日志*/
    var onCatchAction: Action? = {
        if (!AccPermission.haveAccessibilityService(app())) {
            toastQQ("权限未开启")
        } else {
            doBack {
                AccNodeLog().apply {
                    LTime.tick()
                    logMinWindowInfo = true
                    logWindowNode = false
                    val log = "${getAccessibilityWindowLog()}\n耗时:${LTime.time()}"
                    L.i(log)
                    toastQQ(log) {
                        duration = Toast.LENGTH_LONG
                    }
                    reset()
                    getAccessibilityWindowLog().apply {
                        val log = toString()
                        val logPath = log.saveAccLog()
                        /*//直接分享文件
                        logPath?.file()?.shareFile()*/
                        pushToGist("${nowTimeString()}/${Build.MODEL}/catch") {
                            val info = buildString {
                                //屏幕信息, 设备信息
                                app().let {
                                    Device.screenInfo(it, this)
                                    appendln()
                                    Device.deviceInfo(it, this)
                                }
                            }
                            addGistFile("device info", info)
                            addGistFile("node info", log)
                        }
                        onSaveWindowLog?.invoke(log)
                    }
                }
            }
        }
    }

    val _defaultClickAction: Action = {
        app().openApp()
    }

    /**点击浮窗的回调*/
    var onLayerClickAction: Action? = _defaultClickAction

    var onStopAction: Action? = null

    /**暂停状态切换*/
    var onPauseAction: (() -> Unit)? = {
        if (Task.control.isControlPause) {
            Task.control.resume(false)
        } else if (Task.control.isControlStart) {
            Task.control.pause()
        } else {
            L.i("无法处理的状态[${Task.control._controlState.toControlStateStr()}]")
        }
    }

    //</editor-fold desc="回调">

    /**清空默认*/
    fun reset() {
        text = null
        summary = null
        des = null
        duration = 0
        textColor = Color.WHITE
        summaryColor = Color.WHITE
        updateProgress = false
        progressFlicker = false
    }

    fun show(action: AccWindow.() -> Unit = {}) {
        this.action()
        if (!_isNeedHide()) {
            _showRunnable?.let {
                MainExecutor.handler.removeCallbacks(it)
            }

            if (fullscreenLayer) {
                AccWindowFullLayer.show()
            } else {
                AccWindowMiniLayer.show()
            }

            //clear
            updateProgress = false
            progressFlicker = false
            _hideTime = -1
        }
    }

    /**标准的进度显示*/
    fun showProgress(
        text: String?,
        summary: String?,
        duration: Long,
        action: AccWindow.() -> Unit = {}
    ) {
        reset()
        show {
            AccWindow.text = text
            AccWindow.summary = summary
            AccWindow.duration = duration
            action()
        }
    }

    /**显示状态*/
    fun showState(text: String?, textColor: Int = Color.WHITE, action: AccWindow.() -> Unit = {}) {
        reset()
        show {
            AccWindow.text = text
            AccWindow.textColor = textColor
            action()
        }
    }

    /**进度闪烁*/
    fun flicker() {
        doMain {
            progressFlicker = true
            updateProgress = false
            update()
        }
    }

    fun update() {
        AccWindowFullLayer.update()
        AccWindowMiniLayer.update()
    }

    fun hide() {
        AccWindowMiniLayer.hide()
        AccWindowFullLayer.hide()
    }

    fun hideAuto() {
        AccWindowMiniLayer.hide()
        if (notTouch) {
            //如果已经无法接收手势, 则不隐藏浮窗, 多此一举
        } else {
            AccWindowFullLayer.hide()
        }
    }

    fun _isNeedHide(): Boolean {
        return if (_hideToCount > 0) {
            //需要隐藏
            true
        } else {
            val nowTime: Long = nowTime()
            nowTime <= _hideToTime
        }
    }

    /**异常多少次的显示请求*/
    fun hideCount(count: Long) {
        _hideToCount = count
        if (count > 0) {
            hideAuto()
        }
    }

    fun hideCountDown() {
        _hideToCount--
    }

    /**浮窗隐藏多长时间*/
    fun hideTime(time: Long) {
        _hideToCount = -1

        _showRunnable?.let {
            MainExecutor.handler.removeCallbacks(it)
        }

        if (time > 0) {
            hideAuto()

            _hideTime = time
            _hideToTime = time + nowTime()

            _showRunnable?.let {
                MainExecutor.handler.postDelayed(it, time)
            }
        } else {
            _hideTime = -1
            _hideToTime = -1
        }
    }
}