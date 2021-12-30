package com.angcyo.tim.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.angcyo.core.vmApp
import com.angcyo.library.L
import com.angcyo.tim.model.PushModel
import com.heytap.msp.push.callback.ICallBackResultService

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/12/30
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class OPPOPushImpl : ICallBackResultService {

    override fun onRegister(responseCode: Int, registerID: String) {
        L.i("onRegister responseCode: $responseCode registerID: $registerID")
        vmApp<PushModel>().setPushTokenToTIM(registerID)
    }

    override fun onUnRegister(responseCode: Int) {
        L.i("onUnRegister responseCode: $responseCode")
    }

    override fun onSetPushTime(responseCode: Int, s: String) {
        L.i("onSetPushTime responseCode: $responseCode s: $s")
    }

    override fun onGetPushStatus(responseCode: Int, status: Int) {
        L.i(Int, "onGetPushStatus responseCode: $responseCode status: $status")
    }

    override fun onGetNotificationStatus(responseCode: Int, status: Int) {
        L.i("onGetNotificationStatus responseCode: $responseCode status: $status")
    }

    fun createNotificationChannel(context: Context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = "oppotest"
            val description = "this is opptest"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("tuikit", name, importance)
            channel.description = description
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = context.getSystemService(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(channel)
        }
    }
}