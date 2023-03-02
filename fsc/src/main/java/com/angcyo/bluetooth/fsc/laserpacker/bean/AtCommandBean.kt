package com.angcyo.bluetooth.fsc.laserpacker.bean

/**
 * AT指令返回值
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/02
 */
data class AtCommandBean(
    /**设备地址*/
    val address: String,
    /**发送的at指令*/
    val command: String?,
    /**返回的参数*/
    val param: String?,
    val type: Int,
    val status: Int
)
