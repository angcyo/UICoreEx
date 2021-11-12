package com.angcyo.tim.conversation

import com.tencent.imsdk.v2.V2TIMConversation
import com.tencent.imsdk.v2.V2TIMGroupAtInfo

/**
 * 会话助手
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/10
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
object ConversationHelper {

    /**获取@的信息*/
    fun getAtInfoType(conversation: V2TIMConversation): Int {
        var atInfoType = 0
        var atMe = false
        var atAll = false
        val atInfoList = conversation.groupAtInfoList
        if (atInfoList == null || atInfoList.isEmpty()) {
            return V2TIMGroupAtInfo.TIM_AT_UNKNOWN
        }
        for (atInfo in atInfoList) {
            if (atInfo.atType == V2TIMGroupAtInfo.TIM_AT_ME) {
                atMe = true
                continue
            }
            if (atInfo.atType == V2TIMGroupAtInfo.TIM_AT_ALL) {
                atAll = true
                continue
            }
            if (atInfo.atType == V2TIMGroupAtInfo.TIM_AT_ALL_AT_ME) {
                atMe = true
                atAll = true
                continue
            }
        }
        atInfoType = if (atAll && atMe) {
            V2TIMGroupAtInfo.TIM_AT_ALL_AT_ME
        } else if (atAll) {
            V2TIMGroupAtInfo.TIM_AT_ALL
        } else if (atMe) {
            V2TIMGroupAtInfo.TIM_AT_ME
        } else {
            V2TIMGroupAtInfo.TIM_AT_UNKNOWN
        }
        return atInfoType
    }

}