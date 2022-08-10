package com.angcyo.wifip2p.data

import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.os.Parcelable
import com.angcyo.wifip2p.WifiP2p
import com.angcyo.wifip2p.task.WifiP2pReceiveRunnable
import kotlinx.parcelize.Parcelize

/**
 * [WifiP2pDevice]数据包裹
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/08/09
 */
@Parcelize
data class WifiP2pDeviceWrap(
    var instanceName: String?,
    var registrationType: String?,
    var sourceDevice: WifiP2pDevice,
    var txtRecord: HashMap<String, String> = hashMapOf(),
    //连上之后的网络信息
    var wifiP2pInfo: WifiP2pInfo? = null
) : Parcelable

fun WifiP2pDevice.wrap(
    instanceName: String? = null,
    registrationType: String? = null
): WifiP2pDeviceWrap = WifiP2pDeviceWrap(instanceName, registrationType, this)

/**服务端口*/
val WifiP2pDeviceWrap.servicePort: Int
    get() = txtRecord[WifiP2p.KEY_SERVER_PORT]?.toIntOrNull()
        ?: WifiP2pReceiveRunnable.SERVER_PORT