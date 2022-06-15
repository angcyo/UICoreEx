package com.angcyo.bugly

import android.content.Context
import android.os.Build
import com.angcyo.library.annotation.CallComplianceAfter
import com.angcyo.library.app
import com.angcyo.library.ex.isShowDebug
import com.angcyo.library.getAppString
import com.angcyo.library.isMainProgress
import com.angcyo.library.utils.Device
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

    /**初始化方法
     * https://bugly.qq.com/docs/user-guide/advance-features-android/
     * */
    @CallComplianceAfter
    fun init(
        context: Context = app(),
        debug: Boolean = isShowDebug(),
        config: UserStrategy.() -> Unit = {}
    ) {
        // 设置是否为上报进程
        val strategy = UserStrategy(context)
        strategy.isUploadProcess = context.isMainProgress()
        strategy.deviceID = Device.androidId
        strategy.deviceModel = Build.MODEL

        //Bugly会在启动10s后联网同步数据。若您有特别需求，可以修改这个时间。
        //strategy.setAppReportDelay()

        // 设置anr时是否获取系统trace文件，默认为false
        //strategy.setEnableCatchAnrTrace()

        //设置是否获取anr过程中的主线程堆栈，默认为true
        //strategy.setEnableANRCrashMonitor()

        //CrashReport.setUserSceneTag()

        //dsl
        strategy.config()

        //开发设备
        CrashReport.setIsDevelopmentDevice(context, debug)

        //Javascript的异常捕获功能
        //CrashReport.setJavascriptMonitor(WebView webView, boolean autoInject)

        //init
        //CrashReport.initCrashReport(context, BuildConfig.BUGLY_ID, debug, strategy)
        CrashReport.initCrashReport(context, getAppString("bugly_id"), debug, strategy)
    }

    /**测试bugly功能*/
    fun test(native: Boolean = true) {
        CrashReport.testJavaCrash()
        if (native) {
            CrashReport.testNativeCrash()
        }
    }
}