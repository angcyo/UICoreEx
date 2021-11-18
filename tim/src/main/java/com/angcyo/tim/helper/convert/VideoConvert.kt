package com.angcyo.tim.helper.convert

import android.text.TextUtils
import com.angcyo.library.ex.bitmapSize
import com.angcyo.tim.bean.MessageInfoBean
import com.angcyo.tim.bean.isSelf
import com.angcyo.tim.dslitem.BaseChatMsgItem
import com.angcyo.tim.dslitem.MsgImageItem
import com.angcyo.tim.util.TimConfig
import com.tencent.imsdk.v2.V2TIMMessage
import com.tencent.imsdk.v2.V2TIMVideoElem
import java.io.File

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
        return bean.message?.elemType == V2TIMMessage.V2TIM_ELEM_TYPE_VIDEO
    }

    override fun convertToBean(message: V2TIMMessage): MessageInfoBean {
        return baseMessageInfoBean(message).apply {
            content = "[视频]"

            val videoEle: V2TIMVideoElem = message.videoElem
            if (isSelf && !TextUtils.isEmpty(videoEle.snapshotPath)) {
                val size: IntArray = videoEle.snapshotPath.bitmapSize()
                imageWidth = size[0]
                imageHeight = size[1]
                dataPath = videoEle.snapshotPath
                dataUri = videoEle.videoPath
            } else {
                val videoPath: String = TimConfig.getVideoDownloadDir(videoEle.videoUUID)
                dataUri = videoPath
                imageWidth = videoEle.snapshotWidth
                imageHeight = videoEle.snapshotHeight
                val snapPath: String = TimConfig.getImageDownloadDir(videoEle.snapshotUUID)
                //判断快照是否存在,不存在自动下载
                if (File(snapPath).exists()) {
                    dataPath = snapPath
                }
            }
        }
    }

    override fun convertToItem(bean: MessageInfoBean): BaseChatMsgItem {
        return MsgImageItem()
    }
}