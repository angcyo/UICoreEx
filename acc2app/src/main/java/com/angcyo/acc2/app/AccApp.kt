package com.angcyo.acc2.app

import android.content.Context
import com.angcyo.acc2.app.http.Gitee
import com.angcyo.acc2.app.http.bean.MemoryConfigBean
import com.angcyo.acc2.core.AccPermission
import com.angcyo.core.CoreApplication
import com.angcyo.core.fragment.BaseUI
import com.angcyo.library.component.DslNotify
import com.angcyo.library.ex.isDebug
import com.angcyo.library.ex.isDebugType
import com.angcyo.library.ex.uuid
import com.angcyo.library.getAppVersionName
import com.angcyo.library.utils.Device
import com.angcyo.library.utils.RUtils
import com.angcyo.library.utils.fillTo
import com.angcyo.widget.edit.BaseEditDelegate
import com.angcyo.widget.span.span

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/07/04
 * Copyright (c) 2020 angcyo. All rights reserved.
 */
open class AccApp : CoreApplication() {

    companion object {

        //设备码
        var appAndroidId: String = ""
            get() {
                val id = if (field.isEmpty()) {
                    if (isDebugType()) {
                        uuid()
                    } else {
                        Device.androidId
                    }
                } else {
                    field
                }

                field = id

                return id
            }

        //跳过权限判断
        var jumpPermission: Boolean = false

        fun haveAllPermission(context: Context): Boolean {
            if (jumpPermission) {
                return true
            }
            return AccPermission.haveAllPermission(context)
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        RUtils.fixFinalizerWatchdogDaemon()
    }

    override fun onCreate() {
        super.onCreate()

        DslNotify.DEFAULT_NOTIFY_ICON = R.drawable.notify_logo

        BaseUI.fragmentUI.apply {
            fragmentCreateAfter = { _, fragmentConfig, _ ->
                //fragmentConfig.fragmentBackgroundDrawable = ColorDrawable(_color(R.color.bg_color))
            }
        }

        //初始化
        Gitee.init()

        //debug
        BaseEditDelegate.textChangedActionList.add(AccDebug::onAccDebugTextChanged)
    }

    //配置信息
    var memoryConfigBean = MemoryConfigBean()
}

fun app() = com.angcyo.library.app() as AccApp

fun mainMemoryConfig() = app().memoryConfigBean

/**使用主要的配置填充补充当前的配置
 * [MemoryConfigBean]*/
fun memoryConfig(packageName: String = app().packageName): MemoryConfigBean {
    val main = mainMemoryConfig()
    return main.pks?.get(packageName) ?: main
}

fun String.isMyDevice() = this == Device.androidId

fun versionTipName() = span {
    append("当前版本:${getAppVersionName()}")
    if (isDebugType()) {
        append("-dev")
    } else if (isDebug()) {
        append("-debug")
    }
}

/**使用主配置, 填充pks字段*/
fun MemoryConfigBean.fillPks(): MemoryConfigBean {
    pks?.forEach { entry ->
        this.fillTo(
            entry.value,
            ignoreNull = true,
            jumpValue = true,
            ignoreFiledNameList = listOf("pks")
        )
    }
    return this
}