package com.angcyo.tim.helper.convert

import com.angcyo.tim.bean.MessageInfoBean
import com.angcyo.tim.dslitem.BaseChatMsgItem
import com.tencent.imsdk.v2.V2TIMMessage

/**
 * 转换器
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/18
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
abstract class BaseConvert {

    companion object {
        fun baseMessageInfoBean(message: V2TIMMessage): MessageInfoBean {
            val bean = MessageInfoBean()
            message.apply {
                bean.message = this
                bean.msgType = elemType //this
                bean.timestamp = timestamp * 1000
                bean.fromUser = sender
                bean.messageId = msgID
            }
            return bean
        }
    }

    /**是否需要处理[message], 决定[convertToBean]是否调用
     * https://im.sdk.qcloud.com/doc/zh-cn/classcom_1_1tencent_1_1imsdk_1_1v2_1_1V2TIMMessage.html#a00455865d1a14191b8c612252bf20a1c
     * */
    open fun handleMessage(message: V2TIMMessage): Boolean {
        return false
    }

    /**
     * [V2TIMMessage]->[MessageInfoBean]
     * 将SDK消息转换成本地传输的数据结构
     * */
    open fun convertToBean(message: V2TIMMessage): MessageInfoBean? {
        return null
    }

    /**是否需要处理[bean], 决定[convertToItem]是否调用*/
    open fun handleBean(bean: MessageInfoBean): Boolean {
        return false
    }

    /**
     * [MessageInfoBean]->[BaseChatMsgItem]
     * 聊天消息转换成界面元素
     * */
    open fun convertToItem(bean: MessageInfoBean): BaseChatMsgItem? {
        return null
    }

}