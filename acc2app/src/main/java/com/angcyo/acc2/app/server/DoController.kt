package com.angcyo.acc2.app.server

import com.angcyo.acc2.app.component.Task
import com.angcyo.acc2.bean.ActionBean
import com.angcyo.acc2.bean.TaskBean
import com.yanzhenjie.andserver.annotation.PostMapping
import com.yanzhenjie.andserver.annotation.RequestBody
import com.yanzhenjie.andserver.annotation.RequestMapping
import com.yanzhenjie.andserver.annotation.RestController

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/07/06
 * Copyright (c) 2020 angcyo. All rights reserved.
 */

@RequestMapping("/do")
@RestController
class DoController {

    /**执行指定的action*/
    @PostMapping("/action")
    fun action(@RequestBody bean: ActionBean): Boolean {
        Task.control.accSchedule.startTargetAction(bean)
        return true
    }

    /**执行任务*/
    @PostMapping("/task")
    fun task(@RequestBody bean: TaskBean): String? {
        if (Task.control.start(bean, true)) {
            return Task.control.controlLog()
        }
        return Task.control.finishReason
    }
}