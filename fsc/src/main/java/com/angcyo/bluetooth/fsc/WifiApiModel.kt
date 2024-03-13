package com.angcyo.bluetooth.fsc

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.angcyo.bluetooth.fsc.core.WifiDeviceScan
import com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.deviceType
import com.angcyo.bluetooth.fsc.laserpacker.host
import com.angcyo.core.vmApp
import com.angcyo.http.tcp.Tcp
import com.angcyo.http.tcp.TcpConnectInfo
import com.angcyo.http.tcp.TcpDevice
import com.angcyo.http.tcp.TcpState
import com.angcyo.library.L
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.app
import com.angcyo.library.component._delay
import com.angcyo.library.component.hawk.LibLpHawkKeys
import com.angcyo.library.component.runOnMainThread
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
            return HawkEngraveKeys.lastConnectDeviceType == LaserPeckerHelper.DEVICE_TYPE_WIFI
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

            //更新连接状态
            tcpScanDeviceList.find { state.tcpDevice.deviceName == it.deviceName }?.let {
                it.connectState = state.state
            }

            if (state.state == Tcp.CONNECT_STATE_CONNECT_SUCCESS) {
                //连接成功
                if (!tcpConnectDeviceListData.value!!.contains(state.tcpDevice)) {
                    tcpConnectDeviceListData.value!!.add(state.tcpDevice)
                    tcpConnectDeviceListData.updateThis()
                }
                vmApp<DeviceStateModel>().notifyDeviceConnect(LaserPeckerHelper.DEVICE_TYPE_WIFI)
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
    fun isWifiDeviceConnected(): Boolean {
        return httpDeviceConnectData.value != null || tcp.isConnected()
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
        HawkEngraveKeys.lastConnectDeviceType = device.deviceType
        if (device.deviceType == LaserPeckerHelper.DEVICE_TYPE_HTTP) {
            connectStartTime = nowTime()
            //断开旧的
            disconnectHttpDevice(null)
            connectHttpDevice(device)
        } else {
            disconnectHttpDevice(info)
            if (device.deviceType == LaserPeckerHelper.DEVICE_TYPE_WIFI) {
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
        }
    }

    /**断开连接
     * [data] 是否是主动断开*/
    fun disconnect(info: TcpConnectInfo?) {
        tcp.tcpDevice?.let {
            disconnect(it, info)
        }
        disconnectHttpDevice(info)
    }

    /**断开所有设备*/
    fun disconnectAll(info: TcpConnectInfo? = null) {
        tcp.tcpDevice?.let {
            disconnect(it, info)
        }
        tcpConnectDeviceListData.value!!.forEach {
            disconnect(it, info)
        }
        disconnectHttpDevice(info)
    }

    /**断开设备, 但是之后通知
     * [info] 是否是主动断开
     * */
    fun disconnect(device: TcpDevice?, info: TcpConnectInfo?) {
        device ?: return
        tcp.cancel(info) //取消连接, 这里应该要支持多设备连接
    }

    /**开始扫描设备*/
    fun startScan(lifecycleOwner: LifecycleOwner): Boolean {
        return if (HawkEngraveKeys.useOldWifiScan) {
            startIpScan(lifecycleOwner)
        } else {
            startDiscovery(lifecycleOwner)
        }
    }

    /**结束扫描设备*/
    fun stopScan() {
        if (HawkEngraveKeys.useOldWifiScan) {
            stopIpScan()
        } else {
            stopDiscovery()
        }
    }

    //---

    /**使用ip端口扫描设备*/
    fun startIpScan(lifecycleOwner: LifecycleOwner): Boolean {
        if (scanState == WifiDeviceScan.STATE_SCAN_START) {
            return false
        }
        tcpScanDeviceList.clear()
        wifiDeviceScan.lifecycleOwner = lifecycleOwner
        return wifiDeviceScan.startScan(HawkEngraveKeys.wifiPort)
    }

    /**停止端口扫描*/
    fun stopIpScan() {
        if (scanState == WifiDeviceScan.STATE_SCAN_START) {
            wifiDeviceScan.cancel()
        }
    }

    //region ---nsd---

    /**当前连接上的http设备*/
    val httpDeviceConnectData = vmDataNull<TcpDevice>()

    /**连接http设备*/
    fun connectHttpDevice(device: TcpDevice) {
        device.connectState = Tcp.CONNECT_STATE_CONNECT_SUCCESS
        httpDeviceConnectData.updateValue(device)
        tcpStateData.updateValue(TcpState(device, Tcp.CONNECT_STATE_CONNECT_SUCCESS))

        vmApp<DeviceStateModel>().notifyDeviceConnect(LaserPeckerHelper.DEVICE_TYPE_HTTP)
    }

    /**断开http设备的连接*/
    fun disconnectHttpDevice(info: TcpConnectInfo?) {
        val tcpDevice = httpDeviceConnectData.value
        if (tcpDevice != null) {
            tcpDevice.connectState = Tcp.CONNECT_STATE_DISCONNECT

            val find = tcpScanDeviceList.find { it.deviceName == tcpDevice.deviceName }
            if (find != null) {
                find.connectState = Tcp.CONNECT_STATE_DISCONNECT
            }

            tcpConnectDeviceListData.value!!.remove(tcpDevice)
            tcpConnectDeviceListData.updateThis()

            httpDeviceConnectData.updateValue(null)
            tcpStateData.updateValue(TcpState(tcpDevice, Tcp.CONNECT_STATE_DISCONNECT, info))
        }
    }

    var _lifecycleOwner: LifecycleOwner? = null
    val _lifecycleObserver = LifecycleEventObserver { source, event ->
        if (event == Lifecycle.Event.ON_DESTROY) {
            //销毁之后, 自动移除
            stopDiscovery()
        }
    }

    /**[NsdModel]*/
    val nsdManager: NsdManager by lazy {
        app().getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    // Instantiate a new DiscoveryListener
    private val discoveryListener = object : NsdManager.DiscoveryListener {

        // Called as soon as service discovery begins.
        override fun onDiscoveryStarted(regType: String) {
            L.d("开始发现服务:${regType}")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            L.d("发现服务:$service")
            runOnMainThread {
                val device =
                    tcpConnectDeviceListData.value?.find { it.deviceName == service.serviceName }
                        ?: TcpDevice(service.serviceName, service.port, service.serviceName)
                device.address = device.host
                if (device.deviceType == LaserPeckerHelper.DEVICE_TYPE_WIFI) {
                    device.port = HawkEngraveKeys.wifiPort
                }

                if (device.deviceName == httpDeviceConnectData.value?.deviceName) {
                    device.connectState =
                        httpDeviceConnectData.value?.connectState ?: device.connectState
                }

                if (!tcpScanDeviceList.contains(device)) {
                    tcpScanDeviceList.add(device)
                    tcpDeviceOnceData.updateValue(device)
                }
            }
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            // When the network service is no longer available.
            // Internal bookkeeping code goes here.
            L.e("服务丢失:$service")
        }

        override fun onDiscoveryStopped(serviceType: String) {
            L.i("发现服务停止:$serviceType")
            stopDiscovery()
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            L.e("发现服务失败:$serviceType :$errorCode")
            stopDiscovery()
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            L.e("停止发现服务失败:${serviceType} :$errorCode")
            nsdManager.stopServiceDiscovery(this)
        }
    }

    /**更新探测状态*/
    private fun updateDiscoveryState(state: Int) {
        wifiDeviceScan._state = state
        scanStateOnceData.updateValue(state)
    }

    /**使用nsd服务,开始发现设备*/
    fun startDiscovery(lifecycleOwner: LifecycleOwner): Boolean {
        if (scanState == WifiDeviceScan.STATE_SCAN_START) {
            return false
        }
        _lifecycleOwner = lifecycleOwner
        lifecycleOwner.lifecycle.addObserver(_lifecycleObserver)
        tcpScanDeviceList.clear()
        try {
            nsdManager.discoverServices(
                HawkEngraveKeys.nsdServiceType,
                NsdManager.PROTOCOL_DNS_SD,
                discoveryListener
            )
            updateDiscoveryState(WifiDeviceScan.STATE_SCAN_START)

            //3秒后停止
            _delay(3 * 1000) {
                stopDiscovery()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return true
    }

    /**停止发现设备*/
    fun stopDiscovery() {
        if (scanState == WifiDeviceScan.STATE_SCAN_START) {
            nsdManager.stopServiceDiscovery(discoveryListener)
            updateDiscoveryState(WifiDeviceScan.STATE_SCAN_FINISH)
        }
        try {
            nsdManager.stopServiceDiscovery(discoveryListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _lifecycleOwner?.lifecycle?.removeObserver(_lifecycleObserver)
        _lifecycleOwner = null
    }

    //endregion ---nsd---

}