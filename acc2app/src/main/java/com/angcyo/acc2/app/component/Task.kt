package com.angcyo.acc2.app.component

import com.angcyo.acc2.app.AppAccLog
import com.angcyo.acc2.app.app
import com.angcyo.acc2.app.http.Gitee
import com.angcyo.acc2.app.model.GiteeModel
import com.angcyo.acc2.bean.ActionBean
import com.angcyo.acc2.bean.TaskBean
import com.angcyo.acc2.control.AccControl
import com.angcyo.core.vmApp
import com.angcyo.http.base.fromJson
import com.angcyo.library.L
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

        //随机因子
        app().memoryConfigBean.defTimeRandomFactor?.let {
            accSchedule.accParse.defTimeRandomFactor = it
        }

        //日志
        accPrint = AppAccLog(this)

        TaskLayerControlListener.install(this)
    }

    fun start(
        taskBean: TaskBean?,
        enableActionString: String,
        disableActionString: String? = null,
        randomEnableActionString: String? = null,
    ) {
        start(
            taskBean,
            enableActionString.toActionIdList(),
            disableActionString?.toActionIdList(),
            randomEnableActionString?.toActionIdList()
        )
    }

    /**根据指定的url, 启动任务*/
    fun start(taskUrl: String, startAction: (TaskBean?, Throwable?) -> Unit) {
        Gitee.getTask(taskUrl) { data, error ->
            val task = data?.init()
            startAction(task, error)
            task?.let {
                start(it)
            }
        }
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