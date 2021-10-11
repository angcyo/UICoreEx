package com.angcyo.bugly

import android.content.Context
import com.angcyo.library.app
import com.angcyo.library.ex.isDebug
import com.angcyo.library.isMainProgress
import com.tencent.bugly.crashreport.CrashReport

import com.tencent.bugly.crashreport.CrashReport.UserStrategy


/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/10/11
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
object Bugly {

    /**初始化方法*/
    fun init(context: Context = app(), debug: Boolean = isDebug()) {
        // 设置是否为上报进程
        val strategy = UserStrategy(context)
        strategy.isUploadProcess = context.isMainProgress()

        //开发设备
        CrashReport.setIsDevelopmentDevice(context, debug)

        //init
        CrashReport.initCrashReport(context, BuildConfig.BUGLY_ID, debug, strategy)
    }

    /**测试bugly功能*/
    fun test() {
        CrashReport.testJavaCrash()
    }
}