package com.angcyo.jpush.core

import android.content.Context
import android.content.Intent
import cn.jiguang.api.JCoreInterface
import cn.jpush.android.api.*
import cn.jpush.android.service.JPushMessageReceiver
import com.angcyo.core.component.file.DslFileHelper
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

    /**收到了自定义消息
     * ```
     * CustomMessage{messageId='58546898952892463', extra='', message='test1', contentType='', title='', senderId='ce1dbcc87fb0ae51c3bfa8e7', appId='com.wayto.plugin.rjappraise'}
     * ```
     * */
    override fun onMessage(context: Context?, customMessage: CustomMessage?) {
        super.onMessage(context, customMessage)
        L.i("[onMessage] $customMessage".apply {
            DslFileHelper.push(data = this)
        })
        vmCore<JPushModel>().customMessageData.postValue(customMessage)
    }

    /**收到了极光后台推送过来的消息, 会自动显示通知栏
     * ```
     * NotificationMessage{notificationId=516686706, msgId='20266307272951157', appkey='ce1dbcc87fb0ae51c3bfa8e7', notificationContent='test2', notificationAlertType=7, notificationTitle='test1', notificationSmallIcon='', notificationLargeIcon='', notificationExtras='{}', notificationStyle=0, notificationBuilderId=0, notificationBigText='', notificationBigPicPath='', notificationInbox='', notificationPriority=0, notificationCategory='', developerArg0='', platform=0, notificationChannelId='', displayForeground='', notificationType=0', inAppMsgType=1', inAppMsgShowType=2', inAppMsgShowPos=0', inAppMsgTitle=, inAppMsgContentBody=}
     * ```
     * */
    override fun onNotifyMessageArrived(context: Context?, message: NotificationMessage) {
        super.onNotifyMessageArrived(context, message)
        L.i("[onNotifyMessageArrived] $message".apply {
            DslFileHelper.push(data = this)
        })
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
        L.i("[onRegister] $registrationId".apply {
            DslFileHelper.push(data = this)
        })
        vmCore<JPushModel>().registrationIdData.postValue(registrationId)
    }

    /**是否连接到极光后台*/
    override fun onConnected(context: Context?, isConnected: Boolean) {
        super.onConnected(context, isConnected)
        L.i("[onConnected] $isConnected ${JCoreInterface.getRegistrationID(context)}".apply {
            DslFileHelper.push(data = this)
        })
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
        L.i("[onCommandResult] $cmdMessage".apply {
            DslFileHelper.push(data = this)
        })
    }

    /*http://docs.jiguang.cn/jpush/client/Android/android_api/#_77*/

    /**标签操作返回回调*/
    override fun onTagOperatorResult(context: Context?, jPushMessage: JPushMessage?) {
        super.onTagOperatorResult(context, jPushMessage)
        L.i("[onTagOperatorResult] ${jPushMessage.isSucceed()} $jPushMessage".apply {
            DslFileHelper.push(data = this)
        })

        vmCore<JPushModel>().tagMessageData.postValue(jPushMessage)
    }

    override fun onCheckTagOperatorResult(context: Context?, jPushMessage: JPushMessage?) {
        super.onCheckTagOperatorResult(context, jPushMessage)
        L.i("[onCheckTagOperatorResult] ${jPushMessage.isSucceed()} $jPushMessage".apply {
            DslFileHelper.push(data = this)
        })
    }

    /**别名操作返回回调*/
    override fun onAliasOperatorResult(context: Context?, jPushMessage: JPushMessage?) {
        super.onAliasOperatorResult(context, jPushMessage)
        L.i("[onAliasOperatorResult] ${jPushMessage.isSucceed()} $jPushMessage".apply {
            DslFileHelper.push(data = this)
        })

        vmCore<JPushModel>().aliasMessageData.postValue(jPushMessage)
    }

    /**手机号码设置操作返回回调*/
    override fun onMobileNumberOperatorResult(context: Context?, jPushMessage: JPushMessage?) {
        super.onMobileNumberOperatorResult(context, jPushMessage)
        L.i("[onMobileNumberOperatorResult] ${jPushMessage.isSucceed()} $jPushMessage".apply {
            DslFileHelper.push(data = this)
        })

        vmCore<JPushModel>().mobileMessageData.postValue(jPushMessage)
    }

    override fun onNotificationSettingsCheck(context: Context?, isOn: Boolean, source: Int) {
        super.onNotificationSettingsCheck(context, isOn, source)
        L.i("[onNotificationSettingsCheck] isOn:$isOn,source:$source".apply {
            DslFileHelper.push(data = this)
        })
    }
}

/**消息是否成功
 * 错误码:https://docs.jiguang.cn/jpush/client/Android/android_api/#_153
 * */
fun JPushMessage?.isSucceed() = this?.errorCode == 0