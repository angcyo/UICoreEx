package com.angcyo.tim.push

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.TextUtils
import androidx.activity.result.ActivityResultCaller
import com.angcyo.http.base.fromJson
import com.angcyo.library.L
import com.angcyo.library.app
import com.angcyo.tim.bean.ChatInfoBean
import com.angcyo.tim.bean.OfflineMessageBean
import com.angcyo.tim.bean.OfflineMessageContainerBean
import com.angcyo.tim.helper.ConversationHelper
import com.angcyo.tim.util.BrandUtil
import com.xiaomi.mipush.sdk.MiPushMessage
import com.xiaomi.mipush.sdk.PushMessageHelper

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/12/30
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
object OfflineMessageDispatcher {

    fun parseOfflineMessage(intent: Intent?): OfflineMessageBean? {
        L.i("intent: $intent")
        if (intent == null) {
            return null
        }
        val bundle = intent.extras
        L.i("bundle: $bundle")
        return if (bundle == null) {
            val ext = VIVOPushMessageReceiverImpl.getParams()
            if (!ext.isNullOrEmpty()) {
                getOfflineMessageBeanFromContainer(ext)
            } else null
        } else {
            var ext = bundle.getString("ext")
            L.i("push custom data ext: $ext")
            if (TextUtils.isEmpty(ext)) {
                if (BrandUtil.isBrandXiaoMi()) {
                    ext = getXiaomiMessage(bundle)
                    return getOfflineMessageBeanFromContainer(ext)
                } else if (BrandUtil.isBrandOppo()) {
                    ext = getOPPOMessage(bundle)
                    return getOfflineMessageBean(ext)
                }
            } else {
                return getOfflineMessageBeanFromContainer(ext)
            }
            null
        }
    }

    private fun getXiaomiMessage(bundle: Bundle): String? {
        val miPushMessage = bundle.getSerializable(PushMessageHelper.KEY_MESSAGE) as MiPushMessage?
            ?: return null
        val extra: Map<*, *> = miPushMessage.extra
        return extra["ext"].toString()
    }

    private fun getOPPOMessage(bundle: Bundle): String? {
        val set = bundle.keySet()
        if (set != null) {
            for (key in set) {
                val value = bundle[key]
                L.i("push custom data key: $key value: $value")
                if (TextUtils.equals("entity", key)) {
                    return value.toString()
                }
            }
        }
        return null
    }

    private fun getOfflineMessageBeanFromContainer(ext: String?): OfflineMessageBean? {
        if (ext.isNullOrEmpty()) {
            return null
        }
        var bean: OfflineMessageContainerBean? = null
        try {
            bean = ext.fromJson()
        } catch (e: Exception) {
            L.w("getOfflineMessageBeanFromContainer: " + e.message)
        }
        return if (bean == null) {
            null
        } else offlineMessageBeanValidCheck(bean.entity)
    }

    private fun getOfflineMessageBean(ext: String?): OfflineMessageBean? {
        if (TextUtils.isEmpty(ext)) {
            return null
        }
        val bean: OfflineMessageBean? = ext.fromJson()
        return offlineMessageBeanValidCheck(bean)
    }

    private fun offlineMessageBeanValidCheck(bean: OfflineMessageBean?): OfflineMessageBean? {
        if (bean == null) {
            return null
        } else if (bean.version != 1
            || (bean.action != OfflineMessageBean.REDIRECT_ACTION_CHAT
                && bean.action != OfflineMessageBean.REDIRECT_ACTION_CALL)
        ) {
            val packageManager: PackageManager = app().packageManager
            val label =
                packageManager.getApplicationLabel(app().getApplicationInfo())
                    .toString()
            /*ToastUtil.toastLongMessage(
                app().getString(R.string.you_app)
                    .toString() + label + app().getString(R.string.low_version)
            )*/
            L.e("unknown version: " + bean.version.toString() + " or action: " + bean.action)
            return null
        }
        return bean
    }

    fun redirect(caller: ActivityResultCaller, bean: OfflineMessageBean): Boolean {
        if (bean.action == OfflineMessageBean.REDIRECT_ACTION_CHAT) {
            if (bean.sender.isNullOrEmpty()) {
                return true
            }
            //TUIUtils.startChat(bean.sender, bean.nickname, bean.chatType)
            ConversationHelper.conversationJump(caller, ChatInfoBean().apply {
                chatTitle = bean.title
                chatId = bean.sender //2021-12-8 使用 mobile -> code
                chatType = bean.chatType
            })

            return true
        } else if (bean.action == OfflineMessageBean.REDIRECT_ACTION_CALL) {
            redirectCall(bean)
        }
        return true
    }

    fun redirectCall(bean: OfflineMessageBean?) {
        if (bean?.content == null) {
            return
        }
        /*val model: CallModel = Gson().fromJson(bean.content, CallModel::class.java)
        L.i(OfflineMessageDispatcher.TAG, "bean: $bean model: $model")
        if (model != null) {
            model.sender = bean.sender
            model.data = bean.content
            val timeout: Long = V2TIMManager.getInstance().serverTime - bean.sendTime
            if (timeout >= model.timeout) {
                *//*ToastUtil.toastLongMessage(
                    app().getString(R.string.call_time_out)
                )*//*
            } else {
                if (TextUtils.isEmpty(model.groupId)) {
                    val mainIntent = Intent(app(), MainActivity::class.java)
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    app().startActivity(mainIntent)
                } else {
                    val info = V2TIMSignalingInfo()
                    info.inviteID = model.callId
                    info.inviteeList = model.invitedList
                    info.groupID = model.groupId
                    info.inviter = bean.sender
                    V2TIMManager.getSignalingManager()
                        .addInvitedSignaling(info, object : V2TIMCallback {
                            override fun onError(code: Int, desc: String) {
                                L.e("addInvitedSignaling code: $code desc: $desc")
                            }

                            override fun onSuccess() {
                                *//*val mainIntent = Intent(app(), MainActivity::class.java)
                                mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                app().startActivity(mainIntent)
                                TUIUtils.startCall(bean.sender, model.data)*//*
                            }
                        })
                }
            }
        }*/
    }
}