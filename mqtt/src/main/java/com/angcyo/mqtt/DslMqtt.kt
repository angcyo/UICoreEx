package com.angcyo.mqtt

import android.content.Context
import android.text.TextUtils
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.util.*
import kotlin.collections.ArrayList


/**
 * https://github.com/angcyo/DslMqtt
 *
 * https://github.com/eclipse/paho.mqtt.android
 *
 * https://www.eclipse.org/paho/clients/android
 *
 * - 需要库: localbroadcastmanager
 * ```
 * implementation "androidx.localbroadcastmanager:localbroadcastmanager:1.0.0"
 * ```
 * - 需要声明组件:MqttService
 * ```
 * <service android:name="org.eclipse.paho.android.service.MqttService"/>
 * ```
 * - 需要权限:
 * ```
 * android.permission.WAKE_LOCK
 * android.permission.ACCESS_NETWORK_STATE
 * ```
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/12/09
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
open class DslMqtt {
    //<editor-fold desc="属性配置">

    /**客户端的id*/
    var clientId: String = UUID.randomUUID().toString()

    /**日志输出*/
    var mqttLog: MqttLog = MqttLog()

    /**消息连接的配置参数*/
    val connectOptions = MqttConnectOptions().apply {
        //自动重连
        isAutomaticReconnect = true
        isCleanSession = false
        //心跳间隔
        keepAliveInterval = 60
        //超时时长
        connectionTimeout = 10
    }

    //客户端
    var _mqttClient: MqttAndroidClient? = null

    //连接的服务器地址
    var _serverURI: String? = null

    //保存已经订阅的主题(重连后恢复)
    val _topicList = mutableListOf<Pair<String, Int>>()

    //保存需要发送的消息(未连接时, 需要发送的消息)
    val _publishList = mutableListOf<Pair<String, MqttMessage>>()

    //</editor-fold desc="属性配置">

    //<editor-fold desc="可操作方法">

    var _mqttToken: IMqttToken? = null
    /**
     * 连接mqtt, url不能用`/`结尾
     * @param serverURI 服务器地址 "tcp://127.0.0.1:1883" "ssl://127.0.0.1:1883"
     * */
    open fun connect(context: Context, serverURI: String) {
        if (TextUtils.equals(_serverURI, serverURI)) {
            return
        }

        try {
            _mqttClient?.disconnect()
        } catch (e: Exception) {
            //no op
        }

        _serverURI = serverURI

        MqttAndroidClient(context.applicationContext, _serverURI, clientId).apply {
            _mqttClient = this
            setCallback(_mqttCallbackExtended)
            mqttLog.i("开始连接:$_serverURI")
            _mqttToken= connect(connectOptions, null, _mqttActionListener)
        }
    }

    /**断开mqtt*/
    open fun disconnect() {
        try {
            _mqttClient?.disconnect()
        } catch (e: Exception) {
            //no op
        }
        _serverURI = null
    }

    /**释放Service*/
    fun release() {
        disconnect()
        _mqttClient?.close()
        _mqttClient = null
    }

    /**返回是否已连接*/
    fun isConnected(): Boolean {
        return _mqttClient?.isConnected ?: false
    }

    /**订阅主题*/
    fun subscribe(topic: String, qos: Int = 2): IMqttToken? {
        /* qos
       具体有三种消息发布的服务质量：
       至多一次，消息发布完全依赖底层 TCP/IP 网络。会发生消息丢失或重复。这一级别可用于如下情况，环境传感器数据，丢失一次读记录无所谓，因为不久后还会有第二次发送。
       至少一次，确保消息到达，但消息重复可能会发生。
       只有一次，确保消息到达一次。这一级别可用于如下情况，在计费系统中，消息重复或丢失会导致不正确的结果。
       */

        /*
        qos为0：“至多一次”，消息发布完全依赖底层 TCP/IP 网络。会发生消息丢失或重复。这一级别可用于如下情况，环境传感器数据，丢失一次读记录无所谓，因为不久后还会有第二次发送。
        qos为1：“至少一次”，确保消息到达，但消息重复可能会发生。这一级别可用于如下情况，你需要获得每一条消息，并且消息重复发送对你的使用场景无影响。
        qos为2：“只有一次”，确保消息到达一次。这一级别可用于如下情况，在计费系统中，消息重复或丢失会导致不正确的结果。
        */

        _topicList.add(topic to qos)

        return try {
            mqttLog.i("订阅${_mqttClient.hash()}:$topic:$qos")
            _mqttClient?.subscribe(topic, qos)
        } catch (e: Exception) {
            null
        }
    }

    /**批量订阅*/
    fun subscribe(topic: Array<String>, qos: IntArray): IMqttToken? {
        if (topic.isEmpty()) {
            return null
        }

        topic.forEachIndexed { index, s ->
            _topicList.add(s to (qos.getOrNull(index) ?: 2))
        }

        return try {
            mqttLog.i("订阅${_mqttClient.hash()}:$topic:$qos")
            _mqttClient?.subscribe(topic, qos)
        } catch (e: Exception) {
            null
        }
    }

    fun unsubscribe(topic: String): IMqttToken? {
        _topicList.removeAll { TextUtils.equals(it.first, topic) }
        return try {
            mqttLog.i("取消订阅${_mqttClient.hash()}:$topic")
            _mqttClient?.unsubscribe(topic)
        } catch (e: Exception) {
            null
        }
    }

    fun unsubscribe(topic: Array<String>): IMqttToken? {
        if (topic.isEmpty()) {
            return null
        }
        return try {
            mqttLog.i("取消订阅${_mqttClient.hash()}:$topic")
            _mqttClient?.unsubscribe(topic)
        } catch (e: Exception) {
            null
        }
    }

    /**向主题发送消息*/
    fun publish(
        topic: String, message: String?,
        qos: Int = 2,
        retained: Boolean = false
    ): IMqttDeliveryToken? {
        if (message.isNullOrEmpty()) {
            return null
        }
        return publish(topic, message.toByteArray(), qos, retained)
    }

    fun publish(
        topic: String, payload: ByteArray,
        qos: Int = 2,
        retained: Boolean = false
    ): IMqttDeliveryToken? {

        val message = MqttMessage(payload)
        message.qos = qos
        message.isRetained = retained

        return publish(topic, message)
    }

    fun publish(topic: String, message: MqttMessage): IMqttDeliveryToken? {
        if (isConnected()) {

            mqttLog.i("发送消息${_mqttClient.hash()}:$topic:$message")

            return _mqttClient?.publish(topic, message)
        }

        _publishList.add(topic to message)

        return null
    }

    //</editor-fold desc="可操作方法">

    //<editor-fold desc="回调配置">

    /**连接成功回调*/
    var onConnectSuccess: (MqttAndroidClient, IMqttToken) -> Unit =
        { mqttAndroidClient, iMqttToken ->

        }

    /**连接完成回调*/
    var onConnectComplete: (client: MqttAndroidClient, reconnect: Boolean, serverURI: String) -> Unit =
        { client, _, _ ->
            _setBufferOpts(client)
        }

    /**连接失败回调*/
    var onConnectFailure: (IMqttToken, Throwable) -> Unit = { _, _ ->

    }

    /**收到消息回调*/
    var onMessageArrived: (topic: String, message: MqttMessage) -> Unit = { _, _ -> }

    //</editor-fold desc="回调配置">

    //<editor-fold desc="内置回调">

    fun _setBufferOpts(client: MqttAndroidClient) {
        val disconnectedBufferOptions = DisconnectedBufferOptions()
        disconnectedBufferOptions.isBufferEnabled = true
        disconnectedBufferOptions.bufferSize = 4096
        disconnectedBufferOptions.isPersistBuffer = true
        disconnectedBufferOptions.isDeleteOldestMessages = true
        client.setBufferOpts(disconnectedBufferOptions)
    }

    //取消所有订阅
    fun _unsubscribe() {

        if (_topicList.isNotEmpty()) {
            _mqttClient?.unsubscribe(_topicList.map { it.first }.toTypedArray())
        }

        _topicList.clear()
    }

    //订阅所有
    fun _subscribe() {
        if (_topicList.isNotEmpty()) {

            val topic = _topicList.map { it.first }.toTypedArray()
            val qos = _topicList.map { it.second }.toIntArray()

            mqttLog.i("恢复订阅${_mqttClient.hash()}:${topic}:${qos}")

            _mqttClient?.subscribe(topic, qos)
        }
    }

    //发送所有离线缓存的消息
    fun _publish() {
        if (_publishList.isNotEmpty()) {

            val list = ArrayList(_publishList)

            list.forEach {
                mqttLog.i("恢复发送消息${_mqttClient.hash()}:${it.first}:${it.second}")

                _mqttClient?.publish(it.first, it.second)?.actionCallback =
                    object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken) {
                            _publishList.remove(it)
                        }

                        override fun onFailure(
                            asyncActionToken: IMqttToken,
                            exception: Throwable
                        ) {
                            //no op
                        }
                    }
            }
        }
    }

    fun Any?.hash(): String {
        return this?.hashCode()?.toString(16) ?: "0x0"
    }

    var _mqttCallbackExtended: MqttCallback = object : MqttCallbackExtended {

        /**
         * 连接成功回调
         *
         * @param reconnect 是否是重连
         * */
        override fun connectComplete(reconnect: Boolean, serverURI: String) {
            mqttLog.i("连接完成${_mqttClient.hash()}:$serverURI 重连:$reconnect")
            _subscribe()
            _publish()
            _mqttClient?.let {
                onConnectComplete(it, reconnect, serverURI)
            }
        }

        /**收到消息回调*/
        override fun messageArrived(topic: String, message: MqttMessage) {
            mqttLog.i("收到消息${_mqttClient.hash()}:$topic:${message.id}:$message")
            onMessageArrived(topic, message)
        }

        /**连接断开*/
        override fun connectionLost(cause: Throwable?) {
            mqttLog.i("连接断开:$cause")
        }

        /**消息发送完成*/
        override fun deliveryComplete(token: IMqttDeliveryToken) {
            mqttLog.i("消息已发送${token.client.hash()}:${token.topics}:${token.messageId}:${token.message}")
        }
    }

    var _mqttActionListener: IMqttActionListener = object : IMqttActionListener {
        override fun onSuccess(asyncActionToken: IMqttToken) {
            mqttLog.i("连接成功:${asyncActionToken.messageId}:${asyncActionToken.client.hash()}")
            _mqttClient?.let {
                onConnectSuccess(it, asyncActionToken)
            }
        }

        override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
            mqttLog.i("连接失败:${asyncActionToken.messageId}:$exception")
            onConnectFailure(asyncActionToken, exception)
        }
    }

    //<editor-fold desc="内置回调">
}