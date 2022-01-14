package com.angcyo.acc2.app.model

import com.angcyo.acc2.app.http.bean.FunctionBean
import com.angcyo.acc2.app.http.bean.MessageBean
import com.angcyo.acc2.bean.*
import com.angcyo.acc2.control.AccControl
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

    /**存储功能列表
     * [com.angcyo.acc2.app.http.Gitee.fetchFunctionList]*/
    val allFunctionData = vmData(listOf<FunctionBean>())

    /**[com.angcyo.acc2.app.http.Gitee.fetchAllAction]*/
    val allActionData = vmData(listOf<ActionBean>())

    //备份
    val allActionDataBack = mutableListOf<ActionBean>()

    /**[com.angcyo.acc2.app.http.Gitee.fetchAllBackAction]*/
    val allBackActionData = vmData(listOf<ActionBean>())

    //备份
    val allBackActionDataBack = mutableListOf<ActionBean>()

    /**[com.angcyo.acc2.app.http.Gitee.fetchAllCheck]*/
    val allCheckData = vmData(listOf<CheckBean>())

    //备份
    val allCheckDataBack = mutableListOf<CheckBean>()

    /**[com.angcyo.acc2.app.http.Gitee.fetchAllCheck]*/
    val allAssetCheckData = vmData(listOf<CheckBean>())

    /**所有在线任务
     * [com.angcyo.acc2.app.http.Gitee.fetchAllTask]
     * */
    val allTaskData = vmData(listOf<TaskBean>())

    /**新消息提醒
     * [com.angcyo.acc2.app.http.Message.parseMessage]*/
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
        //3
        for (check in allCheckDataBack) {
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

    fun initTask(control: AccControl?, bean: TaskBean): TaskBean {
        return bean.toJson().fromJson<TaskBean>()!!.init(control)
    }

    /**初始化[TaskBean],替换对应的数据*/
    fun TaskBean.init(control: AccControl?): TaskBean {
        _init = true

        //dynamic
        AccControl.initTaskDynamic(control, this)

        actionList = actionList?.init()
        backActionList = backActionList?.init()
        intervalList = intervalList?.init()

        before = before?.init()
        after = after?.init()
        leave = leave?.init()
        lose = lose?.init()

        //公共列表
        if (allActionDataBack.isNotEmpty()) {
            allActionDataBack
        } else {
            allActionData.value
        }?.init()?.let { allActionList ->
            actionList = (actionList?.toMutableList() ?: mutableListOf()).apply {
                addAll(allActionList)
            }
        }

        //回退列表
        if (allBackActionDataBack.isNotEmpty()) {
            allBackActionDataBack
        } else {
            allBackActionData.value
        }?.init()?.let { allBackActionList ->
            backActionList = (backActionList?.toMutableList() ?: mutableListOf()).apply {
                addAll(allBackActionList)
            }
        }

        //init
        initConfig()

        //dynamic
        AccControl.initAllHandleCls(control, this)

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

    fun initAction(bean: ActionBean): ActionBean {
        return bean.init()
    }

    /**初始化[ActionBean],替换对应的数据*/
    fun ActionBean.init(): ActionBean {
        val result = toJson().fromJson<ActionBean>() ?: this
        if (result.check == null) {
            result.check = findCheck(result.checkId)
        }

        result.after = result.after?.init()
        result.before = result.before?.init()
        result.leave = result.leave?.init()
        result.lose = result.lose?.init()
        result.interval = result.interval?.init()

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
                if (isDebug() && AccTaskModel.firstTaskId.contains(it.taskId)) {
                    0
                } else {
                    it.taskId
                }
            }
        }
    }
}