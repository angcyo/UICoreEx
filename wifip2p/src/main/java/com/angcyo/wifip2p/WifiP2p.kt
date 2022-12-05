package com.angcyo.wifip2p

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.net.wifi.p2p.*
import android.net.wifi.p2p.WifiP2pManager.*
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo
import android.os.Build
import android.os.Debug
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.angcyo.core.vmApp
import com.angcyo.library.L
import com.angcyo.library.app
import com.angcyo.library.component.MainExecutor
import com.angcyo.library.ex.isDebuggerConnected
import com.angcyo.library.ex.nowTime
import com.angcyo.library.ex.nowTimeString
import com.angcyo.wifip2p.data.ConnectStateWrap
import com.angcyo.wifip2p.data.ConnectStateWrap.Companion.STATE_CONNECT_FAILURE
import com.angcyo.wifip2p.data.ServiceData
import com.angcyo.wifip2p.data.WifiP2pDeviceWrap
import com.angcyo.wifip2p.task.WifiP2pReceiveRunnable
import java.lang.reflect.Method
import android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION as WIFI_P2P_THIS_DEVICE_CHANGED_ACTION1

/**
 * WLAN 直连（对等连接或 P2P）概览
 *
 * https://developer.android.com/guide/topics/connectivity/wifip2p?hl=zh-cn
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/08/08
 */
class WifiP2p {

    companion object {

        /**服务端口key*/
        const val KEY_SERVER_PORT = "SERVER_PORT"

        /**实例的服务名称*/
        const val KEY_INSTANCE_NAME = "INSTANCE_NAME"

        /**实例的服务类型*/
        const val KEY_SERVICE_TYPE = "SERVICE_TYPE"

        /**服务开始的时间*/
        const val KEY_SERVICE_TIME = "SERVICE_TIME"

        /**开始一个服务, 用于被发现*/
        fun startWifiP2pService(serviceData: ServiceData, context: Context = app()) {
            context.startService(Intent(context, WifiP2pService::class.java).apply {
                putExtra(WifiP2pService.KEY_TYPE, WifiP2pService.TYPE_START)
                putExtra(WifiP2pService.KEY_DATA, serviceData)
            })
        }

        /**停止服务*/
        fun stopWifiP2pService(context: Context = app()) {
            context.startService(Intent(context, WifiP2pService::class.java).apply {
                putExtra(WifiP2pService.KEY_TYPE, WifiP2pService.TYPE_STOP)
            })
        }

        /**发现服务*/
        fun discoverWifiP2pService(context: Context = app()) {
            context.startService(Intent(context, WifiP2pService::class.java).apply {
                putExtra(WifiP2pService.KEY_TYPE, WifiP2pService.TYPE_DISCOVER)
            })
        }

        /**连接设备服务*/
        fun connectWifiP2pService(deviceWrap: WifiP2pDeviceWrap, context: Context = app()) {
            context.startService(Intent(context, WifiP2pService::class.java).apply {
                putExtra(WifiP2pService.KEY_TYPE, WifiP2pService.TYPE_CONNECT)
                putExtra(WifiP2pService.KEY_DATA, deviceWrap)
            })
        }

        /**激活wifi*/
        fun enableWiFi(enable: Boolean = true, context: Context = app()) {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiManager.isWifiEnabled = enable
        }

        /**wifi是否激活*/
        fun isWiFiEnabled(context: Context): Boolean {
            if (hotspotIsEnabled(context)) {
                return false
            }
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            return wifiManager.isWifiEnabled
        }

        /**热点是否激活*/
        fun hotspotIsEnabled(context: Context): Boolean {
            try {
                val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val method: Method = wifiManager.javaClass.getDeclaredMethod("isWifiApEnabled")
                method.isAccessible = true
                return method.invoke(wifiManager, null) as Boolean
            } catch (ex: Exception) {
                ex.printStackTrace()
                L.d("Failed to check tethering state, or it is not enabled.")
            }
            return false
        }

        private fun deleteGroup(
            manager: WifiP2pManager,
            channel: Channel,
            wifiP2pGroup: WifiP2pGroup
        ) {
            try {
                val getNetworkId = WifiP2pGroup::class.java.getMethod("getNetworkId")
                val networkId = getNetworkId.invoke(wifiP2pGroup) as Int
                val deletePersistentGroup = WifiP2pManager::class.java.getMethod(
                    "deletePersistentGroup",
                    Channel::class.java,
                    Int::class.java,
                    WifiP2pManager.ActionListener::class.java
                )
                deletePersistentGroup.invoke(manager, channel, networkId, null)
            } catch (ex: java.lang.Exception) {
                L.v("Failed to delete persistent group.")
            }
        }
    }

    var wifiP2pModel = vmApp<WifiP2pModel>()

    var manager: WifiP2pManager? = null

    /**通道*/
    var _channel: Channel? = null

    /**服务信息*/
    var _serviceInfo: WifiP2pServiceInfo? = null

    /**广播接收*/
    var _receiver: ServiceReceiver? = null

    var _receiverIntentFilter: IntentFilter? = null

    var _context: Context? = null

    /**1.初始化*/
    fun initialize(context: Context) {
        _context = context
        //
        manager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        _initialize()

        //
        _receiverIntentFilter = IntentFilter().apply {
            //WiFi P2P是否可用
            addAction(WIFI_P2P_STATE_CHANGED_ACTION)
            // peers列表发生变化
            addAction(WIFI_P2P_PEERS_CHANGED_ACTION)
            // WiFi P2P连接发生变化
            addAction(WIFI_P2P_CONNECTION_CHANGED_ACTION)
            // WiFi P2P设备信息发生变化
            addAction(WIFI_P2P_THIS_DEVICE_CHANGED_ACTION1)
        }
    }

    //region ---Channel---

    var channelListener: ChannelListener = ChannelListener {
        _initialize()
    }

    /**初始化一个通道*/
    fun _initialize() {
        _channel = manager?.initialize(_context!!, Looper.getMainLooper(), channelListener)
    }

    //endregion ---Channel---

    //region ---service---

    /**本地服务信息
     * 可以在[DnsSdTxtRecordListener]中过滤*/
    var serviceData: ServiceData? = null

    var localServiceAction: ActionListener = object : ActionListener {
        override fun onSuccess() {
            L.i("Successfully added the local service.")
        }

        override fun onFailure(error: Int) {
            L.w("Failed to create ${serviceData?.instanceName}: Error Code: $error")
        }
    }

    var clearLocalServiceAction: ActionListener =
        object : ActionListener {
            override fun onSuccess() {
                L.i("Successfully clear the local service.")
            }

            override fun onFailure(error: Int) {
                L.w("Failed to clear ${serviceData?.instanceName}: Error Code: $error")
            }
        }

    /**接收数据的服务*/
    var wifiP2pReceiveRunnable: WifiP2pReceiveRunnable? = null

    /**2.开启本地服务, 用于被发现以及过滤
     * [serviceData]
     * [Manifest.permission.ACCESS_FINE_LOCATION]
     * */
    fun startService() {
        if (ActivityCompat.checkSelfPermission(
                _context!!,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            L.w("无权限访问!")
            return
        }

        _channel?.let {
            _registerReceiver()

            //Create a service info object will android will actually hand out to the clients.
            val txtRecord = hashMapOf<String, String>()
            serviceData?.txtRecord?.let {
                txtRecord.putAll(it)
            }
            if (wifiP2pReceiveRunnable == null) {
                wifiP2pReceiveRunnable = WifiP2pReceiveRunnable()
                wifiP2pReceiveRunnable?.start()
            }
            txtRecord[KEY_SERVER_PORT] = "${wifiP2pReceiveRunnable?.port}"//端口
            val instanceName = serviceData?.instanceName ?: "${nowTimeString()}-p2p"
            val serviceType = (serviceData?.serviceType ?: "WifiP2pService") + WifiP2pService.TYPE
            txtRecord[KEY_INSTANCE_NAME] = instanceName
            txtRecord[KEY_SERVICE_TYPE] = serviceType
            txtRecord[KEY_SERVICE_TIME] = "${nowTime()}"
            _serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(instanceName, serviceType, txtRecord)
            manager?.addLocalService(it, _serviceInfo, localServiceAction)

            //发现服务
            _discoverService()
        }
    }

    /**停止服务*/
    fun stopService() {
        cancelConnect()
        wifiP2pReceiveRunnable?.cancel()
        wifiP2pReceiveRunnable = null
        _channel?.let {
            manager?.clearLocalServices(it, clearLocalServiceAction)
        }
        _receiver?.let {
            _context?.unregisterReceiver(it)
            _receiver = null
        }
        _removeServiceRequest()
        //
        wifiP2pModel.isDiscoverData.postValue(false)
    }

    /**注册广播*/
    fun _registerReceiver() {
        if (_receiver == null) {
            _receiver = ServiceReceiver().apply {
                _context?.registerReceiver(this, _receiverIntentFilter)
            }
        }
    }

    //endregion ---service---

    //region ---discover---

    var dnsSdServiceResponseListener: DnsSdServiceResponseListener? = null

    /**发现的设备在此回调
     * test--2022-08-09 14:08:07.851.demo.tcp.local. {}
     *
     * wifip2pdemo!2022-08-09 15:42:16.583!.type-test.p2p.local.
     * {key1=value1, key2=value2}
     * */
    var dnsSdTxtRecordListener = DnsSdTxtRecordListener { serviceFullDomainName, record, device ->
        L.d("$serviceFullDomainName $record $device")
        wifiP2pModel.onDnsSdTxtRecordAvailable(serviceFullDomainName, record, device)
    }

    /**2.发现其他服务
     * [Manifest.permission.ACCESS_FINE_LOCATION]
     * */
    fun discoverService() {
        _registerReceiver()
        _registerResponseListeners()
        _discoverService()
    }

    // Found WifiP2pDemo!2022-08-09 15:42:16.583!
    // type-test.p2p.local.
    // Device: Redmi K30
    fun _registerResponseListeners() {
        //注册服务发现时的相应回调
        if (dnsSdServiceResponseListener == null) {
            dnsSdServiceResponseListener =
                DnsSdServiceResponseListener { instanceName, serviceNameAndTP, sourceDevice ->
                    //Found test--2022-08-09 14:08:07.851 demo.tcp.local.
                    L.d("Found $instanceName $serviceNameAndTP $sourceDevice")
                    wifiP2pModel.onDnsSdServiceAvailable(
                        instanceName,
                        serviceNameAndTP,
                        sourceDevice
                    )
                }
            manager?.setDnsSdResponseListeners(
                _channel,
                dnsSdServiceResponseListener,
                dnsSdTxtRecordListener
            )
        }
    }

    var _serviceRequest: WifiP2pDnsSdServiceRequest? = null

    var _serviceRequestAction: ActionListener = object : ActionListener {
        override fun onSuccess() {
            L.v("Service discovery request acknowledged.")
        }

        override fun onFailure(error: Int) {
            L.e("Failed adding service discovery request: $error")
        }
    }

    var _discoverServicesAction: ActionListener = object : ActionListener {
        override fun onSuccess() {
            L.d("Service discovery Services initiated.")
        }

        override fun onFailure(error: Int) {
            L.e("Service discovery Services has failed. Reason Code: $error")
            if (error == P2P_UNSUPPORTED) {
                //p2p不支持
            }
            if (error == NO_SERVICE_REQUESTS) {
                //无服务请求
                enableWiFi(false)
                enableWiFi(true)
            }
        }
    }

    var _discoverPeersAction: ActionListener = object : ActionListener {
        override fun onSuccess() {
            L.d("Service discovery Peers initiated.")
        }

        override fun onFailure(error: Int) {
            L.e("Service discovery Peers has failed. Reason Code: $error")
            if (error == P2P_UNSUPPORTED) {
                //p2p不支持
            }
            if (error == NO_SERVICE_REQUESTS) {
                //无服务请求
                enableWiFi(false)
                enableWiFi(true)
            }
        }
    }

    //发现服务
    fun _discoverService() {
        if (ActivityCompat.checkSelfPermission(
                _context!!,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            L.w("无权限访问!")
            return
        }
        manager?.let {
            _serviceRequest = WifiP2pDnsSdServiceRequest.newInstance()
            it.addServiceRequest(_channel, _serviceRequest, _serviceRequestAction)

            //Manifest.permission.ACCESS_FINE_LOCATION
            //it.discoverPeers(_channel, _discoverPeersAction) //api 14

            //Manifest.permission.ACCESS_FINE_LOCATION
            it.discoverServices(_channel, _discoverServicesAction) //api 16

            //扫描
            wifiP2pModel.isDiscoverData.postValue(true)
        }
    }

    var _removeServiceRequestAction: ActionListener = object : ActionListener {
        override fun onSuccess() {
            L.v("Service remove request acknowledged.")
        }

        override fun onFailure(error: Int) {
            L.e("Failed remove service discovery request: $error")
        }
    }

    //移除服务请求
    fun _removeServiceRequest() {
        _serviceRequest?.let {
            manager?.removeServiceRequest(_channel, it, _removeServiceRequestAction)
        }
    }

    //endregion ---discover---

    //region ---Group---

    /**创建组*/
    @SuppressLint("MissingPermission")
    fun createGroup() {
        manager?.createGroup(_channel, object : ActionListener {
            override fun onSuccess() {
                L.v("Successfully created group.")
                //L.d("Successfully created " + thisDevice.serviceName.toString() + " service running on port " + thisDevice.servicePort)
            }

            override fun onFailure(reason: Int) {
                L.e("Failed to create group. Reason :$reason")
            }
        })
    }

    /**断开连接*/
    @SuppressLint("MissingPermission")
    fun disconnectFromDevice() {
        manager?.requestGroupInfo(_channel) { group ->
            if (group != null) {
                manager?.removeGroup(_channel, object : ActionListener {
                    override fun onSuccess() {
                        deleteGroup(manager!!, _channel!!, group)
                        L.d("Removed WiFi Direct Group.")
                    }

                    override fun onFailure(reason: Int) {
                        L.e("Failed to remove a WiFi Direct Group. Reason: $reason")
                    }
                })
            }
        }
    }

    //endregion ---Group---

    //region ---Connect---

    /**连接超时设置*/
    var connectTimeout: Long = 10_000

    val timeoutRunnable = Runnable {
        cancelConnect()
    }

    /**3.连接设备*/
    @SuppressLint("MissingPermission")
    fun connectDevice(deviceWrap: WifiP2pDeviceWrap, action: WifiP2pConfig.() -> Unit = {}) {
        val connectState = wifiP2pModel.connectState(deviceWrap)
        if (connectState.state == ConnectStateWrap.STATE_CONNECT_START) {
            L.w("正在连接...")
            return
        } else if (connectState.state == ConnectStateWrap.STATE_CONNECT_SUCCESS) {
            L.w("已连接...")
            return
        }
        cancelConnect()//cancel
        val config = WifiP2pConfig()
        config.deviceAddress = deviceWrap.sourceDevice.deviceAddress
        //config.wps.setup = WpsInfo.PBC
        //config.wps.pin
        config.action()
        manager?.connect(_channel, config, object : ActionListener {
            override fun onSuccess() {
                //连接发送成功, 但不一定连接成功. [connectionInfoListener]
                L.d("Attempting to connect to another device.")
                //wifiP2pModel.updateConnectState(deviceWrap, ConnectStateWrap.STATE_CONNECT_SUCCESS)
            }

            override fun onFailure(reason: Int) {
                L.e("Failed to connect to device:${config.deviceAddress}->$reason")
                wifiP2pModel.updateConnectState(deviceWrap, ConnectStateWrap.STATE_CONNECT_FAILURE)
                MainExecutor.handler.removeCallbacks(timeoutRunnable)
            }
        })
        wifiP2pModel.updateConnectState(deviceWrap, ConnectStateWrap.STATE_CONNECT_START)
        MainExecutor.handler.postDelayed(timeoutRunnable, connectTimeout)
    }

    /**取消正在进行的连接, 并且断开已经存在的连接*/
    fun cancelConnect() {
        MainExecutor.handler.removeCallbacks(timeoutRunnable)
        manager?.cancelConnect(_channel, object : ActionListener {
            override fun onSuccess() {
                L.v("Attempting to cancel connect.")
            }

            override fun onFailure(reason: Int) {
                L.v("Failed to cancel connect, the device may not have been trying to connect: $reason")
            }
        })
        //断开连接状态
        wifiP2pModel.disconnectState()
    }

    //endregion ---Connect---

    //region ---Receiver---

    /**连接成功回调的信息*/
    var connectionInfoListener: ConnectionInfoListener = ConnectionInfoListener {
        //groupFormed: true isGroupOwner: false groupOwnerAddress: /192.168.49.1
        it.groupFormed
        it.groupOwnerAddress.hostAddress
        L.i(it)
        wifiP2pModel.connectWifiP2pInfoData.postValue(it)
    }

    /**配对列表数据回调
     *  Device: Redmi K30
     *   deviceAddress: e6:d4:4d:91:ec:c7
     *   primary type: 10-0050F204-5
     *   secondary type: null
     *   wps: 392
     *   grpcapab: 0
     *   devcapab: 37
     *   status: 3
     *   wfdInfo: WFD enabled: trueWFD DeviceInfo: 16
     *   WFD CtrlPort: 7236
     *   WFD MaxThroughput: 50
     * */
    var peerListListener: PeerListListener = PeerListListener {
        L.i(it)
        wifiP2pModel.peerDeviceListData.postValue(it.deviceList.toList())
    }

    /**
     * [android.net.wifi.p2p.STATE_CHANGED]
     * [android.net.wifi.p2p.CONNECTION_STATE_CHANGE]
     * [android.net.wifi.p2p.THIS_DEVICE_CHANGED]
     * */
    inner class ServiceReceiver : BroadcastReceiver() {

        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            L.i("收到服务广播:$intent")
            when (intent.action) {
                //当设备的 WLAN 连接状态更改时广播。
                WIFI_P2P_STATE_CHANGED_ACTION -> {
                    //WiFi P2P是否可用
                    val state = intent.getIntExtra(EXTRA_WIFI_STATE, -1)
                    wifiP2pModel.isEnableWifiP2pData.postValue(state == WIFI_P2P_STATE_ENABLED)
                    if (state == WIFI_P2P_STATE_ENABLED) {
                        //激活
                    } else {
                        L.w(" WiFi P2P is no longer enabled.")
                    }
                }
                //当 WLAN P2P 在设备上启用或停用时广播。
                WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    val deviceList =
                        intent.getParcelableExtra<WifiP2pDeviceList>(EXTRA_P2P_DEVICE_LIST)
                    // peers列表发生变化
                    manager?.requestPeers(_channel, peerListListener)
                }
                WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    // WiFi P2P连接发生变化

                    //
                    MainExecutor.handler.removeCallbacks(timeoutRunnable)

                    val networkInfo = intent.getParcelableExtra<NetworkInfo>(EXTRA_NETWORK_INFO)
                    val wifiP2pInfo = intent.getParcelableExtra<WifiP2pInfo>(EXTRA_WIFI_P2P_INFO)
                    val wifiP2pGroup = intent.getParcelableExtra<WifiP2pGroup>(EXTRA_WIFI_P2P_GROUP)
                    L.i("${networkInfo?.typeName}->$networkInfo\n\n$wifiP2pInfo\n\n$wifiP2pGroup")

                    wifiP2pInfo?.groupOwnerAddress?.hostAddress
                    wifiP2pGroup?.clientList?.size

                    manager?.let {
                        if (networkInfo?.isConnected == true && networkInfo.typeName == "WIFI_P2P") {
                            it.requestConnectionInfo(_channel, connectionInfoListener)//api14
                            wifiP2pModel.wifiP2pGroupData.postValue(wifiP2pGroup)
                            if (isDebuggerConnected()) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    it.requestNetworkInfo(_channel!!) {//api29
                                        L.i(it)
                                    }
                                }
                                it.requestGroupInfo(_channel!!) {//api14
                                    L.i(it)
                                }
                            }
                            wifiP2pModel.updateConnectWifiP2pInfo(wifiP2pInfo)
                        } else {
                            L.d("Not connected to another device.")
                            wifiP2pModel.wifiP2pGroupData.postValue(null)
                            wifiP2pModel.updateConnectWifiP2pInfo(null)
                            wifiP2pModel.updateConnectState(STATE_CONNECT_FAILURE)

                        }
                    }
                }
                //当设备的详细信息（例如设备名称）更改时广播。
                WIFI_P2P_THIS_DEVICE_CHANGED_ACTION1 -> {
                    // WiFi P2P当前设备信息发生变化
                    val device = intent.getParcelableExtra<WifiP2pDevice>(EXTRA_WIFI_P2P_DEVICE)
                    L.i(device)
                    if (isDebuggerConnected()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            manager?.requestDeviceInfo(_channel!!) {//api29
                                L.i(device)
                            }
                        }
                    }
                    wifiP2pModel.selfWifiP2pDeviceData.postValue(device)
                }
            }
        }
    }

    //endregion ---Receiver---

}