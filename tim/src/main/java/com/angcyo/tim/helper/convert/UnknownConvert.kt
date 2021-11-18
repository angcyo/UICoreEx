package com.angcyo.tim.helper.convert

import com.angcyo.tim.bean.MessageInfoBean
import com.angcyo.tim.dslitem.BaseChatMsgItem
import com.angcyo.tim.dslitem.MsgUnknownItem
import com.tencent.imsdk.v2.V2TIMMessage

/**
 * 未知消息的转换, 需要放在列表的最后面
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/18
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class UnknownConvert : BaseConvert() {

    override fun handleMessage(message: V2TIMMessage): Boolean {
        return true
    }

    override fun handleBean(bean: MessageInfoBean): Boolean {
        return true
    }

    override fun convertToBean(message: V2TIMMessage): MessageInfoBean {
        return baseMessageInfoBean(message).apply {
            content = "[不支持的消息类型]"
        }
    }

    override fun convertToItem(bean: MessageInfoBean): BaseChatMsgItem? {
        return MsgUnknownItem()
    }
}