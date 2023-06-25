package com.angcyo.bluetooth.fsc

import androidx.lifecycle.ViewModel
import com.angcyo.http.tcp.Tcp
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.component.hawk.LibLpHawkKeys
import com.angcyo.viewmodel.IViewModel

/**
 * WIFI收发指令
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/05/06
 */
class WifiApiModel : ViewModel(), IViewModel {

    companion object {
        /**是否要使用wifi传输*/
        fun useWifi(): Boolean {
            return LibLpHawkKeys.enableWifiConfig && LibLpHawkKeys.wifiAddress?.contains(".") == true
        }
    }

    /**tcp核心操作*/
    val tcp = Tcp()

    /**初始化配置*/
    @CallPoint
    fun initTcpConfig() {
        val wifiAddress = LibLpHawkKeys.wifiAddress
        val list = wifiAddress?.split(":")

        tcp.address = list?.getOrNull(0)
        list?.getOrNull(1)?.toIntOrNull()?.let {
            tcp.port = it
        }
        tcp.bufferSize = LibLpHawkKeys.wifiBufferSize
        tcp.sendDelay = LibLpHawkKeys.wifiSendDelay
    }

    /**网络是否连接上了*/
    fun isTcpConnected(): Boolean {
        return tcp.isConnected()
    }

}