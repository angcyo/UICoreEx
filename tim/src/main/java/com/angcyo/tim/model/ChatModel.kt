package com.angcyo.tim.model

import com.angcyo.core.lifecycle.LifecycleViewModel
import com.angcyo.library.L
import com.angcyo.viewmodel.vmDataNull
import com.angcyo.viewmodel.vmDataOnce
import com.tencent.imsdk.v2.*

/**
 * 聊天数据共享model
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/12
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class ChatModel : LifecycleViewModel() {

    //<editor-fold desc="数据监听">

    /**自己的头像全路径url*/
    val selfFaceUrlData = vmDataNull<String>()

    /**无头像时, 需要绘制的昵称文本*/
    val selfShowNameData = vmDataNull<String>()

    /**新消息通知, 不会保存新消息*/
    val newMessageData = vmDataOnce<V2TIMMessage>()

    /**是否连接上了sdk服务器*/
    val sdkConnectData = vmDataNull(true)

    /**被踢提醒*/
    val kickedOfflineData = vmDataOnce(false)

    //</editor-fold desc="数据监听">

    /**聊天管理*/
    val messageManager = V2TIMManager.getMessageManager()

    //<editor-fold desc="监听">

    val _messageListener = object : V2TIMAdvancedMsgListener() {

        override fun onRecvNewMessage(msg: V2TIMMessage?) {
            L.i("收到新消息:${msg?.msgID}:${msg?.elemType}")
            //L.d(msg?.toMessageInfoBean()?.toJson())
            //val messageInfoBean = msg?.toMessageInfoBean()
            if (msg?.groupID.isNullOrEmpty()) {
                //C2C单聊
            } else {
                //群聊
            }
            newMessageData.value = msg
        }

        /**收到 C2C 消息已读回执*/
        override fun onRecvC2CReadReceipt(receiptList: MutableList<V2TIMMessageReceipt>) {
            L.i("消息已读通知:$receiptList")
        }

        /**收到消息撤回的通知*/
        override fun onRecvMessageRevoked(msgID: String) {
            L.i("撤回消息:$msgID")
        }

        /**消息内容被修改（第三方服务回调修改了消息内容）*/
        override fun onRecvMessageModified(msg: V2TIMMessage) {
            L.i("消息被修改:$msg")
        }
    }

    /**添加高级消息的事件监听器
     * https://im.sdk.qcloud.com/doc/zh-cn/classcom_1_1tencent_1_1imsdk_1_1v2_1_1V2TIMMessageManager.html#aaccdec10b9fbee5e43eaf908e359c823*/
    fun listenerMessage() {
        messageManager.addAdvancedMsgListener(_messageListener)
    }

    fun removeListenerMessage() {
        messageManager.removeAdvancedMsgListener(_messageListener)
    }

    /** 监听好友信息
     * https://im.sdk.qcloud.com/doc/zh-cn/classcom_1_1tencent_1_1imsdk_1_1v2_1_1V2TIMFriendshipListener.html#a69aac3e5529bf0d71ed59f7ac2471478
     * */
    fun listenerFriend() {
        V2TIMManager.getFriendshipManager().addFriendListener(object : V2TIMFriendshipListener() {
            override fun onFriendInfoChanged(infoList: MutableList<V2TIMFriendInfo>) {
                L.i("好友信息修改:$infoList")
            }
        })
    }

    /**添加群组监听器*/
    fun listenerGroup() {
        V2TIMManager.getInstance().addGroupListener(object : V2TIMGroupListener() {

        })
    }

    val _sdkListener = object : V2TIMSDKListener() {
        //SDK 正在连接到腾讯云服务器
        override fun onConnecting() {
            super.onConnecting()
            L.d("TIM服务器连接中...")
        }

        //SDK 已经成功连接到腾讯云服务器
        override fun onConnectSuccess() {
            super.onConnectSuccess()
            L.i("TIM服务器连接成功.")
            sdkConnectData.value = true
        }

        //SDK 连接腾讯云服务器失败
        override fun onConnectFailed(code: Int, error: String?) {
            super.onConnectFailed(code, error)
            L.w("TIM服务器连接失败:$code:$error")
            sdkConnectData.value = false
        }

        //当前用户被踢下线，此时可以 UI 提示用户，并再次调用 V2TIMManager 的 login() 函数重新登录。
        override fun onKickedOffline() {
            super.onKickedOffline()
            L.w("TIM服务器被踢下线!")
            kickedOfflineData.value = true
        }

        //在线时票据过期：此时您需要生成新的 userSig 并再次调用 V2TIMManager 的 login() 函数重新登录。
        override fun onUserSigExpired() {
            super.onUserSigExpired()
        }

        //登录用户的资料发生了更新
        override fun onSelfInfoUpdated(info: V2TIMUserFullInfo?) {
            super.onSelfInfoUpdated(info)
            L.i(info)
        }
    }

    /**添加 IM 监听
     * https://im.sdk.qcloud.com/doc/zh-cn/classcom_1_1tencent_1_1imsdk_1_1v2_1_1V2TIMManager.html#a2f0297e96d365013e7923275ce2a5d4e*/
    fun listenerSdk() {
        V2TIMManager.getInstance().addIMSDKListener(_sdkListener)
    }

    fun removeListenerSdk() {
        V2TIMManager.getInstance().removeIMSDKListener(_sdkListener)
    }

    //</editor-fold desc="监听">

    override fun reset() {
        super.reset()
    }

    override fun cancel() {
        super.cancel()
    }

    override fun release() {
        selfFaceUrlData.postValue(null)
        selfShowNameData.postValue(null)

        removeListenerMessage()
        removeListenerSdk()
    }
}