package com.angcyo.engrave.firmware

/**
 * 固件异常
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/11/14
 */
class FirmwareException(message: String?, val type: Int) : Exception(message) {
    companion object {
        /**MD5验证失败*/
        const val TYPE_MD5 = 1

        /**非lpbin格式*/
        const val TYPE_LPBIN = 2

        /**更新范围不匹配*/
        const val TYPE_RANGE = 3
    }
}