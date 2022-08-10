package com.angcyo.wifip2p

import android.content.Intent
import com.angcyo.library.component.BaseService
import com.angcyo.wifip2p.data.WifiP2pDeviceWrap

/**
 * [WifiP2p] 服务, 用于被发现
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/08/08
 */
class WifiP2pService : BaseService() {

    companion object {

        /**强制加上的[serviceType]*/
        const val TYPE = ".p2p"

        const val KEY_TYPE = "key_type"
        const val KEY_DATA = "key_data"

        /**停止服务*/
        const val TYPE_STOP = -1

        /**启动服务*/
        const val TYPE_START = 1

        /**发现服务*/
        const val TYPE_DISCOVER = 2

        /**连接设备*/
        const val TYPE_CONNECT = 3
    }

    val wifiP2p = WifiP2p()

    override fun onCreate() {
        super.onCreate()
        wifiP2p.initialize(this)
    }

    override fun handleIntent(intent: Intent) {
        when (intent.getIntExtra(KEY_TYPE, 0)) {
            TYPE_START -> {
                wifiP2p.serviceData = intent.getParcelableExtra(KEY_DATA)
                wifiP2p.startService()
            }
            TYPE_STOP -> {
                wifiP2p.stopService()
            }
            TYPE_DISCOVER -> {
                wifiP2p.discoverService()
            }
            TYPE_CONNECT -> {
                val device: WifiP2pDeviceWrap? = intent.getParcelableExtra(KEY_DATA)
                device?.let {
                    wifiP2p.connectDevice(it)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        wifiP2p.stopService()
    }
}