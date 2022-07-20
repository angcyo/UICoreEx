package com.angcyo.crash.sight

import com.angcyo.library.L
import com.angcyo.library.app
import com.angcyo.library.ex.isDebug
import com.angcyo.library.getAppString
import com.uqm.crashsight.crashreport.CrashReport

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/20
 */
object CrashSight {

    /**
     * 获取APP ID并将以下代码复制到项目Application类onCreate()中，CrashSight会为自动检测环境并完成配置：（为了保证运营数据的准确性，建议不要在异步线程初始化CrashSight。）
     * */
    fun init(debug: Boolean = isDebug()) {
        val serverUrl = getAppString("CrashSightUrl")
        val appId = getAppString("CrashSightAppID")
        // 设置上报地址
        CrashReport.setServerUrl(serverUrl)
        //初始化
        CrashReport.initCrashReport(app(), appId, debug)
        L.i("CrashSight初始化[$debug]:$appId $serverUrl")
    }

    /**测试崩溃*/
    fun test() {
        CrashReport.testJavaCrash()
    }

}