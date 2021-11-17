package com.angcyo.tim.bean

import android.net.Uri
import com.angcyo.library.ex.*
import com.angcyo.tim.util.TimConfig
import com.tencent.imsdk.v2.*
import java.io.File

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/10
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class MessageInfoBean {

    companion object {

        /** 消息未读状态 */
        const val MSG_STATUS_READ = 0x111

        /** 消息删除状态 */
        const val MSG_STATUS_DELETE = 0x112

        /** 消息撤回状态 */
        const val MSG_STATUS_REVOKE = 0x113

        /** 消息正常状态 */
        const val MSG_STATUS_NORMAL = 0

        /** 消息发送中状态 */
        const val MSG_STATUS_SENDING = 1

        /** 消息发送成功状态 */
        const val MSG_STATUS_SEND_SUCCESS = 2

        /** 消息发送失败状态 */
        const val MSG_STATUS_SEND_FAIL = 3

        /** 消息内容下载中状态 */
        const val MSG_STATUS_DOWNLOADING = 4

        /** 消息内容未下载状态 */
        const val MSG_STATUS_UN_DOWNLOAD = 5

        /** 消息内容已下载状态 */
        const val MSG_STATUS_DOWNLOADED = 6
    }

    /**消息的id*/
    var messageId: String = uuid()

    /**TIM SDK 原始的消息数据*/
    var message: V2TIMMessage? = null

    /**消息需要显示的文本内容*/
    var content: String? = null

    /**数据路径, 比如:语音文件路径等
     * 如果是视频消息, 此处是视频缩略图的本地路径.*/
    var dataPath: String? = null

    /**数据的uri, 如视频的本地地址, 图片的原图本地地址, 音频的本地地址
     * 使用[String]类型, 而不是[Uri]类型, 方便传输数据
     * 在部分消息类型下, 会等同于[dataPath]*/
    var dataUri: String? = null

    /**图片的宽度, 视频的缩略图*/
    var imageWidth: Int = -1

    /**图片的高度*/
    var imageHeight: Int = -1

    /**消息的发送状态*/
    var status: Int = MSG_STATUS_NORMAL

    /**消息的下载状态*/
    var downloadStatus: Int = MSG_STATUS_NORMAL

    /**消息的时间, 毫秒*/
    var timestamp: Long = 0

    /**消息的发送者*/
    var fromUser: String? = null
}

val MessageInfoBean.isGroup: Boolean
    get() = !(message?.groupID.isNullOrEmpty())

/**是否是自己发送的消息*/
val MessageInfoBean.isSelf: Boolean
    get() = sender == V2TIMManager.getInstance().loginUser

val MessageInfoBean.messageId: String?
    get() = message?.msgID

/**发送者的头像*/
val MessageInfoBean.faceUrl: String?
    get() = message?.faceUrl

/**消息对方是否已读（只有 C2C 消息有效）*/
val MessageInfoBean.isPeerRead: Boolean
    get() = message?.isPeerRead == true

/**消息的类型*/
val MessageInfoBean.msgType: Int
    get() = message?.elemType ?: V2TIMMessage.V2TIM_ELEM_TYPE_NONE

/**获取消息发送者 userID*/
val MessageInfoBean.sender: String
    get() {
        val sender = fromUser ?: message?.sender
        return if (sender.isNullOrEmpty()) {
            V2TIMManager.getInstance().loginUser
        } else {
            sender
        }
    }

/**获取聊天中, 需要显示的用户名*/
val MessageInfoBean.showUserName: String?
    get() {
        var result: String? = null

        message?.apply {
            result = notEmptyOf(nameCard, friendRemark, nickName, sender)
        }

        return result
    }

/**
 * 获取图片在本地的原始路径 (可能是沙盒中的路径)
 * @param messageInfo 图片消息元组
 * @return 图片原始路径，如果不存在返回 null
 */
val MessageInfoBean.localImagePath: String?
    get() {
        if (!isSelf) {
            return null
        }
        val message: V2TIMMessage? = message
        if (message == null || message.elemType != V2TIMMessage.V2TIM_ELEM_TYPE_IMAGE) {
            return null
        }
        val imageElem = message.imageElem ?: return null
        val path = imageElem.path
        val file = File(path)
        return if (file.exists()) {
            path
        } else null
    }

/**获取原图地址*/
val MessageInfoBean.originImagePath: String?
    get() {
        val v2TIMMessage: V2TIMMessage = message ?: return null
        val v2TIMImageElem = v2TIMMessage.imageElem ?: return null
        var localImgPath: String? = localImagePath
        if (localImgPath == null) {
            var originUUID: String? = null
            for (image in v2TIMImageElem.imageList) {
                if (image.type == V2TIMImageElem.V2TIM_IMAGE_TYPE_ORIGIN) {
                    originUUID = image.uuid
                    break
                }
            }
            val originPath: String =
                TimConfig.generateImagePath(originUUID, V2TIMImageElem.V2TIM_IMAGE_TYPE_ORIGIN)
            val file = File(originPath)
            if (file.exists()) {
                localImgPath = originPath
            }
        }
        return localImgPath
    }

/**图片显示或视频消息, 需要展示的图片本地地址*/
val MessageInfoBean.imagePath: String?
    get() = originImagePath ?: dataPath

/**图片列表*/
val MessageInfoBean.imageList: List<V2TIMImageElem.V2TIMImage>
    get() {
        val images = mutableListOf<V2TIMImageElem.V2TIMImage>()
        val message: V2TIMMessage? = message
        if (message != null) {
            if (message.elemType == V2TIMMessage.V2TIM_ELEM_TYPE_IMAGE) {
                val imageElem = message.imageElem
                val imageList = imageElem.imageList
                if (!imageList.isNullOrEmpty()) {
                    images.addAll(imageList)
                }
            }
        }
        return images
    }

/**视频元素*/
val MessageInfoBean.videoElem: V2TIMVideoElem?
    get() {
        val message: V2TIMMessage? = message
        var videoElem: V2TIMVideoElem? = null
        if (message != null) {
            if (message.elemType == V2TIMMessage.V2TIM_ELEM_TYPE_VIDEO) {
                videoElem = message.videoElem
            }
        }
        return videoElem
    }

/**音频元素*/
val MessageInfoBean.soundElem: V2TIMSoundElem?
    get() {
        val message: V2TIMMessage? = message
        var soundElem: V2TIMSoundElem? = null
        if (message != null) {
            if (message.elemType == V2TIMMessage.V2TIM_ELEM_TYPE_SOUND) {
                soundElem = message.soundElem
            }
        }
        return soundElem
    }

/**文件元素*/
val MessageInfoBean.fileElem: V2TIMFileElem?
    get() {
        val message: V2TIMMessage? = message
        var elem: V2TIMFileElem? = null
        if (message != null) {
            if (message.elemType == V2TIMMessage.V2TIM_ELEM_TYPE_FILE) {
                elem = message.fileElem
            }
        }
        return elem
    }