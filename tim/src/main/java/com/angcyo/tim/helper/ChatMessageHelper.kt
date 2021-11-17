package com.angcyo.tim.helper

import android.text.TextUtils
import com.angcyo.library.L
import com.angcyo.library.ex.bitmapSize
import com.angcyo.library.ex.isFileExist
import com.angcyo.library.ex.nowTime
import com.angcyo.tim.bean.MessageInfoBean
import com.angcyo.tim.bean.isGroup
import com.angcyo.tim.bean.isSelf
import com.angcyo.tim.bean.sender
import com.angcyo.tim.util.TimConfig
import com.tencent.imsdk.v2.*
import java.io.File

/**
 * 聊天消息转换
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/17
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
object ChatMessageHelper {
}

/**[V2TIMMessage]->[MessageInfoBean]*/
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
    bean.timestamp = timestamp * 1000
    bean.fromUser = sender
    bean.messageId = msgID

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
        V2TIMMessage.V2TIM_ELEM_TYPE_IMAGE -> {
            //图片消息内容
            val imageEle: V2TIMImageElem = imageElem
            val localPath = imageEle.path
            if (bean.isSelf && !TextUtils.isEmpty(localPath) && File(localPath).exists()) {
                //自己的消息优先使用本地图片
                bean.dataPath = localPath
                bean.dataUri = localPath
                val size: IntArray = localPath.bitmapSize()
                bean.imageWidth = size[0]
                bean.imageHeight = size[1]
            } else {
                //远程消息使用缩略图
                val imageList = imageEle.imageList
                for (i in imageList.indices) {
                    val img = imageList[i]
                    if (img.type == V2TIMImageElem.V2TIM_IMAGE_TYPE_THUMB) {
                        //缩略图
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
                    } else if (img.type == V2TIMImageElem.V2TIM_IMAGE_TYPE_ORIGIN) {
                        //原图
                        val path: String = TimConfig.generateImagePath(
                                img.uuid,
                                V2TIMImageElem.V2TIM_IMAGE_TYPE_ORIGIN
                        )
                        bean.dataUri = path
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
                bean.dataUri = videoEle.videoPath
            } else {
                val videoPath: String = TimConfig.getVideoDownloadDir(videoEle.videoUUID)
                bean.dataUri = videoPath
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
        V2TIMMessage.V2TIM_ELEM_TYPE_SOUND -> {
            //语音消息内容
            val soundElemEle: V2TIMSoundElem = soundElem
            if (bean.isSelf) {
                bean.dataPath = soundElemEle.path
                bean.dataUri = soundElemEle.path
            } else if (!soundElemEle.uuid.isNullOrEmpty()) {
                val path: String = TimConfig.getRecordDownloadDir(soundElemEle.uuid)
                if (!path.isFileExist()) {
                    soundElemEle.downloadSound(path, object : V2TIMDownloadCallback {
                        override fun onProgress(progressInfo: V2TIMElem.V2ProgressInfo) {
                            val currentSize = progressInfo.currentSize
                            val totalSize = progressInfo.totalSize
                            var progress = 0
                            if (totalSize > 0) {
                                progress = (100 * currentSize / totalSize).toInt()
                            }
                            if (progress > 100) {
                                progress = 100
                            }
                            L.i("下载音频progress:$progress")
                        }

                        override fun onError(code: Int, desc: String) {
                            L.e("下载音频失败:$code:$desc")
                        }

                        override fun onSuccess() {
                            bean.dataPath = path
                            bean.dataUri = path
                        }
                    })
                } else {
                    bean.dataPath = path
                    bean.dataUri = path
                }
            }
            bean.content = "[语音]"
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
                    bean.status = MessageInfoBean.MSG_STATUS_SEND_SUCCESS
                }
                bean.downloadStatus = MessageInfoBean.MSG_STATUS_DOWNLOADED
            } else {
                if (bean.isSelf) {
                    if (TextUtils.isEmpty(fileElem.path)) {
                        bean.downloadStatus = MessageInfoBean.MSG_STATUS_UN_DOWNLOAD
                    } else {
                        file = File(fileElem.path)
                        if (file.exists()) {
                            bean.status = MessageInfoBean.MSG_STATUS_SEND_SUCCESS
                            bean.downloadStatus = MessageInfoBean.MSG_STATUS_DOWNLOADED
                            finalPath = fileElem.path
                        } else {
                            bean.downloadStatus = MessageInfoBean.MSG_STATUS_UN_DOWNLOAD
                        }
                    }
                } else {
                    bean.downloadStatus = MessageInfoBean.MSG_STATUS_UN_DOWNLOAD
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
        bean.status = MessageInfoBean.MSG_STATUS_REVOKE
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
                    bean.status = MessageInfoBean.MSG_STATUS_SEND_FAIL
                }
                V2TIMMessage.V2TIM_MSG_STATUS_SEND_SUCC -> {
                    bean.status = MessageInfoBean.MSG_STATUS_SEND_SUCCESS
                }
                V2TIMMessage.V2TIM_MSG_STATUS_SENDING -> {
                    bean.status = MessageInfoBean.MSG_STATUS_SENDING
                }
            }
        } else {
            bean.status = MessageInfoBean.MSG_STATUS_SEND_SUCCESS
        }
    }

    return bean
}

/**简单消息的包装体*/
fun V2TIMMessage.toMyselfMessageInfoBean(content: String?): MessageInfoBean {
    val bean = MessageInfoBean()

    bean.message = this
    bean.timestamp = nowTime()
    bean.fromUser = V2TIMManager.getInstance().loginUser

    bean.content = content
    return bean
}

/**图片消息的包装体*/
fun V2TIMMessage.toMyselfImageMessageInfoBean(
        imagePath: String,
        content: String? = "[图片]",
): MessageInfoBean {
    val bean = toMyselfMessageInfoBean(content)
    bean.dataUri = imagePath
    bean.dataPath = imagePath

    val size = imagePath.bitmapSize()
    bean.imageWidth = size[0]
    bean.imageHeight = size[1]

    return bean
}

/**视频消息的包装体*/
fun V2TIMMessage.toMyselfVideoMessageInfoBean(
        videoFilePath: String,
        snapshotPath: String,
        content: String? = "[视频]",
): MessageInfoBean {
    val bean = toMyselfImageMessageInfoBean(snapshotPath, content)
    bean.dataUri = videoFilePath
    bean.dataPath = videoFilePath
    return bean
}

/**视频消息的包装体*/
fun V2TIMMessage.toMyselfSoundMessageInfoBean(soundPath: String, content: String? = "[语音]"): MessageInfoBean {
    val bean = toMyselfMessageInfoBean(content)
    bean.dataUri = soundPath
    bean.dataPath = soundPath
    return bean
}