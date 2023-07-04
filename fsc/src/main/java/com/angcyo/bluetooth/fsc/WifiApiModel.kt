package com.angcyo.bluetooth.fsc

import androidx.lifecycle.ViewModel
import com.angcyo.http.tcp.Tcp
import com.angcyo.http.tcp.TcpState
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.component.hawk.LibLpHawkKeys
import com.angcyo.library.ex.nowTime
import com.angcyo.viewmodel.IViewModel
import com.angcyo.viewmodel.updateValue
import com.angcyo.viewmodel.vmDataNull

/**
 * WIFI收发指令
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/05/06
 */
class WifiApiModel : ViewModel(), IViewModel {

    companion object {

        /**是否使用wifi连接设备*/
        val isUseWifiConnect: Boolean
            get() = useWifi()

        /**配置的wifi地址信息*/
        val wifiAddressInfo: List<String>
            get() = LibLpHawkKeys.wifiAddress?.split(":") ?: emptyList()

        /**是否要使用wifi传输*/
        fun useWifi(): Boolean {
            return LibLpHawkKeys.enableWifiConfig && LibLpHawkKeys.wifiAddress?.contains(".") == true
        }
    }

    /**TCP连接状态监听*/
    val tcpStateData = vmDataNull<TcpState>(null)

    val tcpListener = object : Tcp.TcpListener {
        override fun onConnectStateChanged(tcp: Tcp, state: TcpState) {
            super.onConnectStateChanged(tcp, state)
            tcpStateData.updateValue(state)
        }
    }

    /**tcp核心操作*/
    val tcp = Tcp().apply {
        listeners.add(tcpListener)
    }

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

    /**连接开始的时间*/
    var connectStartTime: Long = 0L

    /**连接设备*/
    fun connect(data: Any?) {
        connectStartTime = nowTime()
        tcp.connect(data)
    }

}