package com.angcyo.tim.chat

import com.angcyo.library.L
import com.angcyo.library.component._delay
import com.angcyo.tim.TimSdkException
import com.angcyo.tim.util.TimConfig
import com.tencent.imsdk.v2.V2TIMCallback
import com.tencent.imsdk.v2.V2TIMManager

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/30
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class ChatReadReportControl {

    /**消息管理*/
    val messageManager = V2TIMManager.getMessageManager()

    var _lastReadReportTime = 0L
    var _canReadReport = true

    /**
     * 收到消息上报已读加频率限制
     * @param chatId 如果是 C2C 消息， chatId 是 userId, 如果是 Group 消息 chatId 是 groupId
     * @param isGroup 是否为 Group 消息
     */
    fun limitReadReport(chatId: String?, isGroup: Boolean) {
        if (chatId.isNullOrEmpty()) {
            return
        }
        val currentTime = System.currentTimeMillis()
        val timeDifference: Long = currentTime - _lastReadReportTime
        if (timeDifference >= TimConfig.READ_REPORT_INTERVAL) {
            readReport(chatId, isGroup)
            _lastReadReportTime = currentTime
        } else {
            if (!_canReadReport) {
                return
            }
            val delay: Long = TimConfig.READ_REPORT_INTERVAL - timeDifference
            _canReadReport = false
            _delay(delay) {
                readReport(chatId, isGroup)
                _lastReadReportTime = System.currentTimeMillis()
                _canReadReport = true
            }
        }
    }

    fun readReport(
        chatId: String,
        isGroup: Boolean,
        callback: ((TimSdkException?) -> Unit)? = null
    ) {
        val listener = object : V2TIMCallback {
            override fun onError(code: Int, desc: String) {
                L.w("设置已读失败:$desc")
                callback?.invoke(TimSdkException(code, desc))
            }

            override fun onSuccess() {
                callback?.invoke(null)
            }
        }
        if (!isGroup) {
            messageManager.markC2CMessageAsRead(chatId, listener)
        } else {
            messageManager.markGroupMessageAsRead(chatId, listener)
        }
    }


}