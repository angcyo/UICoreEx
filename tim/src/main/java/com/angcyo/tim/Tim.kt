package com.angcyo.tim

import android.content.Context
import com.angcyo.core.vmApp
import com.angcyo.http.rx.doBack
import com.angcyo.library.app
import com.angcyo.library.ex.isDebug
import com.angcyo.tim.model.ChatModel
import com.angcyo.tim.model.ConversationModel
import com.angcyo.tim.util.FaceManager
import com.tencent.imsdk.v2.*


/**
 * 腾讯即时通信IM SDK
 *
 * [错误码] https://cloud.tencent.com/document/product/269/1671
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/09
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
object Tim {

    /**sdk登录的用户*/
    val loginUer: String
        get() = V2TIMManager.getInstance().loginUser

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

        //init face
        doBack {
            FaceManager.init()
        }
    }

    /**登录
     * [userId] 用户名, 建议只包含大小写英文字母（a-z、A-Z）、数字（0-9）、下划线（_）和连词符（-），长度最大不超过32字节。
     * [userSig] 从业务服务器获取的 userSig
     * */
    fun login(userId: String, userSig: String, callback: ((TimSdkException?) -> Unit)? = null) {
        V2TIMManager.getInstance().login(userId, userSig, object : V2TIMCallback {
            override fun onSuccess() {
                callback?.invoke(null)

                //会话
                vmApp<ConversationModel>().apply {
                    listenerUnreadCount()
                    listenerConversation()
                    fetchConversationList()
                }

                //消息
                vmApp<ChatModel>().apply {
                    listenerMessage()
                }
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

    //<editor-fold desc="操作">

    /**更新自己的信息
     * [name] 昵称*/
    fun setSelfInfo(
        name: String?,
        faceUrl: String?,
        callback: ((TimSdkException?) -> Unit)? = null
    ) {
        setSelfInfo(V2TIMUserFullInfo().apply {
            setNickname(name)
            this.faceUrl = faceUrl
            //customInfo
            //gender
            //role
            //level
            //level
            //allowType
        }, callback)
    }

    /**
     * 修改个人资料
     * https://im.sdk.qcloud.com/doc/zh-cn/classcom_1_1tencent_1_1imsdk_1_1v2_1_1V2TIMManager.html#af004ab2f1d1458de354883f1995b678a
     * */
    fun setSelfInfo(info: V2TIMUserFullInfo, callback: ((TimSdkException?) -> Unit)? = null) {
        //info.nickName
        //info.faceUrl
        //info.role
        //info.gender
        //info.customInfo

        V2TIMManager.getInstance().setSelfInfo(info, object : V2TIMCallback {
            override fun onSuccess() {
                callback?.invoke(null)
            }

            override fun onError(code: Int, desc: String?) {
                callback?.invoke(TimSdkException(code, desc))
            }
        })

        //V2TIMManager.getFriendshipManager().setFriendInfo()
    }

    //</editor-fold desc="操作">


    //<editor-fold desc="会话相关">

    /**获取会话列表
     * [nextSeq] 分页拉取的游标，第一次默认取传 0，后续分页拉传上一次分页拉取成功回调里的 nextSeq
     * [count] 分页拉取的个数，一次分页拉取不宜太多，会影响拉取的速度，建议每次拉取 100 个会话
     * https://im.sdk.qcloud.com/doc/zh-cn/classcom_1_1tencent_1_1imsdk_1_1v2_1_1V2TIMConversationManager.html#a1bb5ba2beecb4f68146e7f664124fd8b
     * */
    fun getConversationList(
        nextSeq: Long = 0,
        count: Int = 100,
        callback: (V2TIMConversationResult?, TimSdkException?) -> Unit
    ) {
        V2TIMManager.getConversationManager().getConversationList(
            nextSeq,
            count,
            object : V2TIMValueCallback<V2TIMConversationResult> {
                override fun onSuccess(v2TIMConversationResult: V2TIMConversationResult?) {
                    //v2TIMConversationResult?.isFinished
                    //v2TIMConversationResult?.conversationList
                    //v2TIMConversationResult?.nextSeq
                    //v2TIMConversationResult?.conversationList?.firstOrNull()?.lastMessage
                    //v2TIMConversationResult?.conversationList?.firstOrNull()?.showName

                    callback(v2TIMConversationResult, null)
                }

                override fun onError(code: Int, desc: String?) {
                    callback(null, TimSdkException(code, desc))
                }
            })
    }


    //</editor-fold desc="会话相关">


}