package com.angcyo.tim

import com.tencent.imsdk.v2.*

/**
 * 消息收发
 * https://cloud.tencent.com/document/product/269/44489
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/09
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
object TimMessage {

    //<editor-fold desc="发送消息">

    /**发送文本消息
     * 即普通的文字消息，该类消息会经过即时通信 IM 的敏感词过滤，发送包含的敏感词消息时会报80001错误码。
     * */
    fun sendTextMessage(
        userId: String,
        text: String,
        callback: (V2TIMMessage?, TimSdkException?) -> Unit
    ) {
        V2TIMManager.getInstance().sendC2CTextMessage(text, userId,
            object : V2TIMValueCallback<V2TIMMessage> {
                override fun onSuccess(v2TIMMessage: V2TIMMessage) {
                    callback(v2TIMMessage, null)
                }

                override fun onError(code: Int, desc: String?) {
                    callback(null, TimSdkException(code, desc))
                }
            })
    }

    /**发送自定义消息
     * 即一段二进制 buffer，通常用于传输您应用中的自定义信令，内容不会经过敏感词过滤。
     * */
    fun sendCustomMessage(
        userId: String,
        data: ByteArray,
        callback: (V2TIMMessage?, TimSdkException?) -> Unit
    ) {
        V2TIMManager.getInstance().sendC2CCustomMessage(data, userId,
            object : V2TIMValueCallback<V2TIMMessage> {
                override fun onSuccess(v2TIMMessage: V2TIMMessage) {
                    callback(v2TIMMessage, null)
                }

                override fun onError(code: Int, desc: String?) {
                    callback(null, TimSdkException(code, desc))
                }
            })
    }

    /**
     * [userId] 消息接收者的 userID, 如果是发送 C2C 单聊消息，只需要指定 receiver 即可。
     * [groupId] 	目标群组 ID，如果是发送群聊消息，只需要指定 groupID 即可。
     * [priority] 	消息优先级，仅针对群聊消息有效。请把重要消息设置为高优先级（比如红包、礼物消息），高频且不重要的消息设置为低优先级（比如点赞消息）。
     * [onlineUserOnly] 是否只有在线用户才能收到，如果设置为 true ，接收方历史消息拉取不到，常被用于实现“对方正在输入”或群组里的非重要提示等弱提示功能，该字段不支持 AVChatRoom。
     * https://im.sdk.qcloud.com/doc/zh-cn/classcom_1_1tencent_1_1imsdk_1_1v2_1_1V2TIMMessageManager.html#a28e01403acd422e53e999f21ec064795
     * */
    fun sendMessage(
        message: V2TIMMessage,
        userId: String? = null,
        groupId: String? = null,
        priority: Int = V2TIMMessage.V2TIM_PRIORITY_DEFAULT,
        onlineUserOnly: Boolean = false,
        offlinePushInfo: V2TIMOfflinePushInfo? = null,
        callback: (V2TIMMessage?, TimSdkException?) -> Unit
    ) {
        V2TIMManager.getMessageManager().sendMessage(
            message,
            userId,
            groupId,
            priority,
            onlineUserOnly,
            offlinePushInfo,
            object : V2TIMSendCallback<V2TIMMessage?> {
                override fun onSuccess(v2TIMMessage: V2TIMMessage?) {
                    callback(message, null)
                }

                override fun onError(code: Int, desc: String?) {
                    callback(null, TimSdkException(code, desc))
                }

                override fun onProgress(progress: Int) {

                }
            })
    }

    fun imageMessage(imagePath: String) =
        V2TIMManager.getMessageManager().createImageMessage(imagePath)

    fun videoMessage(videoFilePath: String, type: String, duration: Int, snapshotPath: String) =
        V2TIMManager.getMessageManager()
            .createVideoMessage(videoFilePath, type, duration, snapshotPath)

    fun soundMessage(soundPath: String, duration: Int) =
        V2TIMManager.getMessageManager()
            .createSoundMessage(soundPath, duration)

    fun fileMessage(filePath: String, fileName: String) =
        V2TIMManager.getMessageManager()
            .createFileMessage(filePath, fileName)


    //</editor-fold desc="发送消息">

    //<editor-fold desc="监听消息">

    /**监听简单的消息*/
    fun listenerSimpleMessage() {
        V2TIMManager.getInstance().addSimpleMsgListener(object : V2TIMSimpleMsgListener() {
            override fun onRecvC2CTextMessage(
                msgID: String?,
                sender: V2TIMUserInfo?,
                text: String?
            ) {
                super.onRecvC2CTextMessage(msgID, sender, text)
            }

            override fun onRecvC2CCustomMessage(
                msgID: String?,
                sender: V2TIMUserInfo?,
                customData: ByteArray?
            ) {
                super.onRecvC2CCustomMessage(msgID, sender, customData)
            }
        })
    }

    /**监听消息*/
    fun listenerMessage() {
        V2TIMManager.getMessageManager()
            .addAdvancedMsgListener(object : V2TIMAdvancedMsgListener() {
                override fun onRecvNewMessage(msg: V2TIMMessage?) {
                    super.onRecvNewMessage(msg)
                }

                override fun onRecvMessageRevoked(msgID: String?) {
                    super.onRecvMessageRevoked(msgID)
                }
            })

    }

    //</editor-fold desc="监听消息">

    //<editor-fold desc="消息操作">

    /**撤销消息*/
    fun revokeMessage(message: V2TIMMessage, callback: (TimSdkException?) -> Unit) {
        V2TIMManager.getMessageManager().revokeMessage(message, object : V2TIMCallback {
            override fun onSuccess() {
                callback(null)
            }

            override fun onError(code: Int, desc: String?) {
                callback(TimSdkException(code, desc))
            }
        })
    }

    /**获取历史消息
     * 历史消息的注意事项
     * 历史消息存储时长如下：
     * 体验版：免费存储7天，不支持延长
     * 专业版：免费存储7天，支持延长
     * 旗舰版：免费存储30天，支持延长
     * https://cloud.tencent.com/document/product/269/44489
     * */
    fun getHistoryMessageList(
        userId: String,
        count: Int = 20,
        lastMsg: V2TIMMessage? = null,
        callback: (List<V2TIMMessage>?, TimSdkException?) -> Unit
    ) {
        V2TIMManager.getMessageManager().getC2CHistoryMessageList(userId, count, lastMsg,
            object : V2TIMValueCallback<List<V2TIMMessage>> {
                override fun onSuccess(list: List<V2TIMMessage>?) {
                    callback(list, null)
                }

                override fun onError(code: Int, desc: String?) {
                    callback(null, TimSdkException(code, desc))
                }

            })
    }

    //</editor-fold desc="消息操作">
}