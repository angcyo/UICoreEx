package com.angcyo.tim.helper.convert

import android.text.TextUtils
import com.angcyo.tim.bean.MessageInfoBean
import com.angcyo.tim.bean.isSelf
import com.angcyo.tim.dslitem.BaseChatMsgItem
import com.angcyo.tim.dslitem.MsgFileItem
import com.angcyo.tim.util.TimConfig
import com.tencent.imsdk.v2.V2TIMFileElem
import com.tencent.imsdk.v2.V2TIMMessage
import java.io.File

/**
 * 文件消息的转换
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/18
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class FileConvert : BaseConvert() {

    override fun handleMessage(message: V2TIMMessage): Boolean {
        return message.elemType == V2TIMMessage.V2TIM_ELEM_TYPE_FILE
    }

    override fun handleBean(bean: MessageInfoBean): Boolean {
        return bean.message?.elemType == V2TIMMessage.V2TIM_ELEM_TYPE_FILE
    }

    override fun convertToBean(message: V2TIMMessage): MessageInfoBean {
        return baseMessageInfoBean(message).apply {
            content = "[文件]"

            val fileElem: V2TIMFileElem = message.fileElem
            val filename = if (fileElem.uuid.isNullOrEmpty()) {
                "${System.currentTimeMillis()}${fileElem.fileName}"
            } else {
                fileElem.uuid
            }
            val path: String = TimConfig.getFileDownloadDir(filename)
            var finalPath: String? = path
            var file = File(path)
            if (file.exists()) {
                if (isSelf) {
                    status = MessageInfoBean.MSG_STATUS_SEND_SUCCESS
                }
                downloadStatus = MessageInfoBean.MSG_STATUS_DOWNLOADED
            } else {
                if (isSelf) {
                    if (TextUtils.isEmpty(fileElem.path)) {
                        downloadStatus = MessageInfoBean.MSG_STATUS_UN_DOWNLOAD
                    } else {
                        file = File(fileElem.path)
                        if (file.exists()) {
                            status = MessageInfoBean.MSG_STATUS_SEND_SUCCESS
                            downloadStatus = MessageInfoBean.MSG_STATUS_DOWNLOADED
                            finalPath = fileElem.path
                        } else {
                            downloadStatus = MessageInfoBean.MSG_STATUS_UN_DOWNLOAD
                        }
                    }
                } else {
                    downloadStatus = MessageInfoBean.MSG_STATUS_UN_DOWNLOAD
                }
            }
            dataPath = finalPath
            dataUri = finalPath
        }
    }

    override fun convertToItem(bean: MessageInfoBean): BaseChatMsgItem {
        return MsgFileItem()
    }
}