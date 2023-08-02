package com.angcyo.bluetooth.fsc

import androidx.lifecycle.ViewModel
import com.angcyo.http.tcp.Tcp
import com.angcyo.http.tcp.TcpDevice
import com.angcyo.http.tcp.TcpState
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.component.hawk.LibLpHawkKeys
import com.angcyo.library.ex.nowTime
import com.angcyo.objectbox.findLastList
import com.angcyo.objectbox.laser.pecker.entity.DeviceConnectEntity
import com.angcyo.objectbox.laser.pecker.lpBoxOf
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

        /**是否要使用wifi传输*/
        fun useWifi(): Boolean {
            //return LibLpHawkKeys.enableWifiConfig && LibLpHawkKeys.wifiAddress?.contains(".") == true
            //return vmApp<WifiApiModel>().isTcpConnected()
            return lpBoxOf(DeviceConnectEntity::class).findLastList()
                .lastOrNull()?.isWifiConnect == true
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
    var tcp = createTcp()

    private fun createTcp(): Tcp {
        return Tcp().apply {
            listeners.add(tcpListener)
            initTcpConfig(this)
        }
    }

    /**初始化配置*/
    @CallPoint
    fun initTcpConfig(tcp: Tcp) {
        /*val wifiAddress = LibLpHawkKeys.wifiAddress
        val list = wifiAddress?.split(":")

        tcp.address = list?.getOrNull(0)
        list?.getOrNull(1)?.toIntOrNull()?.let {
            tcp.port = it
        }*/
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

    /**连接设备
     * [data] true 表示自动连接, false 表示手动连接
     * */
    fun connect(device: TcpDevice, data: Any?) {
        if (tcp.tcpDevice == null || tcp.tcpDevice == device) {
            tcp.tcpDevice = device
        } else {
            tcp.cancel()
            tcp.listeners.remove(tcpListener)

            //重新建立连接
            tcp = createTcp()
            tcp.tcpDevice = device
        }
        connectStartTime = nowTime()
        tcp.connect(data)
    }

    /**断开连接*/
    fun disconnect(data: Any?) {
        tcp.cancel(data)
    }

}