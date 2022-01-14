package com.angcyo.acc2.app.component

import com.angcyo.acc2.app.AppAccPrint
import com.angcyo.acc2.app.app
import com.angcyo.acc2.app.http.Gitee
import com.angcyo.acc2.app.model.GiteeModel
import com.angcyo.acc2.bean.ActionBean
import com.angcyo.acc2.bean.TaskBean
import com.angcyo.acc2.control.AccControl
import com.angcyo.core.vmApp
import com.angcyo.http.base.fromJson
import com.angcyo.library.L
import com.angcyo.library.ex.ensureName
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
        accPrint = AppAccPrint(this)

        //浮窗控制支持
        TaskLayerControlListener.install(this)

        //表单请求支持
        accSchedule.accParse.formParse.formRequestListener = FormRequestListener()
    }

    /**acc控制器的状态*/
    fun controlState() = control._controlState

    fun start(
        taskBean: TaskBean?,
        enableActionString: String,
        disableActionString: String? = null,
        randomEnableActionString: String? = null,
    ): TaskBean? {
        return start(
            taskBean,
            enableActionString.toActionIdList(),
            disableActionString?.toActionIdList(),
            randomEnableActionString?.toActionIdList()
        )
    }

    /**根据指定的url, 启动任务*/
    fun start(taskUrl: String, startAction: (TaskBean?, Throwable?) -> Unit) {
        Gitee.getTask(taskUrl) { data, error ->
            val task = data?.init(control)
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
        randomEnableActionList: List<Long>? = null,
        force: Boolean = false
    ): TaskBean? {
        if (taskBean == null) {
            L.e("无任务需要启动.")
            return null
        }
        val bean = if (taskBean._init) {
            taskBean
        } else {
            taskBean.init(control)
        }
        if (!enableActionList.isNullOrEmpty() || !disableActionList.isNullOrEmpty()) {
            bean.actionList?.forEach {
                if (enableActionList?.contains(it.actionId) == true) {
                    it.enable = true
                }
                if (randomEnableActionList?.contains(it.actionId) == true) {
                    it.randomEnable = true
                }
                if (disableActionList?.contains(it.actionId) == true) {
                    it.enable = false
                }
            }
        }
        control.start(bean, force)
        return bean
    }

    /**从[assets]中, 获取json数据*/
    fun readAssetsTask(name: String): TaskBean? {
        return app().readAssets(name.jsonName())?.fromJson(TaskBean::class.java)
    }

    /**停止任务*/
    fun stop(reason: String? = null) {
        control.stop(reason ?: "主动停止")
    }
}

fun String.jsonName() = ensureName(".json")

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
fun TaskBean.init(control: AccControl? = Task.control): TaskBean {
    return vmApp<GiteeModel>().initTask(control, this)
}

fun ActionBean.init(): ActionBean {
    return vmApp<GiteeModel>().initAction(this)
}