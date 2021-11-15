package com.angcyo.tim.bean

import com.tencent.imsdk.v2.V2TIMConversation

/**
 * 离线推送消息体
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/15
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class OfflineMessageBean {

    companion object {
        /**聊天*/
        val REDIRECT_ACTION_CHAT = 1

        /**打电话*/
        val REDIRECT_ACTION_CALL = 2
    }

    var version = 1
    var chatType = V2TIMConversation.V2TIM_C2C //聊天类型
    var action = REDIRECT_ACTION_CHAT //动作
    var sender: String? = null //发送者, 群聊则是groupId
    var title: String? = null //聊天标题
    var faceUrl: String? = null //头像
    var content: String? = null //内容

    // 发送时间戳，单位毫秒
    var sendTime: Long = 0 //发送事件
}