package com.angcyo.um

import android.app.Application
import android.content.Context
import com.angcyo.library.L
import com.angcyo.library.app
import com.umeng.analytics.MobclickAgent
import com.umeng.commonsdk.UMConfigure

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/02/19
 * Copyright (c) 2020 angcyo. All rights reserved.
 */
class UM {

    companion object {

        //https://developer.umeng.com/docs/119267/detail/118637
        //event id长度不能超过128个字节，key不能超过128个字节，value不能超过256个字节
        //event ID或者key请使用英文、数字、下划线、中划线及加号进行定义，使用其中一种或者几种都可以，不能以“数字”开头，避免使用中文。
        //具体限制请查看下文注意事项为保证数据计算的准确性，非这些“合法”以外的字符无法添加，具体限制请查看下文注意事项；
        fun event(eventId: String, value: String? = null, context: Context = app()) {
            MobclickAgent.onEvent(context, eventId, value)
        }

        fun event(eventId: String, map: Map<String, String>, context: Context = app()) {
            MobclickAgent.onEventObject(context, eventId, map)
        }
    }

    var isDebug: Boolean = L.debug

    //https://developer.umeng.com/docs/193624/detail/194590

    var context: Context? = app()

    /**
     * 注意: 即使您已经在AndroidManifest.xml中配置过appkey和channel值，也需要在App代码中调
     * 用初始化接口（如需要使用AndroidManifest.xml中配置好的appkey和channel值，
     * UMConfigure.init调用中appkey和channel参数请置为null）。
     */
    var appKey: String? = null

    var channel: String? = "Default"

    //设备类型，UMConfigure.DEVICE_TYPE_PHONE为手机、UMConfigure.DEVICE_TYPE_BOX为盒子，默认为手机
    var deviceType: Int = UMConfigure.DEVICE_TYPE_PHONE

    //Push推送业务的secret
    var pushSecret: String? = null

    //设置日志加密
    //https://developer.umeng.com/docs/66632/detail/101814#h1-u521Du59CBu5316u53CAu901Au7528u63A5u53E32
    var encryptEnabled: Boolean = true

    //子进程是否支持自定义事件统计。
    var processEvent: Boolean = true

    var activityLifecycleCallbacks: Application.ActivityLifecycleCallbacks? = null

    //https://developer.umeng.com/docs/119267/detail/118588
    var pageMode: MobclickAgent.PageMode = MobclickAgent.PageMode.AUTO

    /**1: 预初始化*/
    fun preInit() {
        UMConfigure.setLogEnabled(isDebug)
        UMConfigure.setEncryptEnabled(encryptEnabled)

        if (_check()) {
            UMConfigure.preInit(context, appKey, channel)
            val _context = context
            if (_context is Application && activityLifecycleCallbacks != null) {
                _context.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
            }
        }
    }

    /**2: 初始化*/
    fun init() {
        if (_check()) {
            UMConfigure.init(context, appKey, channel, deviceType, pushSecret)

            UMConfigure.setProcessEvent(processEvent)

            // 选用AUTO页面采集模式
            MobclickAgent.setPageCollectionMode(pageMode)
        }
    }

    fun _check(): Boolean {
        if (context == null) {
            L.e("请配置[context]")
            return false
        }
        if (appKey.isNullOrEmpty()) {
            L.e("请配置[appKey]")
            return false
        }
        return true
    }
}