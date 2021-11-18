package com.angcyo.tim.model

import com.angcyo.core.lifecycle.LifecycleViewModel
import com.angcyo.http.base.toJson
import com.angcyo.library.L
import com.angcyo.tim.helper.toMessageInfoBean
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
    var selfFaceUrlData = vmDataNull<String>()

    /**无头像时, 需要绘制的昵称文本*/
    var selfShowNameData = vmDataNull<String>()

    /**新消息通知*/
    var newMessageData = vmDataOnce<V2TIMMessage>()

    //</editor-fold desc="数据监听">

    /**聊天管理*/
    val messageManager = V2TIMManager.getMessageManager()

    //<editor-fold desc="监听">

    val _messageListener = object : V2TIMAdvancedMsgListener() {

        override fun onRecvNewMessage(msg: V2TIMMessage?) {
            L.i("收到新消息:${msg?.msgID}:${msg?.elemType}")
            L.d(msg?.toMessageInfoBean()?.toJson())
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

    //</editor-fold desc="监听">

}