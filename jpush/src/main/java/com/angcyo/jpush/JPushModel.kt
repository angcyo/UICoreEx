package com.angcyo.jpush

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cn.jpush.android.api.CustomMessage
import cn.jpush.android.api.JPushMessage
import cn.jpush.android.api.NotificationMessage

/**
 * 所有消息, 如果仅需要使用一次, 那么在监听到数据后, 需要手动清理. 否则回收缓存.
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/06/23
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class JPushModel : ViewModel() {

    /**自定义消息监听*/
    val customMessageData: MutableLiveData<CustomMessage> = MutableLiveData()

    /**通知消息监听*/
    val notificationMessageData: MutableLiveData<NotificationMessage> = MutableLiveData()

    /**设备注册id成功*/
    val registrationIdData: MutableLiveData<String> = MutableLiveData()

    /**服务器连接状态*/
    val connectedData: MutableLiveData<Boolean> = MutableLiveData()

    /**别名操作数据监听*/
    val aliasMessageData: MutableLiveData<JPushMessage> = MutableLiveData()

    /**标签操作数据监听*/
    val tagMessageData: MutableLiveData<JPushMessage> = MutableLiveData()

    /**手机号码操作数据监听*/
    val mobileMessageData: MutableLiveData<JPushMessage> = MutableLiveData()
}