package com.angcyo.tim.helper.convert

import com.angcyo.tim.bean.MessageInfoBean
import com.angcyo.tim.bean.MessageInfoBean.Companion.MSG_STATUS_REVOKE
import com.angcyo.tim.bean.isGroup
import com.angcyo.tim.bean.isSelf
import com.angcyo.tim.bean.sender
import com.angcyo.tim.dslitem.BaseChatMsgItem
import com.angcyo.tim.dslitem.MsgTipsItem
import com.angcyo.tim.util.TimConfig
import com.tencent.imsdk.v2.V2TIMMessage

/**
 * 提示消息消息的转换
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/18
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class TipsConvert : BaseConvert() {

    override fun handleMessage(message: V2TIMMessage): Boolean {
        return message.status == V2TIMMessage.V2TIM_MSG_STATUS_LOCAL_REVOKED
    }

    override fun handleBean(bean: MessageInfoBean): Boolean {
        return bean.msgType == MSG_STATUS_REVOKE
    }

    override fun convertToBean(message: V2TIMMessage): MessageInfoBean {
        return baseMessageInfoBean(message).apply {
            if (message.status == V2TIMMessage.V2TIM_MSG_STATUS_LOCAL_REVOKED) {
                status = MSG_STATUS_REVOKE
            }

            content = when {
                isSelf -> "您撤回了一条消息"
                isGroup -> {
                    val msg: String = TimConfig.covert2HTMLString(sender)
                    msg + "撤回了一条消息"
                }
                else -> "对方撤回了一条消息"
            }
        }
    }

    override fun convertToItem(bean: MessageInfoBean): BaseChatMsgItem {
        return MsgTipsItem()
    }
}