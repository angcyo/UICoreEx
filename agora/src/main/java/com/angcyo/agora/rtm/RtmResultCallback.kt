package com.angcyo.agora.rtm

import io.agora.rtm.ErrorInfo
import io.agora.rtm.ResultCallback

/**
 * 子线程回调
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/02/07
 */
class RtmResultCallback(val onResult: (errorInfo: ErrorInfo?) -> Unit = {}) : ResultCallback<Void> {
    override fun onSuccess(aVoid: Void?) {
        onResult(null)
    }

    override fun onFailure(errorInfo: ErrorInfo?) {
        onResult(errorInfo)
    }
}