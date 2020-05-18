package com.angcyo.amqp

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Envelope

/**
 * 回调处理
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/05/15
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class RabbitAction {
    /**错误处理
     * @AnyThread*/
    var errorAction: (reason: String, error: Exception) -> Unit = { _, _ -> }

    /**收到消息的回调
     * @AnyThread*/
    var messageAction: (
        msg: String, consumerTag: String,
        envelope: Envelope,
        properties: AMQP.BasicProperties
    ) -> Unit = { _, _, _, _ -> }
}