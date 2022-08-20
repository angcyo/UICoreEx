package com.angcyo.acc2.app.server

import com.angcyo.acc2.app.component.Task
import com.angcyo.acc2.app.component.init
import com.angcyo.acc2.bean.ActionBean
import com.angcyo.acc2.bean.FindBean
import com.angcyo.acc2.bean.HandleBean
import com.angcyo.acc2.bean.TaskBean
import com.angcyo.acc2.control.ControlContext
import com.angcyo.acc2.parse.HandleResult
import com.angcyo.acc2.parse.toLog
import com.angcyo.library.ex.nowTimeString
import com.yanzhenjie.andserver.annotation.*
import com.yanzhenjie.andserver.framework.body.StringBody
import com.yanzhenjie.andserver.http.HttpRequest
import com.yanzhenjie.andserver.http.HttpResponse
import com.yanzhenjie.andserver.http.ResponseBody

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/07/06
 * Copyright (c) 2020 angcyo. All rights reserved.
 */

@RequestMapping("/do")
@RestController("acc")
class DoController {

    /**执行指定的action*/
    @PostMapping("/action")
    fun action(@RequestBody bean: ActionBean): String {
        //Task.control.accSchedule.startTargetAction(bean, true)

        val actionBean = bean.init()

        val controlContext = ControlContext().apply {
            control = Task.control
            action = actionBean
        }

        val handleActionResult = HandleResult()

        Task.control.accSchedule.runActionInner(
            controlContext,
            actionBean,
            null,
            true,
            handleActionResult
        )

        return controlContext.log {
            append("处理结束:${handleActionResult.isSuccessResult()}")
        }
    }

    /**执行任务*/
    @PostMapping("/task")
    fun task(@RequestBody bean: TaskBean): String? {
        if (Task.start(bean, force = true) != null) {
            return Task.control.controlLog()
        }
        return Task.control.finishReason
    }

    /**查找目标元素*/
    @PostMapping("/find")
    fun find(@RequestBody bean: FindBean): String {
        return Task.control.accSchedule.findNodeList(listOf(bean))
            ?.toLog() ?: "空"
    }

    /**执行HandleBean*/
    @PostMapping("/handle")
    fun handle(@RequestBody bean: HandleBean): String {
        val old = Task.control.accPrint.memory
        Task.control.accPrint.memory = true
        val handleResult = Task.control.accSchedule.handle(listOf(bean))
        Task.control.accPrint.memory = old
        if (handleResult.isSuccessResult()) {
            return Task.control.accPrint.memoryLogCache?.joinToString("\n") ?: "处理成功"
        }
        return "处理失败"
    }

    @GetMapping("/test")
    fun test(request: HttpRequest, response: HttpResponse): ResponseBody {
        //response.setBody(StringBody(nowTimeString()))
        return StringBody(nowTimeString())
    }
}