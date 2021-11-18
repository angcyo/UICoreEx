package com.angcyo.tim

import android.media.MediaMetadataRetriever
import android.net.Uri
import com.angcyo.library.L
import com.angcyo.library.ex.fileName
import com.angcyo.library.ex.getPathFromUri
import com.angcyo.library.ex.isFileExist
import com.angcyo.library.ex.save
import com.angcyo.tim.bean.MessageInfoBean
import com.angcyo.tim.helper.*
import com.angcyo.tim.util.TimConfig.MSG_PAGE_COUNT
import com.tencent.imsdk.v2.*
import com.tencent.imsdk.v2.V2TIMMessageListGetOption.*

/**
 * 消息收发
 * https://cloud.tencent.com/document/product/269/44489
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/09
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
object TimMessage {

    /**tim 核心类*/
    val timManager = V2TIMManager.getInstance()

    /**消息管理*/
    val messageManager = V2TIMManager.getMessageManager()

    //<editor-fold desc="发送消息">

    /**发送文本消息
     * 即普通的文字消息，该类消息会经过即时通信 IM 的敏感词过滤，发送包含的敏感词消息时会报80001错误码。
     * */
    fun sendTextMessage(
        userId: String,
        text: String,
        callback: (V2TIMMessage?, TimSdkException?) -> Unit
    ) {
        timManager.sendC2CTextMessage(text, userId,
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
        timManager.sendC2CCustomMessage(data, userId,
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
        messageManager.sendMessage(
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

    /**返回一个图片消息*/
    fun imageMessage(imagePath: String) =
        messageManager.createImageMessage(imagePath)

    fun imageMessageBean(imagePath: String): MessageInfoBean =
        imageMessage(imagePath).toMyselfImageMessageInfoBean(imagePath)

    /**返回一个视频消息
     *
     * [type]]	视频类型，如 mp4 mov 等
     * [duration]]	视频时长，单位 s
     * [snapshotPath]]	视频封面图片路径
     * */
    fun videoMessage(
        videoFilePath: String,
        duration: Int,
        snapshotPath: String,
        type: String = "mp4"
    ) = messageManager.createVideoMessage(videoFilePath, type, duration, snapshotPath)

    fun videoMessageBean(videoFilePath: String): MessageInfoBean? {
        val mmr = MediaMetadataRetriever()
        try {
            mmr.setDataSource(videoFilePath)
            val duration =
                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toIntOrNull()
                    ?: 0 //时长(毫秒)
            val bitmap =
                mmr.getFrameAtTime(0, MediaMetadataRetriever.OPTION_NEXT_SYNC) //缩略图
            if (bitmap == null) {
                L.e("buildVideoMessage() bitmap is null")
                return null
            }
            val imgPath: String = bitmap.save(quality = 100).absolutePath
            return videoMessage(
                videoFilePath,
                duration / 1000,
                imgPath
            ).toMyselfVideoMessageInfoBean(
                videoFilePath,
                imgPath
            )
        } catch (ex: Exception) {
            L.e("MediaMetadataRetriever exception $ex")
        } finally {
            mmr.release()
        }

        return null
    }

    /**返回一个语音消息
     * 2.6 创建语音消息（语音最大支持 28 MB）
     * [duration] 秒*/
    fun soundMessage(soundPath: String, duration: Int) =
        messageManager.createSoundMessage(soundPath, duration)

    fun soundMessageBean(soundPath: String, duration: Int) =
        soundMessage(soundPath, duration).toMyselfSoundMessageInfoBean(soundPath)

    /**返回一个文件消息*/
    fun fileMessage(filePath: String, fileName: String) =
        messageManager.createFileMessage(filePath, fileName)


    /**返回一个文件消息*/
    fun fileMessage(fileUri: Uri): V2TIMMessage? {
        val filePath = fileUri.getPathFromUri()
        return if (filePath.isFileExist()) {
            fileMessage(filePath!!, filePath.fileName()!!)
        } else {
            null
        }
    }

    fun fileMessageBean(fileUri: Uri): MessageInfoBean? {
        val filePath = fileUri.getPathFromUri()
        return fileMessageBean(filePath)
    }

    fun fileMessageBean(filePath: String?): MessageInfoBean? {
        return if (filePath.isFileExist()) {
            messageManager.createFileMessage(filePath!!, filePath.fileName()!!)
                ?.toMyselfFileMessageInfoBean(filePath)
        } else {
            null
        }
    }

    /**返回一个文本消息
     * [atUserList] @的用户id列表
     * 需要 @ 的用户列表，如果需要 @ALL，请传入 AT_ALL_TAG 常量字符串。 举个例子，假设该条文本消息希望@提醒 denny 和 lucy 两个用户，同时又希望@所有人，atUserList 传 ["denny","lucy",AT_ALL_TAG]
     * https://im.sdk.qcloud.com/doc/zh-cn/classcom_1_1tencent_1_1imsdk_1_1v2_1_1V2TIMMessageManager.html#a09a259ceb314754dd267533597138391
     * */
    fun textMessage(message: String?, atUserList: List<String>? = null) =
        if (atUserList.isNullOrEmpty()) {
            messageManager.createTextMessage(message)
        } else {
            messageManager.createTextAtMessage(message, atUserList)
        }

    fun textMessageBean(message: String?): MessageInfoBean =
        textMessage(message).toMyselfMessageInfoBean(message)

    /**
     * 创建地理位置消息
     * https://im.sdk.qcloud.com/doc/zh-cn/classcom_1_1tencent_1_1imsdk_1_1v2_1_1V2TIMMessageManager.html#a67cebe27192392080fc80a86c80a4321*/
    fun locationMessageBean(desc: String?, longitude: Double, latitude: Double): MessageInfoBean? =
        messageManager.createLocationMessage(
            desc,
            longitude,
            latitude
        )?.toMyselfLocationMessageInfoBean()

    //</editor-fold desc="发送消息">

    //<editor-fold desc="监听消息">

    /**监听简单的消息*/
    fun listenerSimpleMessage() {
        timManager.addSimpleMsgListener(object : V2TIMSimpleMsgListener() {
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
        messageManager
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
        messageManager.revokeMessage(message, object : V2TIMCallback {
            override fun onSuccess() {
                callback(null)
            }

            override fun onError(code: Int, desc: String?) {
                callback(TimSdkException(code, desc))
            }
        })
    }

    /**获取历史消息高级接口
     *
     * [option] 拉取消息选项设置，可以设置从云端、本地拉取更老或更新的消息
     *
     * [V2TIM_GET_CLOUD_OLDER_MSG] 按照时间 从大到小排序, [2021-11-18 13:52:51]->[2021-11-17 16:56:00] 排序
     * [V2TIM_GET_CLOUD_NEWER_MSG] 按照时间 从小到大排列, [2021-11-18 13:46:51]->[2021-11-18 13:52:51] 排序
     * https://im.sdk.qcloud.com/doc/zh-cn/classcom_1_1tencent_1_1imsdk_1_1v2_1_1V2TIMMessageListGetOption.html
     */
    fun getHistoryMessageList(
        option: V2TIMMessageListGetOption,
        callback: (List<V2TIMMessage>?, TimSdkException?) -> Unit
    ) {
        messageManager.getHistoryMessageList(
            option,
            object : V2TIMValueCallback<List<V2TIMMessage>> {
                override fun onSuccess(list: List<V2TIMMessage>?) {
                    callback(list, null)
                }

                override fun onError(code: Int, desc: String?) {
                    callback(null, TimSdkException(code, desc))
                }

            })
    }

    /**获取单聊/群聊的历史消息高级接口,
     * 拉取历史消息的时候不会把 lastMsg 返回，需要手动添加上
     * [getType] 拉取类型，取值为 V2TIM_GET_CLOUD_OLDER_MSG，V2TIM_GET_CLOUD_NEWER_MSG，V2TIM_GET_LOCAL_OLDER_MSG，V2TIM_GET_LOCAL_NEWER_MSG
     * https://im.sdk.qcloud.com/doc/zh-cn/classcom_1_1tencent_1_1imsdk_1_1v2_1_1V2TIMMessageManager.html#a97fe2d6a7bab8f45b758f84df48c0b12*/
    fun getHistoryMessageList(
        chatId: String?,
        isGroup: Boolean,
        lastMsg: V2TIMMessage? = null,
        getType: Int = V2TIM_GET_CLOUD_OLDER_MSG,
        count: Int = MSG_PAGE_COUNT,
        callback: (List<V2TIMMessage>?, TimSdkException?) -> Unit
    ) {
        val option = V2TIMMessageListGetOption()
        option.count = count
        option.getType = getType
        option.lastMsg = lastMsg
        if (isGroup) {
            option.groupID = chatId
        } else {
            option.userID = chatId
        }
        getHistoryMessageList(option, callback)
    }

    /**获取当前[msg]消息前后的历史消息*/
    fun getHistoryMessageBothList(
        chatId: String?,
        isGroup: Boolean,
        msg: V2TIMMessage,
        count: Int = MSG_PAGE_COUNT,
        callback: (List<V2TIMMessage>?, TimSdkException?) -> Unit
    ) {
        val option = V2TIMMessageListGetOption()
        option.count = count
        option.getType = V2TIM_GET_CLOUD_OLDER_MSG //第一次先获取当前消息的云端老数据
        option.lastMsg = msg
        if (isGroup) {
            option.groupID = chatId
        } else {
            option.userID = chatId
        }
        val result = mutableListOf<V2TIMMessage>()
        getHistoryMessageList(option) { list, timSdkException ->
            timSdkException?.let {
                result.add(msg)
                callback(result, it)
            }
            list?.let {
                //时间大->小
                result.add(msg)
                result.addAll(it)

                option.getType = V2TIM_GET_CLOUD_NEWER_MSG  //第二次再获取当前消息的云端新数据
                getHistoryMessageList(option) { list, timSdkException ->
                    //end
                    timSdkException?.let {
                        callback(result, it)
                    }
                    list?.let {
                        //时间小->大
                        result.addAll(0, it.reversed())
                        callback(result, null)
                    }
                }
            }
        }
    }

    /**获取单聊历史消息
     * 历史消息的注意事项
     * 历史消息存储时长如下：
     * 体验版：免费存储7天，不支持延长
     * 专业版：免费存储7天，支持延长
     * 旗舰版：免费存储30天，支持延长
     *
     * [count] 拉取消息的个数，不宜太多，会影响消息拉取的速度，这里建议一次拉取 20 个
     * [lastMsg] 获取消息的起始消息，如果传 null，起始消息为会话的最新消息
     *
     * https://cloud.tencent.com/document/product/269/44489
     * */
    fun getC2CHistoryMessageList(
        userId: String?,
        lastMsg: V2TIMMessage? = null,
        count: Int = MSG_PAGE_COUNT,
        callback: (List<V2TIMMessage>?, TimSdkException?) -> Unit
    ) {
        messageManager.getC2CHistoryMessageList(userId, count, lastMsg,
            object : V2TIMValueCallback<List<V2TIMMessage>> {
                override fun onSuccess(list: List<V2TIMMessage>?) {
                    callback(list, null)
                }

                override fun onError(code: Int, desc: String?) {
                    callback(null, TimSdkException(code, desc))
                }
            })
    }

    /**获取群聊历史消息*/
    fun getGroupHistoryMessageList(
        groupId: String?,
        lastMsg: V2TIMMessage? = null,
        count: Int = MSG_PAGE_COUNT,
        callback: (List<V2TIMMessage>?, TimSdkException?) -> Unit
    ) {
        messageManager.getGroupHistoryMessageList(groupId, count, lastMsg,
            object : V2TIMValueCallback<List<V2TIMMessage>> {
                override fun onSuccess(list: List<V2TIMMessage>?) {
                    callback(list, null)
                }

                override fun onError(code: Int, desc: String?) {
                    callback(null, TimSdkException(code, desc))
                }
            })

        //messageManager.getGroupHistoryMessageList()
    }

    //</editor-fold desc="消息操作">
}

/**获取旧的消息*/
fun Int.isGetOldMsg(): Boolean =
    this == V2TIM_GET_CLOUD_OLDER_MSG || this == V2TIM_GET_LOCAL_OLDER_MSG

/**获取新的消息*/
fun Int.isGetNewMsg(): Boolean =
    this == V2TIM_GET_CLOUD_NEWER_MSG || this == V2TIM_GET_LOCAL_NEWER_MSG