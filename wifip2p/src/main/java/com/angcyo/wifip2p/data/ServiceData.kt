package com.angcyo.wifip2p.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 服务数据
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/08/08
 */
@Parcelize
data class ServiceData(
    /**
     * 实例的名字
     * [android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo.newInstance]
     * */
    var instanceName: String,
    /**
     * 服务类型
     * [android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo.newInstance]
     * */
    var serviceType: String,
    /**
     * [android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo.newInstance]
     * */
    var txtRecord: Map<String, String> = emptyMap()
) : Parcelable
