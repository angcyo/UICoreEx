package com.hingin.umeng

import android.app.Application
import android.content.Context
import com.angcyo.library.L
import com.angcyo.library.app
import com.umeng.analytics.MobclickAgent
import com.umeng.commonsdk.UMConfigure

/**
 * UMeng统计
 *
 * https://developer.umeng.com/docs/119267/detail/118578
 *
 * 集成测试配置, 之后扫码就能测试
 * ```
 * <!--umeng-->
 * <intent-filter>
 *   <action android:name="android.intent.action.VIEW" />
 *   <category android:name="android.intent.category.DEFAULT" />
 *   <category android:name="android.intent.category.BROWSABLE" />
 *   <data android:scheme="um.62d6327c88ccdf4b7ed61eba" />
 * </intent-filter>
 * ```
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
        fun event(eventId: String, context: Context = app()) {
            MobclickAgent.onEvent(context, eventId)
        }

        /**计数事件
         * [eventId] 为当前统计的事件ID。
         * [map] 对当前事件的参数描述，定义为“参数名:参数值”的HashMap“<键-值>对”。
         *
         * ```
         * Map<String, Object> music = new HashMap<String, Object>();
         * music.put("music_type", "popular");//自定义参数：音乐类型，值：流行
         * music.put("singer", "JJ"); //歌手：(林俊杰)JJ
         * music.put("song_name","A_Thousand_Years_Later"); //歌名：一千年以后
         * music.put("song_price",100); //价格：100元
         * MobclickAgent.onEventObject(this, "play_music", music);
         * ```
         * */
        fun event(eventId: String, map: Map<String, Any>, context: Context = app()) {
            MobclickAgent.onEventObject(context, eventId, map)
        }

        /**计数事件*/
        fun event(
            eventId: String,
            value: Int,
            map: Map<String, String> = hashMapOf(),
            context: Context = app()
        ) {
            MobclickAgent.onEventValue(context, eventId, map, value)
        }

        /**如果开发者调用kill或者exit之类的方法杀死进程，或者双击back键会杀死进程，请务必在此之前调用onKillProcess方法，用来保存统计数据。*/
        fun onKillProcess(context: Context = app()) {
            MobclickAgent.onKillProcess(context)
        }

        /**定义页面进入
         * [name] 自定义页面名。*/
        fun pageStart(name: String) {
            MobclickAgent.onPageStart(name)
        }

        /**定义页面退出
         * [name] 自定义页面名。*/
        fun pageEnd(name: String) {
            MobclickAgent.onPageEnd(name)
        }

        /**登录用户id*/
        fun signInUserId(userId: String?) {
            //账号来源, 账号id
            if (userId.isNullOrEmpty()) {
                signOffUserId()
            } else {
                MobclickAgent.onProfileSignIn(userId)
            }
        }

        /**登出用户id*/
        fun signOffUserId() {
            MobclickAgent.onProfileSignOff()
        }
    }

    /**请主动赋值*/
    var isDebug: Boolean = false

    //https://developer.umeng.com/docs/193624/detail/194590

    /**请主动赋值*/
    var context: Context? = null

    /**
     * 注意: 即使您已经在AndroidManifest.xml中配置过appkey和channel值，也需要在App代码中调
     * 用初始化接口（如需要使用AndroidManifest.xml中配置好的appkey和channel值，
     * UMConfigure.init调用中appkey和channel参数请置为null）。
     */
    var appKey: String? = null

    /**聚道*/
    var channel: String? = "Default"

    /**设备类型，UMConfigure.DEVICE_TYPE_PHONE为手机、UMConfigure.DEVICE_TYPE_BOX为盒子，默认为手机*/
    var deviceType: Int = UMConfigure.DEVICE_TYPE_PHONE

    /**Push推送业务的secret*/
    var pushSecret: String? = null

    /**
     * 设置日志加密
     * https://developer.umeng.com/docs/66632/detail/101814#h1-u521Du59CBu5316u53CAu901Au7528u63A5u53E32
     * */
    var encryptEnabled: Boolean = true

    /**子进程是否支持自定义事件统计。*/
    var processEvent: Boolean = true

    /**界面监听, SDK会有默认的*/
    var activityLifecycleCallbacks: Application.ActivityLifecycleCallbacks? = null

    /**https://developer.umeng.com/docs/119267/detail/118588*/
    var pageMode: MobclickAgent.PageMode = MobclickAgent.PageMode.AUTO

    /**1: 预初始化*/
    fun preInit() {
        UMConfigure.setLogEnabled(isDebug)
        UMConfigure.setEncryptEnabled(encryptEnabled)

        if (_check()) {
            UMConfigure.preInit(context, appKey, channel)
        }
    }

    /**2: 初始化*/
    fun init() {
        if (_check()) {
            UMConfigure.init(context, appKey, channel, deviceType, pushSecret)

            UMConfigure.setProcessEvent(processEvent)

            // 选用AUTO页面采集模式
            MobclickAgent.setPageCollectionMode(pageMode)

            //MobclickAgent.onProfileSignIn()
            //MobclickAgent.onProfileSignOff()

            val _context = context
            if (_context is Application && activityLifecycleCallbacks != null) {
                _context.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
            }
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

fun String.umengEvent() {
    UM.event(this)
}

fun String.umengEventValue() {
    UM.event(this, 1)
}

fun String.umengEventValue(action: HashMap<String, String>.() -> Unit) {
    UM.event(this, 1, HashMap<String, String>().apply(action))
}

fun String.umengEventObject(map: Map<String, Any>) {
    UM.event(this, map)
}

/**DSL*/
fun String.umengEventObject(action: HashMap<String, Any>.() -> Unit) {
    UM.event(this, HashMap<String, Any>().apply(action))
}