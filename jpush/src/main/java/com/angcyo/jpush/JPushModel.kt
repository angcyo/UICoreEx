package com.angcyo.jpush

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cn.jpush.android.api.CustomMessage
import cn.jpush.android.api.JPushMessage
import cn.jpush.android.api.NotificationMessage

/**
 *
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

    /**注册id*/
    val registrationIdData: MutableLiveData<String> = MutableLiveData()

    /**别名操作数据监听*/
    val aliasMessageData: MutableLiveData<JPushMessage> = MutableLiveData()

    /**标签操作数据监听*/
    val tagMessageData: MutableLiveData<JPushMessage> = MutableLiveData()

    /**手机号码操作数据监听*/
    val mobileMessageData: MutableLiveData<JPushMessage> = MutableLiveData()
}