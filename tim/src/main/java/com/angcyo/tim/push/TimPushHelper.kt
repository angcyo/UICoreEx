package com.angcyo.tim.push

import android.content.Context
import com.angcyo.core.component.BackgroundModel
import com.angcyo.core.vmApp
import com.angcyo.http.rx.doBack
import com.angcyo.library.L
import com.angcyo.tim.model.ConversationModel
import com.angcyo.tim.model.PushModel
import com.angcyo.tim.util.BrandUtil
import com.angcyo.tim.util.PrivateConstants
import com.heytap.msp.push.HeytapPushManager
import com.huawei.agconnect.config.AGConnectServicesConfig
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.common.ApiException
import com.huawei.hms.push.HmsMessaging
import com.meizu.cloud.pushsdk.PushManager
import com.meizu.cloud.pushsdk.util.MzSystemUtils
import com.tencent.imsdk.v2.V2TIMCallback
import com.tencent.imsdk.v2.V2TIMManager
import com.tencent.imsdk.v2.V2TIMValueCallback
import com.vivo.push.PushClient
import com.xiaomi.mipush.sdk.MiPushClient

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/12/30
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
object TimPushHelper {

    /**离线推送*/
    fun initPush(context: Context) {
        HeytapPushManager.init(context, true)

        if (BrandUtil.isBrandXiaoMi()) {
            // 小米离线推送
            MiPushClient.registerPush(
                context,
                PrivateConstants.XM_PUSH_APPID,
                PrivateConstants.XM_PUSH_APPKEY
            )
        } else if (BrandUtil.isBrandHuawei()) {
            // 华为离线推送，设置是否接收Push通知栏消息调用示例
            HmsMessaging.getInstance(context).turnOnPush().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    L.i("huawei turnOnPush Complete")
                } else {
                    L.e("huawei turnOnPush failed: ret=" + task.exception.message)
                }
            }
        } else if (MzSystemUtils.isBrandMeizu(context)) {
            // 魅族离线推送
            PushManager.register(
                context,
                PrivateConstants.MZ_PUSH_APPID,
                PrivateConstants.MZ_PUSH_APPKEY
            )
        } else if (BrandUtil.isBrandVivo()) {
            // vivo离线推送
            PushClient.getInstance(context).initialize()
        } else if (HeytapPushManager.isSupportPush()) {
            // oppo离线推送，因为需要登录成功后向我们后台设置token，所以注册放在MainActivity中做
        } else if (BrandUtil.isGoogleServiceSupport()) {
            /*FirebaseInstanceId.getInstance().getInstanceId()
                    .addOnCompleteListener(new com.google.android.gms.tasks.OnCompleteListener<InstanceIdResult>() {
                        @Override
                        public void onComplete(Task<InstanceIdResult> task) {
                            if (!task.isSuccessful()) {
                                L.w(TAG, "getInstanceId failed exception = " + task.getException());
                                return;
                            }
                            // Get new Instance ID token
                            String token = task.getResult().getToken();
                            L.i(TAG, "google fcm getToken = " + token);

                            ThirdPushTokenMgr.getInstance().setThirdPushToken(token);
                            vmApp<PushModel>().setPushTokenToTIM(token)
                        }
                    });*/
        }

        vmApp<BackgroundModel>().backgroundData.observeForever {
            if (it) {
                V2TIMManager.getConversationManager()
                    .getTotalUnreadMessageCount(object : V2TIMValueCallback<Long> {
                        override fun onSuccess(count: Long) {
                            V2TIMManager.getOfflinePushManager()
                                .doBackground(count.toInt(), object : V2TIMCallback {
                                    override fun onError(code: Int, desc: String) {
                                        L.e("doBackground err = $code, desc = $desc")
                                    }

                                    override fun onSuccess() {
                                        L.i("doBackground success")
                                    }
                                })
                        }

                        override fun onError(code: Int, desc: String?) {
                            L.e("getTotalUnreadMessageCount err = $code, desc = $desc")
                        }
                    })
            } else {
                V2TIMManager.getOfflinePushManager().doForeground(object : V2TIMCallback {
                    override fun onError(code: Int, desc: String) {
                        L.e("doForeground err = $code, desc = $desc")
                    }

                    override fun onSuccess() {
                        L.i("doForeground success")
                    }
                })
            }
        }

        vmApp<ConversationModel>().conversationCountData.observeForever {
            // 华为离线推送角标
            HUAWEIHmsMessageService.updateBadge(context, it.toInt())
        }
    }

    fun preparePushToken(context: Context) {
        vmApp<PushModel>().setPushTokenToTIM(vmApp<PushModel>().pushTokenData.value)

        if (BrandUtil.isBrandHuawei()) {
            // 华为离线推送
            doBack {
                try {
                    // read from agconnect-services.json
                    val appId =
                        AGConnectServicesConfig.fromContext(context).getString("client/app_id")
                    val token = HmsInstanceId.getInstance(context).getToken(appId, "HCM")
                    L.i("huawei get token:$token")
                    if (!token.isNullOrEmpty()) {
                        vmApp<PushModel>().setPushTokenToTIM(token)
                    }
                } catch (e: ApiException) {
                    L.e("huawei get token failed, $e")
                }
            }
        } else if (BrandUtil.isBrandVivo()) {
            // vivo离线推送
            L.i("vivo support push: " + PushClient.getInstance(context.applicationContext).isSupport)
            PushClient.getInstance(context.applicationContext).turnOnPush { state ->
                if (state == 0) {
                    val regId = PushClient.getInstance(context.applicationContext).regId
                    L.i("vivopush open vivo push success regId = $regId")
                    vmApp<PushModel>().setPushTokenToTIM(regId)
                } else {
                    // 根据vivo推送文档说明，state = 101 表示该vivo机型或者版本不支持vivo推送，链接：https://dev.vivo.com.cn/documentCenter/doc/156
                    L.i("vivopush open vivo push fail state = $state")
                }
            }
        } else if (HeytapPushManager.isSupportPush()) {
            // oppo离线推送
            val oppo = OPPOPushImpl()
            oppo.createNotificationChannel(context)
            // oppo接入文档要求，应用必须要调用init(...)接口，才能执行后续操作。
            HeytapPushManager.init(context, false)
            HeytapPushManager.register(
                context,
                PrivateConstants.OPPO_PUSH_APPKEY,
                PrivateConstants.OPPO_PUSH_APPSECRET,
                oppo
            )
        } else if (BrandUtil.isGoogleServiceSupport()) {
            // 谷歌推送
        }
    }

}