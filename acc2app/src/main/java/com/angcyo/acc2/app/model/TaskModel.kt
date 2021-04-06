package com.angcyo.acc2.app.model

import androidx.lifecycle.MutableLiveData
import com.angcyo.acc2.bean.TaskBean
import com.angcyo.acc2.control.AccControl
import com.angcyo.core.lifecycle.LifecycleViewModel
import com.angcyo.viewmodel.vmData
import com.angcyo.viewmodel.vmDataNull

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/02/07
 * Copyright (c) 2020 angcyo. All rights reserved.
 */
open class TaskModel : LifecycleViewModel() {

    companion object {
        /**放在首位的任务id*/
        val firstTaskId = listOf<Long>()
    }

    /**正在运行的任务*/
    val taskData: MutableLiveData<TaskBean?> = vmDataNull()

    /**正在运行的任务状态*/
    val taskStateData: MutableLiveData<Int> = vmData(AccControl.CONTROL_STATE_NORMAL)
}