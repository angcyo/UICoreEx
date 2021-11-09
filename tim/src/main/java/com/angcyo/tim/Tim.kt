package com.angcyo.tim

import android.content.Context
import com.angcyo.library.app
import com.angcyo.library.ex.isDebug
import com.tencent.imsdk.v2.V2TIMCallback
import com.tencent.imsdk.v2.V2TIMManager
import com.tencent.imsdk.v2.V2TIMSDKConfig


/**
 * 腾讯即时通信IM SDK
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/09
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
object Tim {

    /**初始化
     * https://cloud.tencent.com/document/product/269/44477*/
    fun init(appId: Int, context: Context = app()) {
        // 1. 从 IM 控制台获取应用 SDKAppID，详情请参考 SDKAppID。
        // 2. 初始化 config 对象
        val config = V2TIMSDKConfig()
        // 3. 指定 log 输出级别，详情请参考 SDKConfig。
        config.logLevel = if (isDebug()) {
            V2TIMSDKConfig.V2TIM_LOG_INFO
        } else {
            V2TIMSDKConfig.V2TIM_LOG_NONE
        }
        //IM SDK 的日志在4.8.50版本之前默认存储于 /sdcard/tencenet/imsdklogs/应用包名 目录下，
        // 4.8.50及之后的版本存储于 /sdcard/Android/data/包名/files/log/tencent/imsdk 目录下。


        // 4. 初始化 SDK 并设置 V2TIMSDKListener 的监听对象。
        // initSDK 后 SDK 会自动连接网络，网络连接状态可以在 V2TIMSDKListener 回调里面监听。
        V2TIMManager.getInstance().initSDK(context, appId, config)
    }

    /**登录
     * [userId] 用户名, 建议只包含大小写英文字母（a-z、A-Z）、数字（0-9）、下划线（_）和连词符（-），长度最大不超过32字节。
     * [userSig] 从业务服务器获取的 userSig
     * */
    fun login(userId: String, userSig: String, callback: ((TimSdkException?) -> Unit)? = null) {
        V2TIMManager.getInstance().login(userId, userSig, object : V2TIMCallback {
            override fun onSuccess() {
                callback?.invoke(null)
            }

            override fun onError(code: Int, desc: String?) {
                callback?.invoke(TimSdkException(code, desc))
            }
        })
    }

    /**登出*/
    fun logout(callback: ((TimSdkException?) -> Unit)? = null) {
        V2TIMManager.getInstance().logout(object : V2TIMCallback {
            override fun onSuccess() {
                callback?.invoke(null)
            }

            override fun onError(code: Int, desc: String?) {
                callback?.invoke(TimSdkException(code, desc))
            }
        })
    }

}