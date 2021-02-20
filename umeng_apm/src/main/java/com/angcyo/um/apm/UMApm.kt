package com.angcyo.um.apm

import com.umeng.umcrash.UMCrash

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/02/20
 * Copyright (c) 2020 angcyo. All rights reserved.
 */
object UMApm {

    //https://developer.umeng.com/docs/193624/detail/194590
    //type 尽量不要使用中文
    fun customLog(error: String, type: String) {
        UMCrash.generateCustomLog(error, type)
    }

    //https://developer.umeng.com/docs/193624/detail/194590
    fun customLog(error: Throwable, type: String) {
        UMCrash.generateCustomLog(error, type)
    }
}