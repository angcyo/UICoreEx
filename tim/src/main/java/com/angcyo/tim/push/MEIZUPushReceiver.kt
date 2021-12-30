package com.angcyo.tim.push

import android.content.Context
import android.content.Intent
import com.angcyo.core.vmApp
import com.angcyo.library.L
import com.angcyo.tim.model.PushModel
import com.meizu.cloud.pushsdk.MzPushMessageReceiver
import com.meizu.cloud.pushsdk.handler.MzPushMessage
import com.meizu.cloud.pushsdk.notification.PushNotificationBuilder
import com.meizu.cloud.pushsdk.platform.message.*

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/12/30
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class MEIZUPushReceiver : MzPushMessageReceiver() {

    override fun onMessage(context: Context?, s: String) {
        L.i("onMessage method1 msg = $s")
    }

    override fun onMessage(context: Context?, message: String, platformExtra: String) {
        L.i("onMessage method2 msg = $message, platformExtra = $platformExtra")
    }

    override fun onMessage(context: Context?, intent: Intent) {
        val content = intent.extras.toString()
        L.i("flyme3 onMessage = $content")
    }

    override fun onUpdateNotificationBuilder(pushNotificationBuilder: PushNotificationBuilder?) {
        super.onUpdateNotificationBuilder(pushNotificationBuilder)
    }

    override fun onNotificationClicked(context: Context?, mzPushMessage: MzPushMessage) {
        L.i("onNotificationClicked mzPushMessage $mzPushMessage")
    }

    override fun onNotificationArrived(context: Context?, mzPushMessage: MzPushMessage?) {
        super.onNotificationArrived(context, mzPushMessage)
    }

    override fun onNotificationDeleted(context: Context?, mzPushMessage: MzPushMessage?) {
        super.onNotificationDeleted(context, mzPushMessage)
    }

    override fun onNotifyMessageArrived(context: Context?, s: String?) {
        super.onNotifyMessageArrived(context, s)
    }

    override fun onPushStatus(context: Context?, pushSwitchStatus: PushSwitchStatus?) {}

    override fun onRegisterStatus(context: Context?, registerStatus: RegisterStatus) {
        L.i("onRegisterStatus token = " + registerStatus.pushId)
        vmApp<PushModel>().setPushTokenToTIM(registerStatus.pushId)
    }

    override fun onUnRegisterStatus(context: Context?, unRegisterStatus: UnRegisterStatus?) {}

    override fun onSubTagsStatus(context: Context?, subTagsStatus: SubTagsStatus?) {}

    override fun onSubAliasStatus(context: Context?, subAliasStatus: SubAliasStatus?) {}

    override fun onRegister(context: Context?, s: String?) {}

    override fun onUnRegister(context: Context?, b: Boolean) {}
}