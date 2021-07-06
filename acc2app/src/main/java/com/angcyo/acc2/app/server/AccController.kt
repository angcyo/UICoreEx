package com.angcyo.acc2.app.server

import com.angcyo.acc2.app.AppAccLog
import com.angcyo.acc2.app.component.Task
import com.angcyo.acc2.core.AccNodeLog
import com.angcyo.http.base.toJson
import com.angcyo.library.ex.file
import com.yanzhenjie.andserver.annotation.GetMapping
import com.yanzhenjie.andserver.annotation.RestController

/**
 * https://yanzhenjie.com/AndServer/annotation/RestController.html
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/07/06
 * Copyright (c) 2020 angcyo. All rights reserved.
 */

@RestController
class AccController {

    /**捕抓界面*/
    @GetMapping("/catch")
    fun catchLog(): String {
        return AccNodeLog().getAccessibilityWindowLog().toString()
    }

    /**清理日志, 只会清理acc目录下的日志*/
    @GetMapping("/clear")
    fun clearLog(): String {
        val path = AppAccLog.logPath()
        val result = path?.file()?.deleteRecursively() == true
        return "清理日志[$result]:$path"
    }

    /**返回控制器的状态日志*/
    @GetMapping("/controlLog")
    fun controlLog(): String {
        return Task.control.controlLog()
    }

    /**控制器正在进行的任务*/
    @GetMapping("/task")
    fun task(): String {
        return Task.control._taskBean?.toJson() ?: "无任务在运行"
    }
}