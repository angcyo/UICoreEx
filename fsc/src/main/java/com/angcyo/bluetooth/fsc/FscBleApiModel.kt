package com.angcyo.bluetooth.fsc

import android.Manifest
import android.app.Activity
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.angcyo.bluetooth.fsc.core.*
import com.angcyo.bluetooth.fsc.core.DeviceConnectState.Companion.CONNECT_STATE_DISCONNECT
import com.angcyo.bluetooth.fsc.core.DeviceConnectState.Companion.CONNECT_STATE_DISCONNECT_START
import com.angcyo.bluetooth.fsc.core.DeviceConnectState.Companion.CONNECT_STATE_NORMAL
import com.angcyo.bluetooth.fsc.core.DeviceConnectState.Companion.CONNECT_STATE_START
import com.angcyo.bluetooth.fsc.core.DeviceConnectState.Companion.CONNECT_STATE_SUCCESS
import com.angcyo.bluetooth.fsc.core.DevicePacketState.Companion.PACKET_STATE_FINISH
import com.angcyo.bluetooth.fsc.core.DevicePacketState.Companion.PACKET_STATE_PAUSE
import com.angcyo.bluetooth.fsc.core.DevicePacketState.Companion.PACKET_STATE_PROGRESS
import com.angcyo.bluetooth.fsc.core.DevicePacketState.Companion.PACKET_STATE_RECEIVED
import com.angcyo.bluetooth.fsc.core.DevicePacketState.Companion.PACKET_STATE_START
import com.angcyo.bluetooth.fsc.core.DevicePacketState.Companion.PACKET_STATE_STOP
import com.angcyo.bluetooth.fsc.laserpacker.bean.AtCommandBean
import com.angcyo.bluetooth.fsc.laserpacker.writeBleLog
import com.angcyo.http.rx.doMain
import com.angcyo.library.L
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.app
import com.angcyo.library.ex.*
import com.angcyo.viewmodel.*
import com.feasycom.ble.controler.FscBleCentralApi
import com.feasycom.ble.controler.FscBleCentralApiImp
import com.feasycom.ble.controler.FscBleCentralCallbacksImp
import com.feasycom.common.bean.ConnectType
import com.feasycom.common.bean.FscDevice
import com.feasycom.common.controler.FscApi
import com.feasycom.spp.controler.FscSppCentralApi
import com.feasycom.spp.controler.FscSppCentralApiImp
import com.feasycom.spp.controler.FscSppCentralCallbacksImp
import java.io.InputStream
import java.util.concurrent.CopyOnWriteArraySet

/**
 * 蓝牙模型
 * [com.angcyo.bluetooth.fsc.FscBleApiModel.Companion.init]
 * [com.angcyo.bluetooth.fsc.FscBleApiModel.Companion.bluetoothPermissionList]
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/19
 */
class FscBleApiModel : ViewModel(), IViewModel {

    companion object {

        /**蓝牙的状态*/
        const val BLUETOOTH_STATE_NORMAL = 0

        /**扫描中*/
        const val BLUETOOTH_STATE_SCANNING = 1

        /**蓝牙不可用*/
        const val BLUETOOTH_STATE_UNAVAILABLE = 2

        /**蓝牙扫描结束*/
        const val BLUETOOTH_STATE_STOP = 3

        const val REQUEST_CODE_PERMISSION_LOCATION = 0x9902
        const val REQUEST_CODE_ENABLE_BLUETOOTH = 0x9903

        /**不支持ble*/
        var NO_BLE = true

        /**默认情况下, 34748 bytes/s */
        val bleApi: FscBleCentralApi?
            get() = try {
                if (NO_BLE) {
                    null
                } else {
                    FscBleCentralApiImp.getInstance()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

        /**默认情况下, 36412 45544 70826 bytes/s */
        val sppApi: FscSppCentralApi
            get() = FscSppCentralApiImp.getInstance()

        /**初始化方法*/
        @CallPoint
        fun init(application: Context = app(), debug: Boolean = isDebug()) {
            try {
                if (!NO_BLE) {
                    FscBleCentralApiImp.getInstance(application).apply {
                        initialize()
                        isShowLog(debug)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                FscSppCentralApiImp.getInstance(application).apply {
                    initialize()
                    isShowLog(debug)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        /**手机是否有蓝牙设备*/
        fun isSupportBle() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
                && app().packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

        /**是否开启了蓝牙*/
        fun isBlueEnable() = BluetoothAdapter.getDefaultAdapter()?.isEnabled == true &&
                (bleApi?.isEnabled == true || FscSppCentralApiImp.getInstance().isEnabled)

        /**GPS是否已打开*/
        fun checkGPSIsOpen(context: Context = app()): Boolean {
            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
                    ?: return false
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }

        /**打开GPS的设置页面*/
        fun enableGpsSetting(context: Context = app()) {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            intent.baseConfig(context)
            context.startActivity(intent)
        }

        /**激活蓝牙, 或者发起激活蓝牙的请求*/
        fun enableBluetooth(context: Context = app()): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    //有权限, 自动激活蓝牙
                    if (BluetoothAdapter.getDefaultAdapter().enable()) {
                        return true
                    }
                }
            }

            //请求打开蓝牙
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            intent.baseConfig(context)
            if (context is Activity) {
                context.startActivityForResult(intent, REQUEST_CODE_ENABLE_BLUETOOTH)
            } else {
                context.startActivity(intent)
            }
            return false
        }

        /**蓝牙需要的权限列表
         * https://developer.android.google.cn/guide/topics/connectivity/bluetooth?hl=zh_cn*/
        fun bluetoothPermissionList(): List<String> {
            val result = mutableListOf<String>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                //位置权限是必须的
                result.add(Manifest.permission.ACCESS_COARSE_LOCATION)
                result.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                result.add(Manifest.permission.BLUETOOTH_SCAN)
                result.add(Manifest.permission.BLUETOOTH_ADVERTISE)
                result.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            return result
        }

        /**返回是否有蓝牙的相关权限*/
        fun haveBluetoothPermission(context: Context = app()): Boolean =
            context.havePermission(bluetoothPermissionList())
    }

    //<editor-fold desc="Callbacks Init">

    //扫描回调, 子线程回调
    val bleCallback = object : FscBleCentralCallbacksImp() {

        override fun startScan() {
            super.startScan()
            _startScan()
        }

        override fun stopScan() {
            super.stopScan()
            _stopScan()
        }

        // 扫描到设备
        override fun blePeripheralFound(device: FscDevice, rssi: Int, record: ByteArray?) {
            super.blePeripheralFound(device, rssi, record)
            _peripheralFound(device, rssi)
        }

        override fun servicesFound(
            gatt: BluetoothGatt,
            address: String,
            services: List<BluetoothGattService>
        ) {
            super.servicesFound(gatt, address, services)
        }

        // 连接成功
        override fun blePeripheralConnected(
            gatt: BluetoothGatt,
            address: String,
            type: ConnectType
        ) {
            super.blePeripheralConnected(gatt, address, type)
            _peripheralConnected(address, gatt, type)
        }

        // 断开连接
        @WorkerThread
        override fun blePeripheralDisconnected(gatt: BluetoothGatt, address: String, p2: Int) {
            super.blePeripheralDisconnected(gatt, address, p2)
            doMain(true) {
                _peripheralDisconnected(address, gatt)
            }
        }

        /**
         *  发送文件进度
         *  @param address 设备地址
         *  @param percentage 进度
         *  @param data 源数据
         */
        @WorkerThread
        override fun sendPacketProgress(address: String, percentage: Int, sendByte: ByteArray) {
            super.sendPacketProgress(address, percentage, sendByte)
            _sendPacketProgress(address, percentage, sendByte)
        }

        /**
         *  发送数据
         *  @param address 设备地址
         *  @param strValue 字符串
         *  @param data 源数据
         */
        @WorkerThread
        override fun packetSend(address: String, strValue: String, data: ByteArray) {
            super.packetSend(address, strValue, data)
            _packetSend(address, strValue, data)
        }

        /**
         *  收到数据
         *  @param address 设备地址
         *  @param strValue 字符串
         *  @param dataHexString 十六进制
         *  @param data 源数据
         */
        @WorkerThread
        override fun packetReceived(
            address: String,
            strValue: String,
            dataHexString: String,
            data: ByteArray
        ) {
            super.packetReceived(address, strValue, dataHexString, data)
            _packetReceived(address, strValue, dataHexString, data)
        }

        /**
         *  OTA 升级进度
         *  @param address 设备地址
         *  @param percentage 进度
         *  @param status 状态
         */
        override fun otaProgressUpdate(address: String, percentage: Int, status: Int) {
            super.otaProgressUpdate(address, percentage, status)
            _otaProgressUpdate(address, percentage, status)
        }

        /**
         *  AT 指令模式通讯回调
         *  @param command 发送的命令
         *  @param parameter 收到的回复
         *  @param type 类型
         *  @param status 状态
         */
        override fun atCommandCallBack(command: String?, param: String?, type: Int, status: Int) {
            super.atCommandCallBack(command, param, type, status)
            _atCommandCallBack(command, param, type, status)
        }

        /**
         *  AT 指令发送结束时触发
         */
        override fun endATCommand() {
            super.endATCommand()
            L.v("AT...")
        }

        /**
         *  开始发送AT指令时触发
         */
        override fun startATCommand() {
            super.startATCommand()
            L.v("AT...")
        }
    }

    //spp回调, 子线程回调
    val sppCallback = object : FscSppCentralCallbacksImp() {

        override fun startScan() {
            super.startScan()
            _startScan()
        }

        override fun stopScan() {
            super.stopScan()
            _stopScan()
        }

        override fun sppPeripheralFound(sppDevice: FscDevice, rssi: Int) {
            super.sppPeripheralFound(sppDevice, rssi)
            _peripheralFound(sppDevice, rssi)
        }

        // 连接成功
        override fun sppPeripheralConnected(device: BluetoothDevice, type: ConnectType) {
            super.sppPeripheralConnected(device, type)
            val cacheDeviceState = connectDeviceList.find { it.device.address == device.address }
            cacheDeviceState?.device?.device = device
            _peripheralConnected(device.address, null, type)
        }

        // 断开连接
        @WorkerThread
        override fun sppPeripheralDisconnected(address: String) {
            super.sppPeripheralDisconnected(address)
            doMain(true) {
                _peripheralDisconnected(address, null)
            }
        }

        /**
         *  发送文件进度
         *  @param address 设备地址
         *  @param percentage 进度
         *  @param data 源数据
         */
        @WorkerThread
        override fun sendPacketProgress(address: String, percentage: Int, sendByte: ByteArray) {
            super.sendPacketProgress(address, percentage, sendByte)
            _sendPacketProgress(address, percentage, sendByte)
        }

        /**
         *  发送数据
         *  @param address 设备地址
         *  @param strValue 字符串
         *  @param data 源数据
         */
        @WorkerThread
        override fun packetSend(address: String, strValue: String, data: ByteArray) {
            super.packetSend(address, strValue, data)
            _packetSend(address, strValue, data)
        }

        /**
         * 发送数据
         * @param address       设备地址
         * @param strValue      发送的数据
         * @param hexString     发送的十六进制数据
         * @param data          原数据
         */
        @WorkerThread
        override fun packetSend(
            address: String,
            strValue: String,
            packetReceived: String,
            data: ByteArray
        ) {
            super.packetSend(address, strValue, packetReceived, data)
            _packetSend(address, strValue, data)
        }

        /**
         *  收到数据
         *  @param address 设备地址
         *  @param strValue 字符串
         *  @param dataHexString 十六进制
         *  @param data 源数据
         */
        @WorkerThread
        override fun packetReceived(
            address: String,
            strValue: String,
            dataHexString: String,
            data: ByteArray
        ) {
            super.packetReceived(address, strValue, dataHexString, data)
            _packetReceived(address, strValue, dataHexString, data)
        }

        /**
         *  OTA 升级进度
         *  @param address 设备地址
         *  @param percentage 进度
         *  @param status 状态 120
         */
        @WorkerThread
        override fun otaProgressUpdate(address: String, percentage: Int, status: Int) {
            super.otaProgressUpdate(address, percentage, status)
            _otaProgressUpdate(address, percentage, status)
        }

        /**
         *  AT 指令模式通讯回调
         *  @param command 发送的命令
         *  @param param 收到的回复
         *  @param type 类型
         *  @param status 状态
         */
        override fun atCommandCallBack(command: String?, param: String?, type: Int, status: Int) {
            super.atCommandCallBack(command, param, type, status)
            //AT+VER :9.1.1,FSC-BT986 5 2
            //AT_VER :null 5 3
            _atCommandCallBack(command, param, type, status)
        }

        /**
         *  AT 指令发送结束时触发
         */
        @WorkerThread
        override fun endATCommand() {
            super.endATCommand()
            L.v("AT...endATCommand")
        }

        /** 进入AT指令发送模式
         *  [com.feasycom.common.controler.FscApi.connectToModify]
         */
        override fun startATCommand() {
            super.startATCommand()
            L.v("AT...startATCommand")
        }
    }

    //</editor-fold desc="Callbacks">

    /**蓝牙的状态
     * [BLUETOOTH_STATE_NORMAL]
     * [BLUETOOTH_STATE_SCANNING]
     * [BLUETOOTH_STATE_UNAVAILABLE]
     * [BLUETOOTH_STATE_STOP]
     * */
    val bleStateData: MutableLiveData<Int> = vmData(BLUETOOTH_STATE_NORMAL)

    /**扫描到的蓝牙设备, 不重复的设备*/
    val bleDeviceData: MutableOnceLiveData<FscDevice?> = vmDataOnce(null)

    /**扫到了设备变化, 会出现重复的数据*/
    val bleScanDeviceData: MutableOnceLiveData<FscDevice?> = vmDataOnce(null)

    /**扫描到的所有蓝牙设备, 在停止扫描之后才会有值*/
    val bleDeviceListData: MutableLiveData<List<FscDevice>> = vmData(emptyList())

    /**连接状态监听, 不存储数据*/
    val connectStateData: MutableOnceLiveData<DeviceConnectState?> = vmDataOnce(null)

    /**已经连接成功的设备缓存和动态监听*/
    val connectDeviceListData: MutableLiveData<List<DeviceConnectState>> = vmData(emptyList())

    /**设备连接状态列表,包括已连接的设备*/
    val connectDeviceList = mutableListOf<DeviceConnectState>()

    //请求断开连接的设备集合
    val pendingDisconnectList = mutableSetOf<String>()

    val handle = Handler(Looper.getMainLooper())

    /**扫描时长*/
    var scanTimeout = 5_000L

    /**是否使用spp模式扫描蓝牙设备*/
    var useSppScan = true
        set(value) {
            if (field != value) {
                stopScan()
                stopSend()
            }
            //last
            field = value
        }

    /**是否激活spp模式, 走spp api, 并且激活sdp服务
     * 开启sdp服务后, 设备可以通过spp主动连接手机. Service Discovery Protocol
     * [com.feasycom.spp.controler.FscSppCentralApiImp.openSdpService]*/
    var useSppModel = true
        set(value) {
            useSppScan = value
            field = value
        }

    /**api*/
    val fscApi: FscApi<*>
        get() = if (useSppModel) sppApi else bleApi!!

    /**延迟停止[Runnable]*/
    val _delayStopRunnable = Runnable {
        stopScan()
    }

    init {
        bleApi?.setCallbacks(bleCallback)
        sppApi.setCallbacks(sppCallback)
    }

    //<editor-fold desc="操作方法">

    /**开启sdp服务, Service Discovery Protocol*/
    fun openSdpService(open: Boolean = true) {
        val api = fscApi
        if (api is FscSppCentralApiImp) {
            if (open) {
                if (!api.isEnabledSDP) {
                    api.openSdpService()
                }
            } else {
                if (api.isEnabledSDP) {
                    api.closeSdpService()
                }
            }
        }
    }

    /**是否有设备连接成功*/
    fun haveDeviceConnected(): Boolean {
        return connectDeviceListData.value.isNullOrEmpty().not()
    }

    /**获取最后一个连接的设备*/
    fun lastDeviceState(): DeviceConnectState? = connectDeviceListData.value?.lastOrNull()

    /**获取最后一台设备的地址*/
    fun lastDeviceAddress(): String? =
        lastDeviceState()?.device?.address ?: fscApi.bondDevices.lastOrNull()?.device?.address

    /**蓝牙是否正在连接, 或者已经连接*/
    fun isConnectState(device: FscDevice?): Boolean {
        val address = device?.address
        if (address.isNullOrEmpty()) {
            return false
        }
        if (isConnected(device)) {
            return true
        }
        val state = connectState(device)
        return state == CONNECT_STATE_START || state == CONNECT_STATE_SUCCESS
    }

    /**设备是否已连接
     * [isConnected]*/
    fun isConnected(
        address: String?,
        name: String? = null,
        resetConnectState: Boolean = false
    ): Boolean {
        if (address.isNullOrEmpty()) {
            return false
        }
        return isConnected(generateFscDevice(address, name), resetConnectState)
    }

    /**设备是否已连接
     * [resetConnectState] 是否要恢复连接状态*/
    @MainThread
    fun isConnected(device: FscDevice?, resetConnectState: Boolean = false): Boolean {
        val address = device?.address
        if (address.isNullOrEmpty()) {
            return false
        }
        val cacheDevice = connectDeviceList.find { it.device == device }
        return if (cacheDevice?.state != CONNECT_STATE_DISCONNECT && fscApi.isConnected(address)) {
            if (resetConnectState && cacheDevice == null) {
                connectStateData.value = wrapStateDevice(device) {
                    this.state = CONNECT_STATE_SUCCESS
                    this.connectedTime = nowTime()
                }
                //通知连接的蓝牙设备改变
                _notifyConnectDeviceChanged()
            }
            true
        } else {
            /*if (cacheDevice != null) {
                connectStateData.value = wrapStateDevice(device) {
                    initDisconnected()
                }
                //通知连接的蓝牙设备改变
                _notifyConnectDeviceChanged()
            }*/
            false
        }
    }

    /**开始扫描蓝牙设备
     * [bleDeviceData] 监听设备发现
     * [bleDeviceListData] 监听所有的设备
     *
     * [delayStop] 多少毫秒后, 自动关闭扫描
     * [context] 使用 Activity 才会申请对应的权限*/
    fun startScan(context: Context = app(), delayStop: Long = scanTimeout) {
        _checkDisconnectTimeout()

        if (bleStateData.value == BLUETOOTH_STATE_SCANNING) {
            //已经在扫描
            return
        }

        val api = fscApi

        if (!isSupportBle()) {
            //不支持蓝牙的设备
            updateBleState(BLUETOOTH_STATE_UNAVAILABLE)
            return
        } else {
            if (!checkGPSIsOpen(context)) {
                enableGpsSetting(context)
            }
            if (!isBlueEnable()) {
                //未开启蓝牙
                updateBleState(BLUETOOTH_STATE_UNAVAILABLE)
                enableBluetooth(context)

                //等待5s之后, 继续操作. 用户操作到打开蓝牙需要一定的时间.
                handle.postDelayed({
                    if (isBlueEnable()) {
                        startScan(context, delayStop)
                    }
                }, 5000L)
                return
            }
        }

        //扫描
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkPermissions(context)) {
                api.startScan()
            }
        } else {
            api.startScan()
        }

        //自动停止扫描
        if (delayStop > 0) {
            handle.postDelayed(_delayStopRunnable, delayStop)
        }
    }

    /**停止扫描
     * [com.angcyo.bluetooth.fsc.FscBleApiModel._stopScan]*/
    fun stopScan() {
        handle.removeCallbacks(_delayStopRunnable)
        if (bleStateData.value == BLUETOOTH_STATE_SCANNING) {
            //已经在扫描
            updateBleState(BLUETOOTH_STATE_STOP) //可能的重复调用
            fscApi.stopScan()
        } else if (bleStateData.value == BLUETOOTH_STATE_STOP) {
            //恢复默认状态
            updateBleState(BLUETOOTH_STATE_NORMAL)
        }
    }

    /**更新蓝牙扫描的状态*/
    @AnyThread
    fun updateBleState(state: Int) {
        if (bleStateData.value != state) {
            bleStateData.postValue(state)
        }
    }

    /**检查权限,
     * [context] 是 Activity 时, 才会请求权限
     * @return true 权限通过*/
    fun checkPermissions(context: Context): Boolean {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (!bluetoothAdapter.isEnabled) {
            return false
        }
        val permissions = bluetoothPermissionList()
        val permissionDeniedList: MutableList<String> = ArrayList()
        for (permission in permissions) {
            val permissionCheck = ContextCompat.checkSelfPermission(context, permission)
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                //权限给予
                //onPermissionGranted(permission)
            } else {
                permissionDeniedList.add(permission)
            }
        }
        if (permissionDeniedList.isNotEmpty()) {
            val deniedPermissions = permissionDeniedList.toTypedArray()
            if (context is Activity) {
                ActivityCompat.requestPermissions(
                    context,
                    deniedPermissions,
                    REQUEST_CODE_PERMISSION_LOCATION
                )
            }
        }
        return permissionDeniedList.isEmpty()
    }

    /**包裹[DeviceConnectState]*/
    fun wrapStateDevice(
        device: FscDevice,
        action: DeviceConnectState.() -> Unit
    ): DeviceConnectState {
        val state = connectDeviceList.find { it.device == device } ?: DeviceConnectState(device)
        state.action()
        connectDeviceList.remove(state)
        connectDeviceList.add(state)
        return state
    }

    /**[connectState]*/
    fun connectState(address: String?, name: String? = null): Int {
        if (address.isNullOrEmpty()) {
            return CONNECT_STATE_NORMAL
        }
        val fscDevice = generateFscDevice(address, name)
        return connectState(fscDevice)
    }

    /**当前设备的连接状态*/
    fun connectState(bleDevice: FscDevice?): Int {
        val find = connectDeviceList.find { it.device == bleDevice }
        if (find == null) {
            if (isConnected(bleDevice)) {
                return CONNECT_STATE_SUCCESS
            }
            return CONNECT_STATE_NORMAL
        }
        if (find.state == CONNECT_STATE_SUCCESS) {
            if (!isConnected(bleDevice)) {
                return CONNECT_STATE_NORMAL
            }
        }
        return find.state
    }

    /**通过地址直接连接蓝牙设备*/
    fun connect(
        address: String?,
        name: String?,
        isAutoConnect: Boolean = false,
        disconnectOther: Boolean = true,
        stopScan: Boolean = true
    ) {
        if (address.isNullOrEmpty()) {
            return
        }
        val fscDevice = generateFscDevice(address, name)
        connect(fscDevice, isAutoConnect, disconnectOther, stopScan)
    }

    /**连接设备
     * [connectStateData] 监听设备的连接状态
     * [disconnectOther] 是否断开其他设备
     * [stopScan] 是否停止扫描
     * [isAutoConnect] 是否是自动连接
     * */
    @MainThread
    fun connect(
        device: FscDevice?,
        isAutoConnect: Boolean = false,
        disconnectOther: Boolean = false,
        stopScan: Boolean = true,
        connectToModify: Boolean = false,
    ) {
        _checkDisconnectTimeout()
        if (stopScan) {
            stopScan()
        }
        if (device == null) {
            return
        }
        wrapStateDevice(device) {
            this.isAutoConnect = isAutoConnect
        }
        if (isConnected(device, true)) {
            return
        }
        if (isConnectState(device)) {
            return
        }
        pendingDisconnectList.remove(device.address)
        if (disconnectOther) {
            //断开其他设备
            disconnectAll()
        }
        connectDeviceList.removeAll { it.device == device }//移除缓存
        connectStateData.value = wrapStateDevice(device) {
            state = CONNECT_STATE_START
            connectTime = nowTime()
            connectedTime = 0L
            disconnectTime = 0L
            this.isAutoConnect = isAutoConnect
            isActiveDisConnected = false
            exception = null
            isNormalConnect = !connectToModify
        }
        if (connectToModify) {
            fscApi.connectToModify(device.address)
        } else {
            fscApi.connect(device.address)
        }
    }

    /**蓝牙模块空中升级
     * https://document.feasycom.com/web/#/16/451
     * [status] status=10086升级成功 status=120升级失败 121升级中
     * [dfuFile] 固件数据
     * [reset] 是否重置设置
     * */
    fun connectToOTAWithFactory(
        address: String,
        dfuFile: ByteArray,
        reset: Boolean = true,
        callback: (percentage: Int, status: Int) -> Unit
    ): IPacketListener {
        val listener = object : IPacketListener {
            override fun onOtaProgressUpdate(address: String, percentage: Int, status: Int) {
                super.onOtaProgressUpdate(address, percentage, status)
                if (status == 10086 || status == 120) {
                    removePacketListener(this)
                }
                callback(percentage, status)
            }

            override fun onPacketReceived(
                address: String,
                strValue: String,
                dataHexString: String,
                data: ByteArray
            ) {
                super.onPacketReceived(address, strValue, dataHexString, data)
                if (strValue.contains("OK") && strValue.length >= 15) {
                    callback(0, 121)
                } else {
                    removePacketListener(this)
                    callback(-1, 120)
                }
            }
        }
        addPacketListener(listener)
        fscApi.connectToOTAWithFactory(address, dfuFile, reset)
        return listener
    }

    /**断开所有连接的设备
     * [isActiveDisConnected] 是否是主动断开的蓝牙连接*/
    @AnyThread
    fun disconnectAll(isActiveDisConnected: Boolean = false) {
        if (connectDeviceList.isNotEmpty()) {
            val list = connectDeviceList.toList()
            list.forEach {
                if (it.state == CONNECT_STATE_START || it.state == CONNECT_STATE_SUCCESS) {
                    doMain {
                        disconnect(it.device, isActiveDisConnected)
                    }
                }
            }
        }
    }

    /**断开连接
     * [isActiveDisConnected] 是否是主动断开的蓝牙连接*/
    @MainThread
    fun disconnect(bleDevice: FscDevice?, isActiveDisConnected: Boolean = true) {
        if (bleDevice == null) {
            return
        }
        val address = bleDevice.address
        pendingDisconnectList.add(address)
        _checkDisconnectTimeout()
        if (isConnectState(bleDevice)) {
            connectStateData.value = wrapStateDevice(bleDevice) {
                initDisconnectStart()
                this.isActiveDisConnected = isActiveDisConnected
            }
            stopSend(address)
            fscApi.disconnect(address)
            "断开蓝牙[主动${isActiveDisConnected.toDC()}]:$address".writeBleLog()
        } else {
            val cacheDeviceState = connectDeviceList.find { it.device.address == address }
            cacheDeviceState?.let {
                if (it.state == CONNECT_STATE_DISCONNECT_START) {
                    //强制断开连接
                    connectStateData.value = wrapStateDevice(bleDevice) {
                        initDisconnected()
                        this.isActiveDisConnected = isActiveDisConnected
                    }
                }
                //connectDeviceList.remove(deviceState)//断开成功不移除状态
                _notifyConnectDeviceChanged()
            }
        }
    }

    /**开始断开蓝牙*/
    fun DeviceConnectState.initDisconnectStart() {
        state = CONNECT_STATE_DISCONNECT_START
        gatt = null
        disconnectTime = nowTime()
        isAutoConnect = false
    }

    /**成功断开蓝牙*/
    fun DeviceConnectState.initDisconnected() {
        state = CONNECT_STATE_DISCONNECT
        gatt = null
        disconnectedTime = nowTime()
        isAutoConnect = false
    }

    /**断开连接, 有时候会收不到通知.
     * 这里检查一下超时*/
    @MainThread
    fun _checkDisconnectTimeout(isActiveDisConnected: Boolean = true) {
        val list = connectDeviceList.toList()
        list.forEach { deviceState ->
            if (deviceState.state == CONNECT_STATE_DISCONNECT_START && nowTime() - deviceState.disconnectTime > 1_000) {
                //断开超时
                //connectDeviceList.remove(deviceState)//断开成功不移除状态
                pendingDisconnectList.remove(deviceState.device.address)
                connectStateData.value = wrapStateDevice(deviceState.device) {
                    initDisconnected()
                    this.isActiveDisConnected = isActiveDisConnected
                }
                _notifyConnectDeviceChanged()
            }
        }
    }

    /**通过[address] [name]构建一个[FscDevice]对象*/
    fun generateFscDevice(address: String, name: String? = null): FscDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            try {
                val bluetoothManager =
                    app().getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val device = (bluetoothManager?.adapter
                    ?: BluetoothAdapter.getDefaultAdapter())?.getRemoteDevice(address)
                FscDevice(name, address, device, 0, if (useSppModel) "SPP" else "BLE")
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }

    @Synchronized
    override fun release() {
        fscApi.stopScan()
        fscApi.stopSend()
        fscApi.disconnect()
        connectDeviceList.clear()
        devicePacketProgressCacheList.clear()
        bleDeviceListData.updateValue(emptyList())
    }

    //</editor-fold desc="操作方法">

    //<editor-fold desc="Callback Action">

    val cacheScanDeviceList = mutableListOf<FscDevice>()

    fun _startScan() {
        cacheScanDeviceList.clear()
        updateBleState(BLUETOOTH_STATE_SCANNING)
    }

    fun _stopScan() {
        handle.removeCallbacks(_delayStopRunnable)
        updateBleState(BLUETOOTH_STATE_STOP)
        bleDeviceListData.updateValue(cacheScanDeviceList)
        //重置
        updateBleState(BLUETOOTH_STATE_NORMAL)
    }

    @UiThread
    fun _peripheralFound(device: FscDevice, rssi: Int?) {
        rssi?.let {
            device.rssi = rssi
            connectDeviceListData.value?.forEach { connectState ->
                if (connectState.device == device) {
                    connectState.device.rssi = rssi
                }
            }
        }
        bleScanDeviceData.updateValue(device)
        if (!cacheScanDeviceList.contains(device)) {
            bleDeviceData.updateValue(device)
            cacheScanDeviceList.add(device)
        }
    }

    @UiThread
    fun _peripheralConnected(address: String, gatt: BluetoothGatt?, type: ConnectType) {
        val cacheDeviceState = connectDeviceList.find { it.device.address == address }
        cacheDeviceState?.let { deviceState ->
            val connectState = wrapStateDevice(deviceState.device) {
                this.state = CONNECT_STATE_SUCCESS
                this.gatt = gatt
                this.type = type
                this.connectedTime = nowTime()
            }
            //1:通知连接的蓝牙设备改变
            _notifyConnectDeviceChanged()
            //2:最后通知设备状态
            connectStateData.updateValue(connectState)
        }
    }

    @UiThread
    fun _peripheralDisconnected(address: String, gatt: BluetoothGatt?) {
        //fscApi.clearDevice(address)//?
        val cacheDeviceState = connectDeviceList.find { it.device.address == address }
        cacheDeviceState?.let { deviceState ->
            val wrapStateDevice = wrapStateDevice(deviceState.device) {
                initDisconnected()
                this.gatt = gatt
            }
            pendingDisconnectList.remove(deviceState.device.address)
            //connectDeviceList.remove(deviceState)//断开成功不移除状态
            connectStateData.value = wrapStateDevice
            _notifyConnectDeviceChanged()
        }
    }

    /**通知连接的蓝牙设备改变*/
    fun _notifyConnectDeviceChanged() {
        connectDeviceList.filterTo(mutableListOf()) { deviceConnectState ->
            //过滤得到所有连接成功的设备
            deviceConnectState.state == CONNECT_STATE_SUCCESS
        }.apply {
            connectDeviceListData.updateValue(this)
        }
        _checkDisconnectTimeout()
    }

    //</editor-fold desc="Callback">

    //<editor-fold desc="send and received">

    fun stopSend() {
        fscApi.stopSend()
    }

    /**停止发送*/
    fun stopSend(address: String) {
        fscApi.stopSend(address)
        clearProgressCache(address)
        devicePacketStateData.postValue(
            DevicePacketState(
                address,
                byteArrayOf(),
                findProgressCache(address)?.percentage ?: -1,
                PACKET_STATE_STOP
            )
        )
    }

    /**暂停发送*/
    fun pauseSend(address: String): Boolean {
        devicePacketStateData.postValue(
            DevicePacketState(
                address,
                byteArrayOf(),
                findProgressCache(address)?.percentage ?: -1,
                PACKET_STATE_PAUSE
            )
        )
        wrapProgressDevice(address) {
            isPause = true
        }
        return fscApi.pauseSend(address)
    }

    /**继续发送*/
    fun continueSend(address: String): Boolean {
        devicePacketStateData.postValue(
            DevicePacketState(
                address,
                byteArrayOf(),
                findProgressCache(address)?.percentage ?: -1,
                PACKET_STATE_PROGRESS
            )
        )
        wrapProgressDevice(address) {
            isPause = false
        }
        return fscApi.continueSend(address)
    }

    fun send(address: String, data: String): Boolean {
        clearProgressCache(address)
        clearReceiveCache(address)
        if (data.isEmpty()) {
            L.w("$address 发送空数据!")
        }
        return fscApi.send(address, data)
    }

    fun send(address: String, packet: ByteArray): Boolean {
        clearProgressCache(address)
        clearReceiveCache(address)
        if (packet.isEmpty()) {
            L.w("$address 发送空数据!")
        }
        return fscApi.send(address, packet)
    }

    /**发送假数据, [size]数据大小*/
    fun sendFile(address: String, size: Int): Boolean {
        clearProgressCache(address)
        clearReceiveCache(address)
        if (size <= 0) {
            L.w("$address 发送空数据!")
        }
        return fscApi.sendFile(address, size)
    }

    fun sendFile(address: String, inputStream: InputStream): Boolean {
        clearProgressCache(address)
        clearReceiveCache(address)
        if (inputStream.available() <= 0) {
            L.w("$address 发送空数据!")
        }
        return fscApi.sendFile(address, inputStream)
    }

    fun sendFile(address: String, byteArray: ByteArray): Boolean {
        clearProgressCache(address)
        clearReceiveCache(address)
        if (byteArray.isEmpty()) {
            L.w("$address 发送空数据!")
        }
        return fscApi.sendFile(address, byteArray)
    }

    /**发送一个AT指令
     * [command] AT+VER*/
    fun sendAtCommand(command: String, callback: (bean: AtCommandBean) -> Unit) {
        val address = lastDeviceAddress() ?: return
        val listener = object : IPacketListener {
            override fun onAtCommandCallBack(bean: AtCommandBean) {
                super.onAtCommandCallBack(bean)
                removePacketListener(this)
                callback(bean)
            }
        }
        addPacketListener(listener)
        fscApi.sendATCommand(address, setOf(command))
    }

    /**数据发送状态,进度监听*/
    val devicePacketStateData: MutableOnceLiveData<DevicePacketState> = vmDataOnce(null)

    //发送数据缓存
    val devicePacketProgressCacheList = mutableListOf<DevicePacketProgress>()

    //接收数据缓存
    val devicePacketReceiveCacheList = mutableListOf<PacketReceive>()

    val packetListenerList = CopyOnWriteArraySet<IPacketListener>()

    fun addPacketListener(listener: IPacketListener) {
        packetListenerList.add(listener)
    }

    fun removePacketListener(listener: IPacketListener) {
        packetListenerList.remove(listener)
    }

    /**清除Packet监听对象*/
    fun clearPacketListener() {
        packetListenerList.clear()
    }

    @Synchronized
    fun clearProgressCache(address: String?) {
        devicePacketProgressCacheList.removeAll { it.address == address }
    }

    @Synchronized
    fun clearReceiveCache(address: String?) {
        devicePacketReceiveCacheList.removeAll { it.address == address }
    }

    /**进度缓存*/
    @Synchronized
    fun findProgressCache(address: String?): DevicePacketProgress? {
        return devicePacketProgressCacheList.find { it.address == address }
    }

    @Synchronized
    fun findReceiveCache(address: String?): PacketReceive? {
        return devicePacketReceiveCacheList.find { it.address == address }
    }

    /**包裹[DevicePacketProgress]*/
    @Synchronized
    fun wrapProgressDevice(
        address: String,
        action: DevicePacketProgress.() -> Unit
    ): DevicePacketProgress {
        val element = devicePacketProgressCacheList.find { it.address == address }
            ?: DevicePacketProgress(address)
        element.action()
        devicePacketProgressCacheList.remove(element)
        devicePacketProgressCacheList.add(element)
        return element
    }

    @Synchronized
    fun wrapReceiveDevice(address: String, action: PacketReceive.() -> Unit): PacketReceive {
        val element = devicePacketReceiveCacheList.find { it.address == address }
            ?: PacketReceive(address)
        element.action()
        devicePacketReceiveCacheList.remove(element)
        devicePacketReceiveCacheList.add(element)
        return element
    }

    @WorkerThread
    fun _packetSend(address: String, strValue: String, data: ByteArray) {
        if (data.size > 100) {
            L.d("$address 发送:数据大小${data.size}bytes")
        } else {
            L.d("$address 发送:\n${data.toHexString(true)} ${data.size}bytes")
        }
        val packetProgress = wrapProgressDevice(address) {
            sendBytesSize += data.size
            sendPacketCount++
            if (percentage == -1) {
                //记录开始发送的时间
                percentage = 0
                startTime = System.currentTimeMillis()
            }
        }
        devicePacketStateData.postValue(
            DevicePacketState(address, data, -1, PACKET_STATE_START)
        )
        packetListenerList.forEach {
            it.onPacketSend(packetProgress, address, strValue, data)
        }
    }

    @WorkerThread
    fun _sendPacketProgress(address: String, percentage: Int, sendByte: ByteArray) {
        L.d("$address 发送进度:$percentage% ${sendByte.size}bytes")
        val packetProgress = wrapProgressDevice(address) {
            this.percentage = percentage
            if (percentage == 100) {
                //记录完成发送的时间
                finishTime = System.currentTimeMillis()
            }
        }
        devicePacketStateData.postValue(
            DevicePacketState(
                address,
                sendByte,
                percentage,
                if (percentage >= 100) PACKET_STATE_FINISH else PACKET_STATE_PROGRESS
            )
        )
        packetListenerList.forEach {
            it.onSendPacketProgress(packetProgress, address, percentage, sendByte)
        }
    }

    /**
     * [address] DC:0D:30:10:19:5E
     * [dataHexString] [AA BB 13 00 06 00 00 00 00 00 00 00 00 00 00 00 06 00 01 00 00 0D ]*/
    @WorkerThread
    @Synchronized
    fun _packetReceived(address: String, strValue: String, dataHexString: String, data: ByteArray) {
        L.d("$address 收到:\n${strValue} $dataHexString ${data.size}bytes")
        wrapReceiveDevice(address) {
            if (startTime == -1L) {
                startTime = System.currentTimeMillis()
            }
            receiveBytesSize += data.size
            receivePacketCount++
        }
        devicePacketStateData.postValue(DevicePacketState(address, data, -1, PACKET_STATE_RECEIVED))
        packetListenerList.forEach {
            it.onPacketReceived(address, strValue, dataHexString, data)
        }
    }

    /**AT指令返回回调*/
    @WorkerThread
    @Synchronized
    fun _atCommandCallBack(command: String?, param: String?, type: Int, status: Int) {
        val address = lastDeviceAddress() ?: return
        val bean = AtCommandBean(address, command, param, type, status)
        L.d("$address AT指令[$command]返回:${param} $type $status")
        wrapReceiveDevice(address) {
            if (startTime == -1L) {
                startTime = System.currentTimeMillis()
            }
        }
        devicePacketStateData.postValue(
            DevicePacketState(
                address,
                param?.toByteArray() ?: byteArrayOf(),
                100,
                PACKET_STATE_RECEIVED
            )
        )
        packetListenerList.forEach {
            it.onAtCommandCallBack(bean)
        }
    }

    /**空中升级进度
     * status=10086升级成功 status=120升级失败 121升级中
     * [DC:0D:30:1D:3E:E3 空中升级:100 121 121]
     * [DC:0D:30:1D:3E:E3 空中升级:100 10086 10086]
     * */
    @WorkerThread
    @Synchronized
    fun _otaProgressUpdate(address: String, percentage: Int, status: Int) {
        val str = if (status == 10086) "成功" else if (status == 120) "失败" else "..."
        L.d("$address 空中升级:${percentage} $status $str")
        wrapReceiveDevice(address) {
            if (startTime == -1L) {
                startTime = System.currentTimeMillis()
            }
        }
        devicePacketStateData.postValue(
            DevicePacketState(
                address,
                byteArrayOf(),
                percentage,
                PACKET_STATE_RECEIVED
            )
        )
        packetListenerList.forEach {
            it.onOtaProgressUpdate(address, percentage, status)
        }
    }

    //</editor-fold desc="send and received">
}
