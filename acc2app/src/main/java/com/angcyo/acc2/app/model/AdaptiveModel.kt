package com.angcyo.acc2.app.model

import android.app.Activity
import android.content.Context
import com.angcyo.acc2.app.R
import com.angcyo.acc2.app.dslitem.shareApk
import com.angcyo.acc2.app.http.AccGitee
import com.angcyo.acc2.app.http.Message
import com.angcyo.acc2.app.http.bean.*
import com.angcyo.core.lifecycle.LifecycleViewModel
import com.angcyo.core.vmApp
import com.angcyo.dialog.normalIosDialog
import com.angcyo.http.base.fromJson
import com.angcyo.http.toBean
import com.angcyo.library.app
import com.angcyo.library.component.appBean
import com.angcyo.library.ex.*
import com.angcyo.library.toastQQ
import com.angcyo.library.utils.Device
import com.angcyo.viewmodel.vmDataNull
import com.angcyo.widget.span.DslSpan

/**
 * 对应程序适配管理
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/08/05
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */

class AdaptiveModel : LifecycleViewModel() {

    companion object {
        //包名
        const val DY_PACKAGENAME = "com.ss.android.ugc.aweme"
        const val KS_PACKAGENAME = "com.smile.gifmaker"
        const val WX_PACKAGENAME = "com.tencent.mm"
        const val XHS_PACKAGENAME = "com.xingin.xhs"
    }

    val adaptiveData = vmDataNull<AdaptiveVersionBean>()
    val adminData = vmDataNull<AdminBean>()
    val appListData = vmDataNull<HttpBean<List<AppInfoBean>>>(null)

    /**加载本地适配数据信息*/
    fun loadAdaptiveVersion(online: Boolean = !isDebugType()) {
        AccGitee.fetchAdaptiveConfig(online) { data, error ->
            val oldVersion: Long = adaptiveData.value?.version ?: 0
            if (oldVersion < data?.version ?: -1) {
                adaptiveData.value = data
            }
        }
    }

    /**加载本地适配数据信息*/
    fun loadAnim(online: Boolean = !isDebugType()) {
        if (online) {
            AccGitee.get("admin.json") { data, error ->
                if (data != null) {
                    val bean = data.toBean<AdminBean>()
                    val oldBean: Long = adminData.value?.version ?: 0
                    if (oldBean < (bean?.version ?: 0)) {
                        adminData.value = bean!!
                    }
                }
            }
        } else {
            app().readAssets("admin.json")
                ?.fromJson(AdminBean::class.java).let {
                    adminData.value = it
                }
        }
    }

    /**加载网赚挂机APP列表*/
    fun loadAppList(online: Boolean = !isDebugType()) {
        val beanListType = listBeanType(AppInfoBean::class.java)
        if (online) {
            AccGitee.get("app_list.json") { data, error ->
                data?.let {
                    it.toBean<HttpBean<List<AppInfoBean>>>(beanListType).let { bean ->
                        val oldVersion = appListData.value?.version ?: 0
                        if (oldVersion < (bean?.version ?: 0)) {
                            //应用列表
                            appListData.value = bean
                        }
                    }
                }
            }
        } else {
            AccGitee.assets<HttpBean<List<AppInfoBean>>>("app_list.json", beanListType) {
                appListData.value = it
            }
        }
    }

    /**最后一条的适配信息*/
    fun lastAdaptiveInfo(packageName: String?): AppVersionBean? {
        return adaptiveData.value?.data?.last { adaptiveBean ->
            //获取对应包名的适配信息
            adaptiveBean.packageName == packageName
        }
    }

    /**获取对应程序的适配信息, 如果未适配, 返回null*/
    fun getAdaptiveInfo(packageName: String?): AppVersionBean? {
        return packageName?.appBean()?.let { app ->
            adaptiveData.value?.data?.find { adaptiveBean ->
                //获取对应包名的适配信息
                adaptiveBean.packageName == app.packageName
            }?.run {
                //查找适配的版本规则
                val findVersion = versionNameList?.find { versionName ->
                    if (versionName.startsWith("^"))
                        app.versionName.have(versionName)
                    else
                        versionName == app.versionName
                }
                if (findVersion == null) {
                    null
                } else {
                    this
                }
            }
        }
    }

    /**检查程序适配信息
     * [packageName] 要检查的应用包名
     * 返回 是否未适配*/
    fun checkAdaptiveInfo(
        context: Context?,
        packageName: String?,
        toHelp: () -> Unit
    ): Boolean {
        val builder = DslSpan()

        var noAdaptive = false

        if (packageName == null) {
            return false
        }

        getAdaptiveInfo(packageName)?.let {
            //适配
        }.elseNull {
            //未适配
            builder.append("发现不匹配程序版本\n\n")

            noAdaptive = true

            builder.append("本机")
            builder.append(packageName.appBean()?.appName) {
                foregroundColor = _color(R.color.colorAccent)
            }
            builder.append("版本不支持")
            builder.appendln()
        }

        if (noAdaptive) {
            //builder.append("\n继续使用将会产生未知的识别误差!")

            //share
            if (context is Activity) {
                context.shareApk("分享APK给技术适配")

                context.normalIosDialog {
                    dialogTitle = "注意"
                    dialogMessage = builder.doIt()

                    negativeButtonText = null
                    positiveButton("查看帮助") { dialog, dialogViewHolder ->
                        dialog.dismiss()
                        toHelp()
                    }
                }
            } else {
                toastQQ(builder.doIt())
            }
        }

        return noAdaptive
    }

    /**检查程序适配信息
     * [packageName] 要检查的应用包名
     * 返回是否适配*/
    fun checkAdaptiveInfo2(context: Context?, packageName: String?): Boolean {
        val builder = DslSpan()

        var adaptive = false

        if (packageName == null) {
            return false
        }

        val adaptiveInfo = lastAdaptiveInfo(packageName)

        getAdaptiveInfo(packageName)?.let {
            //适配
            adaptive = true
        }.elseNull {
            //未适配
            builder.append("您的${adaptiveInfo?.name}版本不匹配\n\n")
            builder.append("请使用: ")
            builder.append(adaptiveInfo?.versionNameList?.lastOrNull()) {
                foregroundColor = _color(R.color.colorAccent)
            }
            builder.appendln()
        }

        if (!adaptive) {
            //builder.append("\n继续使用将会产生未知的识别误差!")
            if (context is Activity) {
                context.normalIosDialog {
                    dialogTitle = "注意"
                    dialogMessage = builder.doIt()

                    negativeButtonText = null
                }
            } else {
                toastQQ(builder.doIt())
            }
        }

        return adaptive
    }

    /**获取所有app适配的情况/安装情况以及对应的版本信息*/
    fun getAllAppAdaptiveInfo(): String = buildString {
        allApp.forEach { app ->
            app.packageName?.appBean()?.it {
                append(app.label ?: it.appName)
                append("[${app.packageName}]")
                append(":")
                append(it.versionName)

                if (vmApp<AdaptiveModel>().getAdaptiveInfo(it.packageName) == null) {
                    //未找到适配信息
                    append(" ×")
                }
            }.elseNull {
                append(app.label)
                append("[${app.packageName}]")
                append(":未安装")
            }
            append(" ")
        }
    }

    /**是否是管理员*/
    fun isAdmin(num: String? = null, device: String = Device.androidId): Boolean {
        val adminBean = adminData.value
        return adminBean?.data?.contains(num) == true ||
                adminBean?.devices?.contains(device) == true ||
                isSuperAdmin(num, device)
    }

    /**是否是调试设备*/
    fun isDebugDevice(num: String? = null, device: String = Device.androidId): Boolean {
        val adminBean = adminData.value
        return adminBean?.debugDevices?.contains(device) == true
    }

    /**是否是超级管理员*/
    fun isSuperAdmin(num: String? = null, device: String = Device.androidId): Boolean {
        return adminData.value?.superDevices?.contains(device) == true
    }

    /**自己的设备*/
    fun isSelfDevice(device: String = Device.androidId): Boolean {
        return adminData.value?.selfDevices?.contains(device) == true
    }

    /**vip*/
    fun isVip(device: String = Device.androidId): Boolean {
        return adminData.value?.vip?.contains(device) == true
    }

    /**更新配置
     * [com.angcyo.acc2.app.AccMainActivity.onResume]*/
    fun updateOnResume() {
        //load
        loadAnim()
        loadAdaptiveVersion()
        //loadAppSetting(true)
        loadAppList()

        //拉取消息列表
        Message.fetchMessage()
    }
}

/**所有APP列表*/
val allApp: List<AppInfoBean>
    get() = vmApp<AdaptiveModel>().appListData.value?.data ?: emptyList()

/**是否安装了程序*/
fun String?.isInstall() = this?.appBean() != null

/**是否适配了程序*/
fun String?.isAdaptive() = vmApp<AdaptiveModel>().getAdaptiveInfo(this) != null

/**未适配的程序信息*/
fun noAdaptiveDes(): String? {
    val result = StringBuilder()
    allApp.forEach {
        if (it.enable && !it.packageName.isAdaptive()) {
            it.packageName?.appBean()?.apply {
                if (!result.toString().isBlank()) {
                    result.appendLine()
                }
                result.append(appName)
                result.append(versionName)
                result.append(versionCode.toStr().des())
                result.append("未适配")
            }
        }
    }
    if (result.toString().isBlank()) {
        return null
    }
    return result.toStr()
}