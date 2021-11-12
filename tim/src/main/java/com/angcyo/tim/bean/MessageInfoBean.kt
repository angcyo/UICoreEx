package com.angcyo.tim.bean

import android.net.Uri
import android.text.TextUtils
import com.angcyo.library.L
import com.angcyo.library.ex.bitmapSize
import com.angcyo.library.ex.toUri
import com.angcyo.tim.bean.MessageInfoBean.Companion.MSG_STATUS_DOWNLOADED
import com.angcyo.tim.bean.MessageInfoBean.Companion.MSG_STATUS_REVOKE
import com.angcyo.tim.bean.MessageInfoBean.Companion.MSG_STATUS_SENDING
import com.angcyo.tim.bean.MessageInfoBean.Companion.MSG_STATUS_SEND_FAIL
import com.angcyo.tim.bean.MessageInfoBean.Companion.MSG_STATUS_SEND_SUCCESS
import com.angcyo.tim.bean.MessageInfoBean.Companion.MSG_STATUS_UN_DOWNLOAD
import com.angcyo.tim.util.TimConfig
import com.tencent.imsdk.v2.*
import com.tencent.imsdk.v2.V2TIMElem.V2ProgressInfo
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

    /**TIM SDK 原始的消息数据*/
    var message: V2TIMMessage? = null

    /**消息需要显示的文本内容*/
    var content: String? = null

    /**数据路径, 比如:语音文件路径等*/
    var dataPath: String? = null

    /**数据的uri, 如视频*/
    var dataUri: Uri? = null

    /**图片的宽度, 视频的缩略图*/
    var imageWidth: Int = -1

    /**图片的高度*/
    var imageHeight: Int = -1

    /**消息的发送状态*/
    var status: Int = MSG_STATUS_NORMAL

    /**消息的下载状态*/
    var downloadStatus: Int = MSG_STATUS_NORMAL
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

/**获取消息发送者 userID*/
val MessageInfoBean.sender: String
    get() {
        val sender = message?.sender
        return if (sender.isNullOrEmpty()) {
            V2TIMManager.getInstance().loginUser
        } else {
            sender
        }
    }

fun V2TIMMessage.toMessageInfoBean(): MessageInfoBean? {
    if (status == V2TIMMessage.V2TIM_MSG_STATUS_HAS_DELETED /*消息被删除*/ ||
        elemType == V2TIMMessage.V2TIM_ELEM_TYPE_NONE /*无元素的消息*/) {
        return null
    }

    if (elemType == V2TIMMessage.V2TIM_ELEM_TYPE_CUSTOM) {
        //自定义消息, 但是没有内容
        if (customElem == null || customElem.data.isEmpty()) {
            return null
        }
    }

    val bean = MessageInfoBean()
    bean.message = this

    //https://im.sdk.qcloud.com/doc/zh-cn/classcom_1_1tencent_1_1imsdk_1_1v2_1_1V2TIMMessage.html#a00455865d1a14191b8c612252bf20a1c
    when (elemType) {
        V2TIMMessage.V2TIM_ELEM_TYPE_CUSTOM -> {
            //自定义的消息
            bean.content = "[自定义消息]"
        }
        V2TIMMessage.V2TIM_ELEM_TYPE_GROUP_TIPS -> {
            //群 tips 消息内容
        }
        V2TIMMessage.V2TIM_ELEM_TYPE_TEXT -> {
            //文本消息内容
            bean.content = textElem.text
        }
        V2TIMMessage.V2TIM_ELEM_TYPE_FACE -> {
            //表情消息内容
            if (faceElem.index < 1 || faceElem.data == null) {
                L.w("faceElem data is null or index<1")
                return null
            }
            bean.content = "[自定义表情]"
        }
        V2TIMMessage.V2TIM_ELEM_TYPE_SOUND -> {
            //语音消息内容
            val soundElemEle: V2TIMSoundElem = soundElem
            if (bean.isSelf) {
                bean.dataPath = soundElemEle.path
            } else {
                val path: String = TimConfig.getRecordDownloadDir(soundElemEle.uuid)
                val file = File(path)
                if (!file.exists()) {
                    soundElemEle.downloadSound(path, object : V2TIMDownloadCallback {
                        override fun onProgress(progressInfo: V2ProgressInfo) {
                            val currentSize = progressInfo.currentSize
                            val totalSize = progressInfo.totalSize
                            var progress = 0
                            if (totalSize > 0) {
                                progress = (100 * currentSize / totalSize).toInt()
                            }
                            if (progress > 100) {
                                progress = 100
                            }
                            L.i(
                                "ConversationMessageInfoUtil getSoundToFile",
                                "progress:$progress"
                            )
                        }

                        override fun onError(code: Int, desc: String) {
                            L.e(
                                "ConversationMessageInfoUtil getSoundToFile",
                                "$code:$desc"
                            )
                        }

                        override fun onSuccess() {
                            bean.dataPath = path
                        }
                    })
                } else {
                    bean.dataPath = path
                }
            }
            bean.content = "[语音]"
        }
        V2TIMMessage.V2TIM_ELEM_TYPE_IMAGE -> {
            //图片消息内容
            val imageEle: V2TIMImageElem = imageElem
            val localPath = imageEle.path
            if (bean.isSelf && !TextUtils.isEmpty(localPath) && File(localPath).exists()) {
                bean.dataPath = localPath
                val size: IntArray = localPath.bitmapSize()
                bean.imageWidth = size[0]
                bean.imageHeight = size[1]
            } else {
                val imageList = imageEle.imageList
                for (i in imageList.indices) {
                    val img = imageList[i]
                    if (img.type == V2TIMImageElem.V2TIM_IMAGE_TYPE_THUMB) {
                        val path: String = TimConfig.generateImagePath(
                            img.uuid,
                            V2TIMImageElem.V2TIM_IMAGE_TYPE_THUMB
                        )
                        bean.imageWidth = img.width
                        bean.imageHeight = img.height
                        val file = File(path)
                        if (file.exists()) {
                            bean.dataPath = path
                        }
                    }
                }
            }
            bean.content = "[图片]"
        }
        V2TIMMessage.V2TIM_ELEM_TYPE_VIDEO -> {
            //视频消息内容
            val videoEle: V2TIMVideoElem = videoElem
            if (bean.isSelf && !TextUtils.isEmpty(videoEle.snapshotPath)) {
                val size: IntArray = videoEle.snapshotPath.bitmapSize()
                bean.imageWidth = size[0]
                bean.imageHeight = size[1]
                bean.dataPath = videoEle.snapshotPath
                bean.dataUri = videoEle.videoPath.toUri()
            } else {
                val videoPath: String = TimConfig.getVideoDownloadDir(videoEle.videoUUID)
                val uri = Uri.parse(videoPath)
                bean.dataUri = uri
                bean.imageWidth = videoEle.snapshotWidth
                bean.imageHeight = videoEle.snapshotHeight
                val snapPath: String = TimConfig.getImageDownloadDir(videoEle.snapshotUUID)
                //判断快照是否存在,不存在自动下载
                if (File(snapPath).exists()) {
                    bean.dataPath = snapPath
                }
            }
            bean.content = "[视频]"
        }
        V2TIMMessage.V2TIM_ELEM_TYPE_FILE -> {
            //文件消息内容
            val fileElem: V2TIMFileElem = fileElem
            var filename = fileElem.uuid
            if (TextUtils.isEmpty(filename)) {
                filename = System.currentTimeMillis().toString() + fileElem.fileName
            }
            val path: String = TimConfig.getFileDownloadDir(filename)
            var finalPath: String? = path
            var file = File(path)
            if (file.exists()) {
                if (bean.isSelf) {
                    bean.status = MSG_STATUS_SEND_SUCCESS
                }
                bean.downloadStatus = MSG_STATUS_DOWNLOADED
            } else {
                if (bean.isSelf) {
                    if (TextUtils.isEmpty(fileElem.path)) {
                        bean.downloadStatus = MSG_STATUS_UN_DOWNLOAD
                    } else {
                        file = File(fileElem.path)
                        if (file.exists()) {
                            bean.status = MSG_STATUS_SEND_SUCCESS
                            bean.downloadStatus = MSG_STATUS_DOWNLOADED
                            finalPath = fileElem.path
                        } else {
                            bean.downloadStatus = MSG_STATUS_UN_DOWNLOAD
                        }
                    }
                } else {
                    bean.downloadStatus = MSG_STATUS_UN_DOWNLOAD
                }
            }
            bean.dataPath = finalPath
            bean.content = "[文件]"
        }
        V2TIMMessage.V2TIM_ELEM_TYPE_MERGER -> {
            //转发消息
            bean.content = "[聊天记录]"
        }
        V2TIMMessage.V2TIM_ELEM_TYPE_NONE -> {
            //没有元素
        }
    }

    if (status == V2TIMMessage.V2TIM_MSG_STATUS_LOCAL_REVOKED) {
        bean.status = MSG_STATUS_REVOKE
        bean.content = when {
            isSelf -> "您撤回了一条消息"
            bean.isGroup -> {
                val message: String = TimConfig.covert2HTMLString(bean.sender)
                message + "撤回了一条消息"
            }
            else -> "对方撤回了一条消息"
        }
    } else {
        if (isSelf) {
            when (status) {
                V2TIMMessage.V2TIM_MSG_STATUS_SEND_FAIL -> {
                    bean.status = MSG_STATUS_SEND_FAIL
                }
                V2TIMMessage.V2TIM_MSG_STATUS_SEND_SUCC -> {
                    bean.status = MSG_STATUS_SEND_SUCCESS
                }
                V2TIMMessage.V2TIM_MSG_STATUS_SENDING -> {
                    bean.status = MSG_STATUS_SENDING
                }
            }
        }
    }

    return bean
}