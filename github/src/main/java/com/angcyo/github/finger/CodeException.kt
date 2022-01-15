package com.angcyo.github.finger

import androidx.annotation.IntDef

/**
 * 错误异常码
 */
object CodeException {

    /*系统API小于23*/
    const val SYSTEM_API_ERROR = 0x1

    /*没有指纹识别权限*/
    const val PERMISSION_DENIED_ERROE = 0x2

    /*没有指纹识别模块*/
    const val HARDWARE_MISSIING_ERROR = 0x3

    /*没有开启锁屏密码*/
    const val KEYGUARDSECURE_MISSIING_ERROR = 0x4

    /*没有指纹录入*/
    const val NO_FINGERPRINTERS_ENROOLED_ERROR = 0x5

    /*连续多次指纹识别失败*/
    const val FINGERPRINTERS_FAILED_ERROR = 0x6

    /*该次指纹识别失败*/
    const val FINGERPRINTERS_RECOGNIZE_FAILED = 0x7

    @IntDef(
        SYSTEM_API_ERROR,
        PERMISSION_DENIED_ERROE,
        HARDWARE_MISSIING_ERROR,
        KEYGUARDSECURE_MISSIING_ERROR,
        NO_FINGERPRINTERS_ENROOLED_ERROR,
        FINGERPRINTERS_FAILED_ERROR,
        FINGERPRINTERS_RECOGNIZE_FAILED
    )
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class CodeEp
}