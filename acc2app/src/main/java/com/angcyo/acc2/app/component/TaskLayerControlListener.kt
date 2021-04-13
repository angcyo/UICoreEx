package com.angcyo.acc2.app.component

import android.graphics.Color
import com.angcyo.acc2.action.FullscreenAction
import com.angcyo.acc2.action.HideWindowAction
import com.angcyo.acc2.action.NotTouchableAction
import com.angcyo.acc2.app.model.TaskModel
import com.angcyo.acc2.bean.TaskBean
import com.angcyo.acc2.control.AccControl
import com.angcyo.acc2.control.ControlListener
import com.angcyo.acc2.control.isControlStart
import com.angcyo.core.vmApp
import com.angcyo.http.rx.doMain
import com.angcyo.library.app
import com.angcyo.library.ex.isDebugType
import com.angcyo.library.ex.openApp

/**
 * 任务状态, 浮窗控制监听
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/03/11
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class TaskLayerControlListener : ControlListener() {

    companion object {
        fun install(control: AccControl) {
            control.apply {
                accSchedule.accParse.handleParse.registerActionList.forEach {
                    when (it) {
                        is FullscreenAction -> it.fullscreenAction = {
                            AccWindow.fullscreenLayer = it
                            true
                        }
                        is NotTouchableAction -> it.notTouchableAction = {
                            AccWindow.notTouch = it
                            true
                        }
                        is HideWindowAction -> it.hideWindowAction = { time, count ->
                            when {
                                time > 0 -> AccWindow.hideTime(time)
                                count > 0 -> AccWindow.hideCount(time)
                                else -> AccWindow.hide()
                            }
                            true
                        }
                    }
                }

                //事件监听
                controlListenerList.add(TaskLayerControlListener())
            }
        }
    }

    override fun onControlStart(control: AccControl, taskBean: TaskBean) {
        super.onControlStart(control, taskBean)
        doMain {
            vmApp<TaskModel>().taskData.value = taskBean
            if (taskBean.fullscreen) {
                //全屏浮窗
                AccWindow.fullscreenLayer = true
            }
            if (taskBean.notTouchable) {
                //浮窗无手势
                AccWindow.notTouch = taskBean.notTouchable
            }
            AccWindow.showState("Ready")
        }
    }

    override fun onControlStateChanged(control: AccControl, oldState: Int, newState: Int) {
        super.onControlStateChanged(control, oldState, newState)
        doMain {
            vmApp<TaskModel>().taskStateData.value = newState
            if (!control.isControlStart) {
                doMain(false) {
                    AccWindow.show()
                }
            } else {
                AccWindow.update()
            }
        }
    }

    override fun onControlEnd(
        control: AccControl,
        taskBean: TaskBean,
        state: Int,
        reason: String?
    ) {
        super.onControlEnd(control, taskBean, state, reason)
        doMain {
            vmApp<TaskModel>().taskData.value = null
            if (taskBean.finishToApp) {
                //回到主程序
                app().openApp()
            }
            if (isDebugType()) {
                when (state) {
                    AccControl.CONTROL_STATE_FINISH -> AccWindow.showState("^..^")
                    AccControl.CONTROL_STATE_STOP -> AccWindow.showState("Stop")
                    else -> AccWindow.showState("Err", Color.RED)
                }
            } else {
                AccWindow.hide()
            }
        }
    }
}