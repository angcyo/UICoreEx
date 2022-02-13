package com.angcyo.acc2.app.component

import com.angcyo.acc2.action.BaseTouchAction
import com.angcyo.acc2.app.memoryConfig
import com.angcyo.acc2.bean.TaskBean
import com.angcyo.acc2.control.AccControl
import com.angcyo.acc2.control.ControlListener
import com.angcyo.core.component.IObserver
import com.angcyo.core.component.VolumeObserver

/**
 * Acc任务监听
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2022/01/20
 * Copyright (c) 2020 angcyo. All rights reserved.
 */
class AccTaskControlListener : ControlListener {

    val volumeObserver = object : IObserver {
        override fun onChange(type: Int, from: Int, value: Int) {
            if (value != from) {
                //音量变小
                if (memoryConfig().pauseOnVolumeDown) {
                    Task.pause()
                }
            }
        }
    }

    override fun onControlStart(control: AccControl, taskBean: TaskBean) {
        super.onControlStart(control, taskBean)
        control.accSchedule.accParse.handleParse.registerActionList.forEach {
            if (it is BaseTouchAction) {
                val memoryConfig = memoryConfig()
                it.gestureStartTime = memoryConfig.gestureStartTime ?: it.gestureStartTime
                it.gestureDuration = memoryConfig.gestureDuration ?: it.gestureDuration
                it.gestureMoveDuration = memoryConfig.gestureMoveDuration ?: it.gestureMoveDuration
                it.gestureFlingDuration =
                    memoryConfig.gestureFlingDuration ?: it.gestureFlingDuration
            }
        }
    }

    override fun onControlStateChanged(control: AccControl, oldState: Int, newState: Int) {
        super.onControlStateChanged(control, oldState, newState)
        if (memoryConfig().pauseOnVolumeDown) {
            if (newState == AccControl.CONTROL_STATE_RUNNING) {
                //开始运行, 监听音量变化
                VolumeObserver.observe(volumeObserver)
                VolumeObserver.startListener()
            } else {
                VolumeObserver.removeObserve(volumeObserver)
                VolumeObserver.stopListener()
            }
        }
    }

}