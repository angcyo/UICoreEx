package com.angcyo.tim.helper.convert

import com.angcyo.tim.bean.MessageInfoBean
import com.angcyo.tim.dslitem.BaseChatMsgItem
import com.tencent.imsdk.v2.V2TIMMessage

/**
 * 自定义消息的转换
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/18
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class CustomConvert : BaseConvert() {

    override fun handleMessage(message: V2TIMMessage): Boolean {
        if (message.elemType == V2TIMMessage.V2TIM_ELEM_TYPE_CUSTOM) {
            //自定义消息, 但是没有内容
            val data = message.customElem?.data
            if (data == null || data.isEmpty()) {
                return false
            }
            return true
        }
        return false
    }

    override fun handleBean(bean: MessageInfoBean): Boolean {
        return bean.message?.elemType == V2TIMMessage.V2TIM_ELEM_TYPE_CUSTOM
    }

    override fun convertToBean(message: V2TIMMessage): MessageInfoBean {
        return baseMessageInfoBean(message).apply {
            content = "[自定义消息]"
        }
    }

    override fun convertToItem(bean: MessageInfoBean): BaseChatMsgItem? {
        return super.convertToItem(bean)
    }
}