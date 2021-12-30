package com.angcyo.tim.model

import com.angcyo.core.lifecycle.LifecycleViewModel
import com.angcyo.library.L
import com.angcyo.tim.util.BrandUtil
import com.angcyo.tim.util.PrivateConstants
import com.angcyo.viewmodel.vmDataNull
import com.tencent.imsdk.v2.V2TIMCallback
import com.tencent.imsdk.v2.V2TIMManager
import com.tencent.imsdk.v2.V2TIMOfflinePushConfig

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/12/30
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class PushModel : LifecycleViewModel() {

    /**推送的token, 某些推送需要*/
    val pushTokenData = vmDataNull<String>()

    /**将推送Token设置给TIM*/
    fun setPushTokenToTIM(token: String?) {
        pushTokenData.value = token
        if (token.isNullOrEmpty()) {
            L.i("setPushTokenToTIM third token is empty")
            return
        }
        var v2TIMOfflinePushConfig: V2TIMOfflinePushConfig? = null
        v2TIMOfflinePushConfig = if (BrandUtil.isBrandXiaoMi()) {
            V2TIMOfflinePushConfig(PrivateConstants.XM_PUSH_BUZID, token)
        } else if (BrandUtil.isBrandHuawei()) {
            V2TIMOfflinePushConfig(PrivateConstants.HW_PUSH_BUZID, token)
        } else if (BrandUtil.isBrandMeizu()) {
            V2TIMOfflinePushConfig(PrivateConstants.MZ_PUSH_BUZID, token)
        } else if (BrandUtil.isBrandOppo()) {
            V2TIMOfflinePushConfig(PrivateConstants.OPPO_PUSH_BUZID, token)
        } else if (BrandUtil.isBrandVivo()) {
            V2TIMOfflinePushConfig(PrivateConstants.VIVO_PUSH_BUZID, token)
        } else if (BrandUtil.isGoogleServiceSupport()) {
            V2TIMOfflinePushConfig(PrivateConstants.GOOGLE_FCM_PUSH_BUZID, token)
        } else {
            return
        }
        V2TIMManager.getOfflinePushManager()
            .setOfflinePushConfig(v2TIMOfflinePushConfig, object : V2TIMCallback {
                override fun onError(code: Int, desc: String) {
                    L.d("setOfflinePushToken err code = $code")
                }

                override fun onSuccess() {
                    L.d("setOfflinePushToken success")
                }
            })
    }

}