package com.angcyo.acc2.app.model

import com.angcyo.acc2.app.http.bean.FunctionBean
import com.angcyo.acc2.app.http.bean.MessageBean
import com.angcyo.acc2.bean.*
import com.angcyo.core.lifecycle.LifecycleViewModel
import com.angcyo.http.base.fromJson
import com.angcyo.http.base.toJson
import com.angcyo.library.L
import com.angcyo.library.ex.isDebug
import com.angcyo.library.isMain
import com.angcyo.viewmodel.vmData
import com.angcyo.viewmodel.vmDataNull

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/02/03
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class GiteeModel : LifecycleViewModel() {

    /**存储功能列表*/
    val allFunctionData = vmData(listOf<FunctionBean>())

    val allActionData = vmData(listOf<ActionBean>())

    val allBackActionData = vmData(listOf<ActionBean>())

    val allCheckData = vmData(listOf<CheckBean>())
    val allAssetCheckData = vmData(listOf<CheckBean>())

    /**所有在线任务*/
    val allTaskData = vmData(listOf<TaskBean>())

    /**新消息提醒*/
    val messageData = vmDataNull<MessageBean>(null)

    /**获取指定类型的任务[TaskBean]*/
    fun findTask(type: String?): TaskBean? {
        for (task in allTaskData.value ?: emptyList()) {
            if (task.type == type) {
                //深拷贝
                return task
            }
        }
        L.w("未找到任务类型:$type")
        return null
    }

    /**获取[CheckBean]*/
    fun findCheck(checkId: Long): CheckBean? {
        //1
        for (check in allCheckData.value ?: emptyList()) {
            if (check.checkId == checkId) {
                //深拷贝
                return check.toJson().fromJson()
            }
        }
        //2
        for (check in allAssetCheckData.value ?: emptyList()) {
            if (check.checkId == checkId) {
                //深拷贝
                return check.toJson().fromJson()
            }
        }
        L.w("未找到CheckId:$checkId")
        return null
    }

    /**[com.angcyo.acc.market.bean.FunctionBean.FUNCTION_AFTER_PLAY]
     * */
    fun findFunction(type: Int): FunctionBean? {
        return allFunctionData.value?.find { it.type == type }
    }

    fun functionList() = allFunctionData.value?.filter { it.enable }

    //<editor-fold desc="初始化">

    fun initTask(bean: TaskBean): TaskBean {
        return bean.toJson().fromJson<TaskBean>()!!.init()
    }

    /**初始化[TaskBean],替换对应的数据*/
    fun TaskBean.init(): TaskBean {
        actionList = actionList?.init()
        backActionList = backActionList?.init()
        before = before?.init()
        after = after?.init()

        //回退列表
        allBackActionData.value?.init()?.let { allBackActionList ->
            backActionList = (backActionList?.toMutableList() ?: mutableListOf()).apply {
                addAll(allBackActionList)
            }
        }

        //init
        initConfig()

        return this
    }

    fun List<ActionBean>.init(): List<ActionBean> {
        val result = mutableListOf<ActionBean>()
        forEach {
            if (!it.hide) {
                result.add(it.init())
            }
        }
        return result
    }

    /**初始化[ActionBean],替换对应的数据*/
    fun ActionBean.init(): ActionBean {
        val result = toJson().fromJson<ActionBean>() ?: this
        if (result.check == null) {
            result.check = findCheck(result.checkId)
        }
        result.after = result.after?.init()
        result.before = result.before?.init()
        if (result.actionId == -1L) {
            //如果未指定[actionId]
            result.actionId = result.hashCode().toLong()
        }
        return result
    }

    //</editor-fold desc="初始化">

    fun _addTasks(bean: TaskBean) {
        _addTasks(listOf(bean))
    }

    fun _addTasks(list: List<TaskBean>) {
        val result = mutableListOf<TaskBean>()

        allTaskData.value?.forEach {
            if (!result.contains(it)) {
                result.add(it)
            }
        }

        list.forEach {
            if (it.enable && !result.contains(it)) {
                result.add(it)
            }
        }

        //排序
        result.sort()

        if (isMain()) {
            allTaskData.value = result
        }
    }

    /**从小到大的自然排序*/
    fun MutableList<TaskBean>.sort() {
        this.sortBy {
            val isInstall = it.packageName.isInstall()
            val isAdaptive = it.packageName.isAdaptive()

            if (!isInstall) {
                //未安装, 放在最后面
                Long.MAX_VALUE
            } else if (it.adaptive && !isAdaptive) {
                //未适配放在最前面
                Long.MIN_VALUE
            } else {
                if (isDebug() && TaskModel.firstTaskId.contains(it.taskId)) {
                    0
                } else {
                    it.taskId
                }
            }
        }
    }
}