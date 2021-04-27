package com.angcyo.acc2.app.model

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.angcyo.acc2.app.R
import com.angcyo.acc2.app.dslitem.shareApk
import com.angcyo.acc2.app.http.Gitee
import com.angcyo.acc2.app.http.Message
import com.angcyo.acc2.app.http.bean.*
import com.angcyo.core.lifecycle.LifecycleViewModel
import com.angcyo.core.vmApp
import com.angcyo.dialog.normalIosDialog
import com.angcyo.http.base.fromJson
import com.angcyo.http.get
import com.angcyo.http.interceptor.LogInterceptor
import com.angcyo.http.isSucceed
import com.angcyo.http.rx.observer
import com.angcyo.http.toBean
import com.angcyo.library.app
import com.angcyo.library.component.appBean
import com.angcyo.library.ex.*
import com.angcyo.library.utils.Device
import com.angcyo.viewmodel.vmData
import com.angcyo.widget.span.DslSpan

/**
 * 对应程序适配管理
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/08/05
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */

class AdaptiveModel : LifecycleViewModel() {

    val adaptiveData: MutableLiveData<AdaptiveVersionBean> = vmData(null)
    val adminData: MutableLiveData<AdminBean> = vmData(null)
    val appListData: MutableLiveData<HttpBean<List<AppInfoBean>>> = vmData(null)

    /**加载本地适配数据信息*/
    fun loadAdaptiveVersion(online: Boolean = !isDebugType()) {
        if (online) {
            get {
                url = "${Gitee.BASE}/adaptive_version.json"
                query = hashMapOf("time" to nowTime()) //带上时间参数, 避免缓存
                header = hashMapOf(LogInterceptor.closeLog())
            }.observer {
                onObserverEnd = { data, _ ->
                    data?.let {
                        if (it.isSucceed()) {
                            it.toBean(AdaptiveVersionBean::class.java)?.let { bean ->
                                val oldVersion: Long = adaptiveData.value?.version ?: 0
                                if (oldVersion < bean.version) {
                                    adaptiveData.value = bean
                                }
                            }
                        }
                    }
                }
            }
        } else {
            app().readAssets("adaptive_version.json")
                ?.fromJson(AdaptiveVersionBean::class.java)?.let {
                    adaptiveData.value = it
                }
        }
    }

    /**加载本地适配数据信息*/
    fun loadAnim(online: Boolean = !isDebugType()) {
        if (online) {
            get {
                url = "${Gitee.BASE}/admin.json"
                query = hashMapOf("time" to nowTime()) //带上时间参数, 避免缓存
                header = hashMapOf(LogInterceptor.closeLog())
            }.observer {
                onObserverEnd = { data, _ ->
                    data?.let {
                        if (it.isSucceed()) {
                            it.toBean(AdminBean::class.java).let { bean ->
                                val oldBean: Long = adminData.value?.version ?: 0
                                if (oldBean < bean?.version ?: 0) {
                                    adminData.value = bean
                                }
                            }
                        }
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
            get {
                url = "${Gitee.BASE}/app_list.json"
                query = hashMapOf("time" to nowTime()) //带上时间参数, 避免缓存
                header = hashMapOf(LogInterceptor.closeLog())
            }.observer {
                onObserverEnd = { data, _ ->
                    data?.let {
                        if (it.isSucceed()) {
                            it.toBean<HttpBean<List<AppInfoBean>>>(beanListType).let { bean ->
                                val oldVersion = appListData.value?.version ?: 0
                                if (oldVersion < bean?.version ?: 0) {
                                    appListData.value = bean
                                    //应用列表
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Gitee.assets<HttpBean<List<AppInfoBean>>>("app_list.json", beanListType) {
                appListData.value = it
            }
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
     * [packageName] 要检查的应用包名*/
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
            context?.shareApk("分享APK给技术适配")

            context?.normalIosDialog {
                dialogTitle = "注意"
                dialogMessage = builder.doIt()

                negativeButtonText = null
                positiveButton("查看帮助") { dialog, dialogViewHolder ->
                    dialog.dismiss()
                    toHelp()
                }
            }
        }

        return noAdaptive
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