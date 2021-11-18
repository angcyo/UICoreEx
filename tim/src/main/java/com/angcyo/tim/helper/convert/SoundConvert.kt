package com.angcyo.tim.helper.convert

import com.angcyo.library.L
import com.angcyo.library.ex.isFileExist
import com.angcyo.tim.bean.MessageInfoBean
import com.angcyo.tim.bean.isSelf
import com.angcyo.tim.dslitem.BaseChatMsgItem
import com.angcyo.tim.dslitem.MsgAudioItem
import com.angcyo.tim.util.TimConfig
import com.tencent.imsdk.v2.V2TIMDownloadCallback
import com.tencent.imsdk.v2.V2TIMElem
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
            if (isSelf) {
                dataPath = soundElemEle.path
                dataUri = soundElemEle.path
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
                            dataPath = path
                            dataUri = path
                        }
                    })
                } else {
                    dataPath = path
                    dataUri = path
                }
            }
        }
    }

    override fun convertToItem(bean: MessageInfoBean): BaseChatMsgItem {
        return MsgAudioItem()
    }
}