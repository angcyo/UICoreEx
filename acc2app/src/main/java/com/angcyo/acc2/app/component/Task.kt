package com.angcyo.acc2.app.component

import android.graphics.Color
import com.angcyo.acc2.action.FullscreenAction
import com.angcyo.acc2.action.HideWindowAction
import com.angcyo.acc2.action.NotTouchableAction
import com.angcyo.acc2.app.AppAccLog
import com.angcyo.acc2.app.model.GiteeModel
import com.angcyo.acc2.app.model.TaskModel
import com.angcyo.acc2.bean.ActionBean
import com.angcyo.acc2.bean.TaskBean
import com.angcyo.acc2.control.AccControl
import com.angcyo.acc2.control.ControlListener
import com.angcyo.core.vmApp
import com.angcyo.http.base.fromJson
import com.angcyo.http.rx.doMain
import com.angcyo.library.L
import com.angcyo.library.app
import com.angcyo.library.ex.isDebugType
import com.angcyo.library.ex.openApp
import com.angcyo.library.ex.readAssets

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/02/01
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
object Task {

    val control = AccControl().apply {

        //日志
        accPrint = AppAccLog(this)

        //Action回调赋值
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
        //controlListenerList.add(OperateControlListener(this))
        controlListenerList.add(object : ControlListener() {

            override fun onControlStart(taskBean: TaskBean) {
                super.onControlStart(taskBean)
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
                    AccWindow.update()
                }
            }

            override fun onControlEnd(taskBean: TaskBean, state: Int, reason: String) {
                super.onControlEnd(taskBean, state, reason)
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
        })
    }

    fun start(
        taskBean: TaskBean?,
        enableActionString: String? = null,
        disableActionString: String? = null,
        randomEnableActionString: String? = null,
    ) {
        start(
            taskBean,
            enableActionString?.toActionIdList(),
            disableActionString?.toActionIdList(),
            randomEnableActionString?.toActionIdList()
        )
    }

    /**启动任务[taskBean]
     * [enableActionList]需要激活的[ActionBean]
     * [disableActionList]需要禁用的[ActionBean]
     * [randomEnableActionList]需要随机激活的[ActionBean]
     * */
    fun start(
        taskBean: TaskBean?,
        enableActionList: List<Long>? = null,
        disableActionList: List<Long>? = null,
        randomEnableActionList: List<Long>? = null
    ) {
        if (taskBean == null) {
            L.e("无任务需要启动.")
            return
        }
        if (!enableActionList.isNullOrEmpty() || !disableActionList.isNullOrEmpty()) {
            taskBean.actionList?.forEach {
                if (enableActionList?.contains(it.actionId) == true) {
                    it.enable = true
                }
                if (disableActionList?.contains(it.actionId) == true) {
                    it.enable = false
                }
                if (randomEnableActionList?.contains(it.actionId) == true) {
                    it.randomEnable = true
                }
            }
        }
        control.start(taskBean, false)
    }

    /**从[assets]中, 获取json数据*/
    fun readAssetsTask(name: String): TaskBean? {
        return app().readAssets(name.jsonName())?.fromJson(TaskBean::class.java)
    }
}

fun String.jsonName() = if (endsWith(".json")) this else "$this.json"

/**to id*/
fun String.toActionIdList(): List<Long> {
    val actionIdList = mutableListOf<Long>()
    split(";").forEach { actionIdStr ->
        actionIdStr.toLongOrNull()
            ?.let { actionId -> actionIdList.add(actionId) }
    }
    return actionIdList
}

/**初始化任务*/
fun TaskBean.init(): TaskBean {
    return vmApp<GiteeModel>().initTask(this)
}