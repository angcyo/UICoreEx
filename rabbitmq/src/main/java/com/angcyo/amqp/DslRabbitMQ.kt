package com.angcyo.amqp

import com.angcyo.library.L
import com.rabbitmq.client.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * RabbitMQ
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/05/15
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class DslRabbitMQ {

    companion object {
        const val DEFAULT_EXCHANGE = "DslRabbitMQ"
    }

    //<editor-fold desc="成员变量">

    /**交换器, 用于发送/接收消息. 很重要的属性.*/
    var exchange: String = DEFAULT_EXCHANGE

    /**帐号密码*/
    var username: String = "admin"
    var password: String = "admin"

    /**用于创建[Connection]*/
    val connectionFactory: ConnectionFactory by lazy {
        ConnectionFactory().apply {
            username = this@DslRabbitMQ.username
            password = this@DslRabbitMQ.password
            port
            connectionTimeout
            isAutomaticRecoveryEnabled = true
        }
    }

    /**监听器列表*/
    val actionList = mutableListOf<RabbitAction>()

    //</editor-fold desc="成员变量">

    //<editor-fold desc="操作方法">

    /**[amqp://47.107.208.147:15674] or [amqps://47.107.208.147:15674]
     * 连接*/
    fun connect(uri: String) {
        if (_uri == uri) {
            L.w("已经连接:$uri")
            return
        }

        _uri?.apply {
            //已经连接过
            disconnect()
        }
        _uri = uri
        try {
            L.i("连接:$uri")
            connectionFactory.setUri(_uri)
            _executors.execute {
                try {
                    connectionFactory.newConnection()?.apply {
                        _connection = this
                        _channel = createChannel()
                    }
                    L.i("连接:$uri 完成")
                } catch (e: Exception) {
                    _uri = null
                    actionList.forEach { it.errorAction(e.message ?: "", e) }
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            _uri = null
            actionList.forEach { it.errorAction(e.message ?: "", e) }
            e.printStackTrace()
        }
    }

    /**断开连接*/
    fun disconnect() {
        try {
            _channel?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            _connection?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**监听器*/
    fun action(config: RabbitAction.() -> Unit) {
        actionList.add(RabbitAction().apply(config))
    }

    //</editor-fold desc="操作方法">

    //<editor-fold desc="内部处理">

    val _executors: Executor by lazy {
        Executors.newSingleThreadExecutor()
    }

    var _uri: String? = null

    var _channel: Channel? = null

    var _connection: Connection? = null

    /**发布消息*/
    fun _publish(routingKey: String, msg: String) {
        val channel = _channel
        if (channel == null) {
            L.w("channel is null!")
        } else {
            _executors.execute {
                try {
                    channel.basicPublish(exchange, routingKey, null, msg.toByteArray())
                } catch (e: Exception) {//AlreadyClosedException
                    actionList.forEach { it.errorAction(e.message ?: "", e) }
                    e.printStackTrace()
                }
            }
        }
    }

    /**订阅消息*/
    fun _subscribe(queueName: String, routingKey: String) {
        val channel = _channel
        if (channel == null) {
            L.w("channel is null!")
        } else {
            _executors.execute {
                //channel.basicQos()

                try {//声明交换器
                    channel.exchangeDeclare(exchange, BuiltinExchangeType.TOPIC.type, true)

                    //声明队列（持久的、非独占的、连接断开后队列会自动删除）
                    channel.queueDeclare(queueName, true, false, false, null)

                    //交换器 和 队列绑定.
                    channel.queueBind(queueName, exchange, routingKey)

                    //消费队列上的消息
                    channel.basicConsume(queueName, true, object : DefaultConsumer(channel) {
                        override fun handleCancel(consumerTag: String?) {
                            super.handleCancel(consumerTag)
                            L.v(consumerTag)
                        }

                        override fun handleCancelOk(consumerTag: String?) {
                            super.handleCancelOk(consumerTag)
                            L.v(consumerTag)
                        }

                        override fun handleConsumeOk(consumerTag: String?) {
                            super.handleConsumeOk(consumerTag)
                            L.v(consumerTag)
                        }

                        override fun handleRecoverOk(consumerTag: String?) {
                            super.handleRecoverOk(consumerTag)
                            L.v(consumerTag)
                        }

                        override fun handleShutdownSignal(
                            consumerTag: String?,
                            sig: ShutdownSignalException?
                        ) {
                            super.handleShutdownSignal(consumerTag, sig)
                            L.v(consumerTag)
                        }

                        override fun handleDelivery(
                            consumerTag: String?,
                            envelope: Envelope?,
                            properties: AMQP.BasicProperties?,
                            body: ByteArray?
                        ) {
                            body?.apply {
                                val msg = String(this)
                                L.i("收到消息:$msg $consumerTag $envelope $properties")
                                actionList.forEach {
                                    it.messageAction(
                                        msg,
                                        consumerTag!!,
                                        envelope!!,
                                        properties!!
                                    )
                                }
                            }
                        }
                    })
                } catch (e: Exception) {
                    actionList.forEach { it.errorAction(e.message ?: "", e) }
                    e.printStackTrace()
                }
            }
        }
    }

    //</editor-fold desc="内部处理">
}