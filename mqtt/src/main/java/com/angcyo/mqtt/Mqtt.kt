package com.angcyo.mqtt

import android.content.Context
import android.text.TextUtils
import com.angcyo.library.L
import com.angcyo.library.utils.Device
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/06/21
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 *
 * https://www.jianshu.com/p/8ba08973a81a
 */

@Deprecated("请使用[DslMqtt]")
class Mqtt(context: Context) {

    val applicationContext: Context = context.applicationContext

    var userName: String? = null
    var password: String? = null

    var mqttConnectOptions: MqttConnectOptions
    var mqttAndroidClient: MqttAndroidClient? = null

    var clientId: String

    init {
        val options = MqttConnectOptions()
        options.isAutomaticReconnect = true
        options.isCleanSession = false
        options.keepAliveInterval = 60
        options.connectionTimeout = 10

        mqttConnectOptions = options

        clientId = Device.deviceId
    }

    /**
     * "tcp://127.0.0.1:1883"
     * "ssl://127.0.0.1:1883"
     * */
    var serverURI: String? = null

    var userContext: Any? = null

    /**连接到服务器*/
    fun connect(url: String? = serverURI) {
        if (TextUtils.isEmpty(url)) {
            throw IllegalStateException("请设置[serverURI]")
        }

        if (isConnect()) {
            if (TextUtils.equals(url, serverURI)) {
                return
            }
            disconnect()
            mqttAndroidClient = null
        }

        serverURI = url

        mqttConnectOptions.userName = userName
        if (!TextUtils.isEmpty(password)) {
            mqttConnectOptions.password = password?.toCharArray()
        }

        if (mqttAndroidClient == null) {
            initClient()
        }

        mqttAndroidClient?.connect(mqttConnectOptions, userContext,
            object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    L.i(asyncActionToken)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    L.e(exception?.message)
                }
            })
    }

    /**断开服务器*/
    fun disconnect() {
        if (!isConnect()) {
            mqttAndroidClient = null
            return
        }
        mqttAndroidClient?.disconnect(userContext,
            object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    L.i(asyncActionToken)
                    mqttAndroidClient = null
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    L.e(exception?.message)
                }
            })
    }

    /**订阅 主题*/
    fun subscribe(topic: String) {
        if (mqttAndroidClient == null) {
            throw IllegalStateException("请先调用[connect]")
        }
        if (!isConnect()) {
            return
        }
        mqttAndroidClient?.subscribe(topic, 1, userContext,
            object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    L.i(asyncActionToken)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    L.e(exception?.message)
                }
            })
    }

    /**向指定主题, 发送消息*/
    fun publish(topic: String, message: String) {
        publish(topic, message.toByteArray())
    }

    fun publish(topic: String, bytes: ByteArray) {
        if (!isConnect()) {
            return
        }

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
        mqttAndroidClient?.publish(topic, bytes, 1, false, userContext,
            object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    L.i(asyncActionToken)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    L.e(exception?.message)
                }
            })
    }

    fun unsubscribe(topic: String) {
        if (!isConnect()) {
            return
        }
        mqttAndroidClient?.unsubscribe(topic, userContext,
            object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    L.i(asyncActionToken)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    //客户端会自动处理重连
                    L.e(exception?.message)
                }
            })
    }

    /**监听, 回调 (主线程)*/
    var onConnectComplete: (reconnect: Boolean, serverURI: String) -> Unit = { _, _ -> }
    var onConnectionLost: (cause: Throwable?) -> Unit = { }
    var onMessageArrived: (topic: String, message: MqttMessage) -> Unit = { _, _ -> }
    var onDeliveryComplete: (token: IMqttDeliveryToken?) -> Unit = { }

    protected fun initClient() {
        mqttAndroidClient = MqttAndroidClient(applicationContext, serverURI, clientId).apply {
            setTraceEnabled(BuildConfig.DEBUG)

            setCallback(object : MqttCallbackExtended {

                override fun connectComplete(reconnect: Boolean, serverURI: String) {
                    val disconnectedBufferOptions = DisconnectedBufferOptions()
                    disconnectedBufferOptions.isBufferEnabled = true
                    disconnectedBufferOptions.bufferSize = 5000
                    disconnectedBufferOptions.isDeleteOldestMessages = true
                    disconnectedBufferOptions.isPersistBuffer = true
                    setBufferOpts(disconnectedBufferOptions)

                    onConnectComplete(reconnect, serverURI)
                }

                override fun connectionLost(cause: Throwable?) {
                    onConnectionLost(cause)
                }

                override fun messageArrived(topic: String, message: MqttMessage) {
                    onMessageArrived(topic, message)
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    onDeliveryComplete(token)
                }
            })
        }
    }

    /**
     * 服务是否连上, 连上之后 [mqttService] 才有值, 否则就是 NPE 了
     * */
    fun isConnect(): Boolean {
        return try {
            mqttAndroidClient?.isConnected == true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}