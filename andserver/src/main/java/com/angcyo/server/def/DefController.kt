package com.angcyo.server.def

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
        return "准备执行脚本:$body"
    }

}