package com.angcyo.jpush

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cn.jpush.android.api.CustomMessage
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
}