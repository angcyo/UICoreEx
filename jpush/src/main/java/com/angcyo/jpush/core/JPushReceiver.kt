package com.angcyo.jpush.core

import android.content.Context
import android.content.Intent
import cn.jpush.android.api.*
import cn.jpush.android.service.JPushMessageReceiver
import com.angcyo.core.vmCore
import com.angcyo.jpush.JPushModel
import com.angcyo.library.L

/**
 * 极光推送统一接收入口
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/06/23
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class JPushReceiver : JPushMessageReceiver() {

    /**收到了自定义消息*/
    override fun onMessage(context: Context?, customMessage: CustomMessage?) {
        super.onMessage(context, customMessage)
        L.i("[onMessage] $customMessage")
        vmCore<JPushModel>().customMessageData.postValue(customMessage)
    }

    /**收到了极光后台推送过来的消息, 会自动显示通知栏*/
    override fun onNotifyMessageArrived(context: Context?, message: NotificationMessage) {
        super.onNotifyMessageArrived(context, message)
        L.i("[onNotifyMessageArrived] $message")
        vmCore<JPushModel>().notificationMessageData.postValue(message)
    }

    /**滑动取消了通知*/
    override fun onNotifyMessageDismiss(context: Context?, message: NotificationMessage) {
        super.onNotifyMessageDismiss(context, message)
        L.i("[onNotifyMessageDismiss] $message")
    }

    /**点击通知栏*/
    override fun onNotifyMessageOpened(context: Context?, message: NotificationMessage?) {
        super.onNotifyMessageOpened(context, message)
        L.i("[onNotifyMessageOpened] $message")
    }

    /**设备注册成功*/
    override fun onRegister(context: Context?, registrationId: String) {
        super.onRegister(context, registrationId)
        L.i("[onRegister] $registrationId")
        vmCore<JPushModel>().registrationIdData.postValue(registrationId)
    }

    /**是否连接到极光后台*/
    override fun onConnected(context: Context?, isConnected: Boolean) {
        super.onConnected(context, isConnected)
        L.i("[onConnected] $isConnected")
        vmCore<JPushModel>().connectedData.postValue(isConnected)
    }

    override fun onMultiActionClicked(context: Context, intent: Intent) {
        super.onMultiActionClicked(context, intent)

        L.i("[onMultiActionClicked] 用户点击了通知栏按钮")

        val nActionExtra = intent.extras!!.getString(JPushInterface.EXTRA_NOTIFICATION_ACTION_EXTRA)

        //开发者根据不同 Action 携带的 extra 字段来分配不同的动作。
        if (nActionExtra == null) {
            L.d("ACTION_NOTIFICATION_CLICK_ACTION nActionExtra is null")
            return
        }
        if (nActionExtra == "my_extra1") {
            L.i("[onMultiActionClicked] 用户点击通知栏按钮一")
        } else if (nActionExtra == "my_extra2") {
            L.i("[onMultiActionClicked] 用户点击通知栏按钮二")
        } else if (nActionExtra == "my_extra3") {
            L.i("[onMultiActionClicked] 用户点击通知栏按钮三")
        } else {
            L.i("[onMultiActionClicked] 用户点击通知栏按钮未定义")
        }
    }

    override fun onCommandResult(context: Context?, cmdMessage: CmdMessage) {
        super.onCommandResult(context, cmdMessage)
        L.i("[onCommandResult] $cmdMessage")
    }

    /*http://docs.jiguang.cn/jpush/client/Android/android_api/#_77*/

    /**标签操作返回回调*/
    override fun onTagOperatorResult(context: Context?, jPushMessage: JPushMessage?) {
        super.onTagOperatorResult(context, jPushMessage)
        L.i("[onTagOperatorResult] $jPushMessage")

        vmCore<JPushModel>().tagMessageData.postValue(jPushMessage)
    }

    override fun onCheckTagOperatorResult(context: Context?, jPushMessage: JPushMessage?) {
        super.onCheckTagOperatorResult(context, jPushMessage)
        L.i("[onCheckTagOperatorResult] $jPushMessage")
    }

    /**别名操作返回回调*/
    override fun onAliasOperatorResult(context: Context?, jPushMessage: JPushMessage?) {
        super.onAliasOperatorResult(context, jPushMessage)
        L.i("[onAliasOperatorResult] $jPushMessage")

        vmCore<JPushModel>().aliasMessageData.postValue(jPushMessage)
    }

    /**手机号码设置操作返回回调*/
    override fun onMobileNumberOperatorResult(context: Context?, jPushMessage: JPushMessage?) {
        super.onMobileNumberOperatorResult(context, jPushMessage)
        L.i("[onMobileNumberOperatorResult] $jPushMessage")

        vmCore<JPushModel>().mobileMessageData.postValue(jPushMessage)
    }

    override fun onNotificationSettingsCheck(context: Context?, isOn: Boolean, source: Int) {
        super.onNotificationSettingsCheck(context, isOn, source)
        L.i("[onNotificationSettingsCheck] isOn:$isOn,source:$source")
    }
}

/**消息是否成功
 * 错误码:https://docs.jiguang.cn/jpush/client/Android/android_api/#_153
 * */
fun JPushMessage.isSucceed() = errorCode == 0