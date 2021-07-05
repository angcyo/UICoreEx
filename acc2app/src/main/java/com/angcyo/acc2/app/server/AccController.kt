package com.angcyo.acc2.app.server

import com.angcyo.acc2.core.AccNodeLog
import com.yanzhenjie.andserver.annotation.GetMapping
import com.yanzhenjie.andserver.annotation.RestController

/**
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
}