package com.angcyo.tim.helper.convert

import com.angcyo.library.ex.bitmapSize
import com.angcyo.library.ex.isFileExist
import com.angcyo.tim.bean.MessageInfoBean
import com.angcyo.tim.bean.MessageInfoBean.Companion.MSG_STATUS_DOWNLOADED
import com.angcyo.tim.bean.isSelf
import com.angcyo.tim.dslitem.BaseChatMsgItem
import com.angcyo.tim.dslitem.MsgImageItem
import com.angcyo.tim.helper.ChatDownloadHelper
import com.angcyo.tim.util.TimConfig
import com.tencent.imsdk.v2.V2TIMMessage
import com.tencent.imsdk.v2.V2TIMVideoElem

/**
 * 视频消息的转换
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/18
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class VideoConvert : BaseConvert() {

    override fun handleMessage(message: V2TIMMessage): Boolean {
        return message.elemType == V2TIMMessage.V2TIM_ELEM_TYPE_VIDEO
    }

    override fun handleBean(bean: MessageInfoBean): Boolean {
        return bean.msgType == V2TIMMessage.V2TIM_ELEM_TYPE_VIDEO
    }

    override fun convertToBean(message: V2TIMMessage): MessageInfoBean {
        return baseMessageInfoBean(message).apply {
            content = "[视频]"

            val videoEle: V2TIMVideoElem = message.videoElem
            if (isSelf && videoEle.snapshotPath.isFileExist()) {
                //截图文件路径（只有发送方才能获取到）
                val size: IntArray = videoEle.snapshotPath.bitmapSize()
                imageWidth = size[0]
                imageHeight = size[1]
                dataPath = videoEle.snapshotPath
                dataUri = videoEle.videoPath
                if (videoEle.videoPath.isFileExist()) {
                    //视频文件路径（只有发送方才能获取到）
                    downloadStatus = MSG_STATUS_DOWNLOADED
                }
            } else {
                //下载视频缩略图
                ChatDownloadHelper.downloadSnapshot(videoEle, this, null, null)
                val snapPath: String = TimConfig.getImageDownloadDir(videoEle.snapshotUUID)
                val videoPath: String = TimConfig.getVideoDownloadDir(videoEle.videoUUID)

                dataPath = snapPath
                dataUri = videoPath
                imageWidth = videoEle.snapshotWidth
                imageHeight = videoEle.snapshotHeight
            }
        }
    }

    override fun convertToItem(bean: MessageInfoBean): BaseChatMsgItem {
        return MsgImageItem()
    }
}