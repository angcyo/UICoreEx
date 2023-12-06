package com.angcyo.bluetooth.fsc

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.angcyo.bluetooth.fsc.core.WifiDeviceScan
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.http.tcp.Tcp
import com.angcyo.http.tcp.TcpConnectInfo
import com.angcyo.http.tcp.TcpDevice
import com.angcyo.http.tcp.TcpState
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.component.hawk.LibLpHawkKeys
import com.angcyo.library.ex.nowTime
import com.angcyo.viewmodel.IViewModel
import com.angcyo.viewmodel.MutableOnceLiveData
import com.angcyo.viewmodel.updateThis
import com.angcyo.viewmodel.updateValue
import com.angcyo.viewmodel.vmData
import com.angcyo.viewmodel.vmDataNull
import com.angcyo.viewmodel.vmDataOnce
import java.net.Proxy

/**
 * WIFI收发指令
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/05/06
 */
class WifiApiModel : ViewModel(), IViewModel {

    companion object {

        /**是否使用强制的wifi配置信息进行连接, 否则使用上一次连接的设备进行连接*/
        val forceUseWifiConnect: Boolean
            get() = (LibLpHawkKeys.enableWifiConfig && LibLpHawkKeys.wifiAddress?.contains(".") == true)

        /**配置的wifi地址信息*/
        val wifiAddressInfo: List<String>
            get() = LibLpHawkKeys.wifiAddress?.split(":") ?: emptyList()

        /**是否要使用wifi进行数据传输传输*/
        fun useWifi(): Boolean {
            if (forceUseWifiConnect) {
                return true
            }
            /*return lpBoxOf(DeviceConnectEntity::class).findLastList()
                .lastOrNull()?.isWifiConnect == true*/
            return HawkEngraveKeys.lastWifiConnect
        }
    }

    /**TCP连接状态监听*/
    val tcpStateData = vmDataNull<TcpState>(null)

    /**TCP设备连接状态的通知*/
    val tcpConnectDeviceOnceData = vmDataOnce<TcpDevice>(null)

    /**连接上的tcp设备缓存*/
    val tcpConnectDeviceListData = vmData(mutableListOf<TcpDevice>())

    /**TCP状态监听*/
    val tcpListener = object : Tcp.TcpListener {
        override fun onConnectStateChanged(tcp: Tcp, state: TcpState, info: TcpConnectInfo?) {
            super.onConnectStateChanged(tcp, state, info)
            tcpStateData.updateValue(state)
            tcpConnectDeviceOnceData.updateValue(state.tcpDevice)
            if (state.state == Tcp.CONNECT_STATE_CONNECT_SUCCESS) {
                //连接成功
                if (!tcpConnectDeviceListData.value!!.contains(state.tcpDevice)) {
                    tcpConnectDeviceListData.value!!.add(state.tcpDevice)
                    tcpConnectDeviceListData.updateThis()
                }
            } else if (state.state == Tcp.CONNECT_STATE_DISCONNECT) {
                //连接断开
                tcpConnectDeviceListData.value!!.remove(state.tcpDevice)
                tcpConnectDeviceListData.updateThis()
            }
        }
    }

    /**tcp核心操作*/
    var tcp = createTcp()

    /**扫描状态回调*/
    val scanStateOnceData = vmDataOnce<Int>()

    /**扫描到的tcp设备, 不重复的设备*/
    val tcpDeviceOnceData: MutableOnceLiveData<TcpDevice?> = vmDataOnce(null)

    /**扫描到的tcp设备列表, 不重复的设备*/
    val tcpScanDeviceList = mutableListOf<TcpDevice>()

    /**局域网端口扫描*/
    val wifiDeviceScan = WifiDeviceScan().apply {
        scanStateAction = {
            //状态
            scanStateOnceData.updateValue(it)
        }
        scanDeviceAction = {
            //扫描到的设备
            if (!tcpScanDeviceList.contains(it)) {
                tcpScanDeviceList.add(it)
                tcpDeviceOnceData.updateValue(it)
            }
        }
    }

    /**当前的扫描状态*/
    val scanState: Int
        get() = wifiDeviceScan._state

    private fun createTcp(): Tcp {
        return Tcp().apply {
            proxy = Proxy.NO_PROXY
            listeners.add(tcpListener)
            initTcpConfig(this)
        }
    }

    /**初始化配置*/
    @CallPoint
    fun initTcpConfig(tcp: Tcp = this.tcp) {
        /*val wifiAddress = LibLpHawkKeys.wifiAddress
        val list = wifiAddress?.split(":")

        tcp.address = list?.getOrNull(0)
        list?.getOrNull(1)?.toIntOrNull()?.let {
            tcp.port = it
        }*/
        tcp.bufferSize = LibLpHawkKeys.wifiBufferSize
        tcp.sendDelay = LibLpHawkKeys.wifiSendDelay
        tcp.sendDelayByteCount = LibLpHawkKeys.wifiSendDelayByteCount
    }

    /**网络是否连接上了*/
    fun isTcpConnected(): Boolean {
        return tcp.isConnected()
    }

    /**连接开始的时间*/
    var connectStartTime: Long = 0L

    /**设备的连接状态*/
    fun connectState(device: TcpDevice?): Int {
        return device?.connectState ?: Tcp.CONNECT_STATE_DISCONNECT
    }

    /**连接设备
     * [info] true 表示自动连接, false 表示手动连接
     * */
    fun connect(device: TcpDevice, info: TcpConnectInfo?) {
        HawkEngraveKeys.forceUseWifi = true
        HawkEngraveKeys.lastWifiConnect = true
        HawkEngraveKeys.lastWifiIp = device.address
        if (tcp.tcpDevice == null || tcp.tcpDevice == device) {
            tcp.tcpDevice = device
        } else {
            tcp.cancel(TcpConnectInfo())
            tcp.listeners.remove(tcpListener)

            //重新建立连接
            tcp = createTcp()
            tcp.tcpDevice = device
        }
        connectStartTime = nowTime()
        tcp.connect(info)
    }

    /**断开连接
     * [data] 是否是主动断开*/
    fun disconnect(info: TcpConnectInfo?) {
        tcp.tcpDevice?.let {
            disconnect(it, info)
        }
    }

    /**断开所有设备*/
    fun disconnectAll(info: TcpConnectInfo?) {
        tcp.tcpDevice?.let {
            disconnect(it, info)
        }
        tcpConnectDeviceListData.value!!.forEach {
            disconnect(it, info)
        }
    }

    /**断开设备, 但是之后通知
     * [info] 是否是主动断开
     * */
    fun disconnect(device: TcpDevice?, info: TcpConnectInfo?) {
        device ?: return
        tcp.cancel(info) //取消连接, 这里应该要支持多设备连接
    }

    //---

    /**端口扫描*/
    fun startScan(lifecycleOwner: LifecycleOwner): Boolean {
        if (scanState == WifiDeviceScan.STATE_SCAN_START) {
            return false
        }
        tcpScanDeviceList.clear()
        wifiDeviceScan.lifecycleOwner = lifecycleOwner
        return wifiDeviceScan.startScan(HawkEngraveKeys.wifiPort)
    }

    /**停止端口扫描*/
    fun stopScan() {
        if (scanState == WifiDeviceScan.STATE_SCAN_START) {
            wifiDeviceScan.cancel()
        }
    }

}