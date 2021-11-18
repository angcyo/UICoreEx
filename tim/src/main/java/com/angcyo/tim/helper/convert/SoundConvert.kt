package com.angcyo.tim.helper.convert

import com.angcyo.library.ex.isFileExist
import com.angcyo.tim.bean.MessageInfoBean
import com.angcyo.tim.bean.isSelf
import com.angcyo.tim.dslitem.BaseChatMsgItem
import com.angcyo.tim.dslitem.MsgAudioItem
import com.angcyo.tim.helper.ChatDownloadHelper
import com.angcyo.tim.util.TimConfig
import com.tencent.imsdk.v2.V2TIMMessage
import com.tencent.imsdk.v2.V2TIMSoundElem

/**
 * 语音消息的转换
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/18
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class SoundConvert : BaseConvert() {

    override fun handleMessage(message: V2TIMMessage): Boolean {
        return message.elemType == V2TIMMessage.V2TIM_ELEM_TYPE_SOUND
    }

    override fun handleBean(bean: MessageInfoBean): Boolean {
        return bean.message?.elemType == V2TIMMessage.V2TIM_ELEM_TYPE_SOUND
    }

    override fun convertToBean(message: V2TIMMessage): MessageInfoBean {
        return baseMessageInfoBean(message).apply {
            content = "[语音]"

            val soundElemEle: V2TIMSoundElem = message.soundElem
            val localPath = soundElemEle.path //获取音频的本地文件路径
            if (isSelf && localPath.isFileExist()) {
                dataPath = soundElemEle.path
                dataUri = soundElemEle.path
                downloadStatus = MessageInfoBean.MSG_STATUS_DOWNLOADED
            } else {
                val path: String = TimConfig.getRecordDownloadDir(soundElemEle.uuid)
                if (path.isFileExist()) {
                    dataPath = path
                    dataUri = path
                    downloadStatus = MessageInfoBean.MSG_STATUS_DOWNLOADED
                } else {
                    //语音文件, 自动下载增加体验
                    ChatDownloadHelper.downloadSound(soundElemEle, this, null)
                }
            }
        }
    }

    override fun convertToItem(bean: MessageInfoBean): BaseChatMsgItem {
        return MsgAudioItem()
    }
}