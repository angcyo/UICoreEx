package com.angcyo.server.def

import com.angcyo.library.LTime
import com.angcyo.library.ex.className
import com.angcyo.library.ex.nowTimeString
import com.angcyo.library.ex.size
import com.angcyo.library.ex.syncSingle
import com.angcyo.library.ex.toSizeString
import com.angcyo.quickjs.QuickJSEngine
import com.yanzhenjie.andserver.annotation.CrossOrigin
import com.yanzhenjie.andserver.annotation.PostMapping
import com.yanzhenjie.andserver.annotation.RequestBody
import com.yanzhenjie.andserver.annotation.RestController

/**
 * 提供一些默认的借口
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2023/06/10
 */
@RestController
@CrossOrigin
class DefController {

    /**执行js脚本*/
    @PostMapping("/js")
    fun executeScript(@RequestBody body: String): String {
        LTime.tick()
        val resultBuilder = StringBuilder()
        resultBuilder.appendLine(nowTimeString())
        resultBuilder.appendLine("准备执行脚本[${body.size().toSizeString()}]->")
        if (body.size() <= 1024) {//小于1k, 直接打印内容
            resultBuilder.appendLine(body)
        }
        syncSingle {
            QuickJSEngine.executeScript(body) { result, error ->
                if (error == null) {
                    resultBuilder.appendLine("执行结果[${result?.className() ?: ""}]:$result")
                } else {
                    resultBuilder.appendLine("执行错误:$error")
                }
                resultBuilder.append("耗时:${LTime.time()}")
                it.countDown()
            }
        }
        return resultBuilder.toString()
    }

}