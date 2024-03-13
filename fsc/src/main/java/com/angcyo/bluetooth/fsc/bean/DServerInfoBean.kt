package com.angcyo.bluetooth.fsc.bean

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2024/03/13
 *
 *  "firmware_version": "LX2-V90000-XXXXXXXX-24:03:07_17:26:33",
 *  "firmware_name": "LX2",
 *  "firmware_code": 90000,
 *  "firmware_data": "24:03:07_17:26:33",
 *  "firmware_hardware": "xxxxxxxx"
 */
data class DServerInfoBean(
    val firmware_code: Int? = null,
    val firmware_hardware: String? = null,
    val firmware_name: String? = null,
    val firmware_version: String? = null,
)
