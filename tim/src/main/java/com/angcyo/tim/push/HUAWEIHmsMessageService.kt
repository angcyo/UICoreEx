package com.angcyo.tim.push

import android.content.Context
import android.net.Uri
import android.os.Bundle
import com.angcyo.core.vmApp
import com.angcyo.library.L
import com.angcyo.tim.model.PushModel
import com.angcyo.tim.util.BrandUtil
import com.huawei.hms.push.HmsMessageService
import com.huawei.hms.push.RemoteMessage

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/12/30
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class HUAWEIHmsMessageService : HmsMessageService() {

    companion object {
        /** 华为离线推送角标 */
        fun updateBadge(context: Context, number: Int) {
            if (!BrandUtil.isBrandHuawei()) {
                return
            }
            L.i("huawei badge = $number")
            try {
                val extra = Bundle()
                extra.putString("package", "com.tencent.qcloud.tim.tuikit")
                extra.putString("class", "com.tencent.qcloud.tim.demo.SplashActivity")
                extra.putInt("badgenumber", number)
                context.contentResolver.call(
                    Uri.parse("content://com.huawei.android.launcher.settings/badge/"),
                    "change_badge",
                    null,
                    extra
                )
            } catch (e: Exception) {
                L.w("huawei badge exception: " + e.localizedMessage)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        L.i("onMessageReceived message=$message")
    }

    override fun onMessageSent(msgId: String) {
        L.i("onMessageSent msgId=$msgId")
    }

    override fun onSendError(msgId: String, exception: Exception?) {
        L.i("onSendError msgId=$msgId")
    }

    override fun onNewToken(token: String) {
        L.i("onNewToken token=$token")
        vmApp<PushModel>().setPushTokenToTIM(token)
    }

    override fun onTokenError(exception: Exception) {
        L.i("onTokenError exception=$exception")
    }

    override fun onMessageDelivered(msgId: String, exception: Exception?) {
        L.i("onMessageDelivered msgId=$msgId")
    }
}