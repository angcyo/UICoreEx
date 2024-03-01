package com.angcyo.bluetooth.fsc.laserpacker.bean

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/10/08
 */
data class AddDeviceConfigBean(
    /**显示的名称*/
    val name: String, //"LP2",
    /**类型*/
    val type: Int, //2,
    /**图标url*/
    val url: String, //"assets/images/device_l2.png",
    /**需要打开的页面
     * [com.hingin.flutter.FlutterHelper#PAGE_BLE_SCAN]
     * [com.hingin.flutter.FlutterHelper#PAGE_DEVICE_DISCOVERY]
     * */
    val channelPage: String, //, MethodChannelParam.deviceDiscovery
    /**是否只在debug模式下显示*/
    val debug: Boolean = false,
) {
    companion object {
        const val CHANNEL_PAGE_HTTP = "apDiscovery"
        const val CHANNEL_PAGE_WIFI = "deviceDiscovery"
        const val CHANNEL_PAGE_BLE = "bleScan"
    }
}
