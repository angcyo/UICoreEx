package com.angcyo.tim.helper.convert

import com.angcyo.http.base.fromJson
import com.angcyo.tim.bean.MessageInfoBean
import com.angcyo.tim.dslitem.BaseChatMsgItem
import com.angcyo.tim.dslitem.MsgTextItem
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
            return true
        }
        return false
    }

    override fun handleBean(bean: MessageInfoBean): Boolean {
        return bean.msgType == V2TIMMessage.V2TIM_ELEM_TYPE_CUSTOM
    }

    override fun convertToBean(message: V2TIMMessage): MessageInfoBean {
        return baseMessageInfoBean(message).apply {
            //{"businessId":2,"description":"您有新的合同待签署","businessType":"CONTRACT","contentType":"TEXT"}
            val data = message.customElem?.data

            //自定义消息, 但是没有内容
            if (data == null || data.isEmpty()) {
                content = "[自定义消息]"
            } else {
                val json = String(data)
                val map = json.fromJson<Map<String, Any>>()
                content = map?.get("textContent")?.toString() ?: map?.get("description")?.toString()
                    ?: json
            }
        }
    }

    override fun convertToItem(bean: MessageInfoBean): BaseChatMsgItem? {
        //自定义的消息, 暂且使用文本item显示
        return MsgTextItem()
    }
}