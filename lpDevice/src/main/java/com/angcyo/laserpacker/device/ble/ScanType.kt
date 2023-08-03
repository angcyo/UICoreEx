package com.angcyo.laserpacker.device.ble

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/08/02
 */
data class ScanType(
    val type: Int,
    val text: String
) {
    companion object {
        const val TYPE_WIFI = 1
        const val TYPE_BLE = 2
    }
}
