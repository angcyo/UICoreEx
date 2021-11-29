package com.angcyo.tim.helper.convert

import com.angcyo.tim.bean.MessageInfoBean
import com.angcyo.tim.dslitem.BaseChatMsgItem
import com.angcyo.tim.dslitem.MsgLocationItem
import com.tencent.imsdk.v2.V2TIMMessage

/**
 * 位置消息的转换
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/18
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class LocationConvert : BaseConvert() {

    override fun handleMessage(message: V2TIMMessage): Boolean {
        return message.elemType == V2TIMMessage.V2TIM_ELEM_TYPE_LOCATION
    }

    override fun handleBean(bean: MessageInfoBean): Boolean {
        return bean.msgType == V2TIMMessage.V2TIM_ELEM_TYPE_LOCATION
    }

    override fun convertToBean(message: V2TIMMessage): MessageInfoBean {
        return baseMessageInfoBean(message).apply {
            content = "[位置]"
        }
    }

    override fun convertToItem(bean: MessageInfoBean): BaseChatMsgItem {
        return MsgLocationItem()
    }
}