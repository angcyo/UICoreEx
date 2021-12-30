package com.angcyo.tim.push

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import com.angcyo.core.vmApp
import com.angcyo.library.L
import com.angcyo.tim.model.PushModel
import com.xiaomi.mipush.sdk.*

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/12/30
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class XiaomiMsgReceiver : PushMessageReceiver() {

    var mRegId: String? = null

    override fun onReceivePassThroughMessage(context: Context?, miPushMessage: MiPushMessage?) {
        L.d("onReceivePassThroughMessage is called. ")
    }

    override fun onNotificationMessageClicked(context: Context?, miPushMessage: MiPushMessage) {
        L.d("onNotificationMessageClicked miPushMessage $miPushMessage")
        val extra = miPushMessage.extra
        val ext = extra["ext"]
        if (TextUtils.isEmpty(ext)) {
            L.w("onNotificationMessageClicked: no extra data found")
            return
        }
        val bundle = Bundle()
        bundle.putString("ext", ext)
        //TUIUtils.startActivity("MainActivity", bundle)
    }

    override fun onNotificationMessageArrived(context: Context?, miPushMessage: MiPushMessage?) {
        L.d("onNotificationMessageArrived is called. ")
    }

    override fun onReceiveRegisterResult(
        context: Context?,
        miPushCommandMessage: MiPushCommandMessage
    ) {
        L.d("onReceiveRegisterResult is called. $miPushCommandMessage")
        val command = miPushCommandMessage.command
        val arguments = miPushCommandMessage.commandArguments
        val cmdArg1 = if (arguments != null && arguments.size > 0) arguments[0] else null
        L.d("cmd: " + command + " | arg: " + cmdArg1 + " | result: " + miPushCommandMessage.resultCode + " | reason: " + miPushCommandMessage.reason)
        if (MiPushClient.COMMAND_REGISTER == command) {
            if (miPushCommandMessage.resultCode == ErrorCode.SUCCESS.toLong()) {
                mRegId = cmdArg1
            }
        }
        L.d("regId: $mRegId")
        vmApp<PushModel>().setPushTokenToTIM(mRegId)
    }

    override fun onCommandResult(context: Context?, miPushCommandMessage: MiPushCommandMessage?) {
        super.onCommandResult(context, miPushCommandMessage)
    }
}