package com.angcyo.github.finger

/**
 * Created by Administrator on 2016/12/31.
 */
class FPerException(val code: Int /*错误码*/) : RuntimeException() {

    /*显示的信息*/
    val displayMessage: String
        get() = when (code) {
            CodeException.SYSTEM_API_ERROR -> "系统API小于23"
            CodeException.PERMISSION_DENIED_ERROE -> "没有指纹识别权限"
            CodeException.HARDWARE_MISSIING_ERROR -> "没有指纹识别模块"
            CodeException.KEYGUARDSECURE_MISSIING_ERROR -> "没有开启锁屏密码"
            CodeException.NO_FINGERPRINTERS_ENROOLED_ERROR -> "没有指纹录入"
            CodeException.FINGERPRINTERS_FAILED_ERROR -> "指纹认证失败，请稍后再试"
            CodeException.FINGERPRINTERS_RECOGNIZE_FAILED -> "指纹识别失败，请重试"
            else -> ""
        }

}