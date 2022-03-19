package com.angcyo.bluetooth.fsc

import android.Manifest
import android.app.Activity
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.angcyo.bluetooth.fsc.DeviceConnectState.Companion.CONNECT_STATE_DISCONNECT
import com.angcyo.bluetooth.fsc.DeviceConnectState.Companion.CONNECT_STATE_START
import com.angcyo.bluetooth.fsc.DeviceConnectState.Companion.CONNECT_STATE_SUCCESS
import com.angcyo.core.lifecycle.LifecycleViewModel
import com.angcyo.library.app
import com.angcyo.library.ex.isDebug
import com.angcyo.viewmodel.MutableOnceLiveData
import com.angcyo.viewmodel.vmData
import com.angcyo.viewmodel.vmDataOnce
import com.feasycom.ble.controler.FscBleCentralApi
import com.feasycom.ble.controler.FscBleCentralApiImp
import com.feasycom.ble.controler.FscBleCentralCallbacksImp
import com.feasycom.common.bean.ConnectType
import com.feasycom.common.bean.FscDevice
import com.feasycom.common.controler.FscApi
import com.feasycom.spp.controler.FscSppCentralApi
import com.feasycom.spp.controler.FscSppCentralApiImp
import com.feasycom.spp.controler.FscSppCentralCallbacksImp

/**
 * 蓝牙模型
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/19
 */
class FscBleApiModel : LifecycleViewModel() {

    companion object {

        /**蓝牙的状态*/
        const val BLUETOOTH_STATE_NORMAL = 0

        /**扫描中*/
        const val BLUETOOTH_STATE_SCANNING = 1

        /**蓝牙不可用*/
        const val BLUETOOTH_STATE_UNAVAILABLE = 2

        const val REQUEST_CODE_PERMISSION_LOCATION = 0x9902

        val bleApi: FscBleCentralApi
            get() = FscBleCentralApiImp.getInstance()

        val sppApi: FscSppCentralApi
            get() = FscSppCentralApiImp.getInstance()

        /**初始化方法*/
        fun init(application: Application, debug: Boolean = isDebug()) {
            FscBleCentralApiImp.getInstance(application).apply {
                initialize()
                isShowLog(debug)
            }
            FscSppCentralApiImp.getInstance(application).apply {
                initialize()
                isShowLog(debug)
            }
        }

        /**手机是否有蓝牙设备*/
        fun isSupportBle() = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
                && app().packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))

        /**是否开启了蓝牙*/
        fun isBlueEnable() =
            FscBleCentralApiImp.getInstance().isEnabled || FscSppCentralApiImp.getInstance().isEnabled

        /**GPS是否已打开*/
        fun checkGPSIsOpen(context: Context): Boolean {
            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
                    ?: return false
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        }

        /**激活蓝牙*/
        fun enableBluetooth(context: Context = app()): Boolean {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
            return BluetoothAdapter.getDefaultAdapter().enable()
        }
    }

    //扫描回调
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
            _peripheralFound(device)
        }

        override fun servicesFound(
            p0: BluetoothGatt?,
            p1: String?,
            p2: MutableList<BluetoothGattService>?
        ) {
            super.servicesFound(p0, p1, p2)
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
        override fun blePeripheralDisconnected(gatt: BluetoothGatt, address: String, p2: Int) {
            super.blePeripheralDisconnected(gatt, address, p2)
            _peripheralDisconnected(address, gatt)
        }

        /**
         *  发送文件进度
         *  @param address 设备地址
         *  @param percentage 进度
         *  @param data 源数据
         */
        override fun sendPacketProgress(address: String, percentage: Int, sendByte: ByteArray) {
            super.sendPacketProgress(address, percentage, sendByte)
        }

        /**
         *  发送数据
         *  @param address 设备地址
         *  @param strValue 字符串
         *  @param data 源数据
         */
        override fun packetSend(address: String, strValue: String, data: ByteArray) {
            super.packetSend(address, strValue, data)
        }

        /**
         *  收到数据
         *  @param address 设备地址
         *  @param strValue 字符串
         *  @param dataHexString 十六进制
         *  @param data 源数据
         */
        override fun packetReceived(
            address: String,
            strValue: String,
            dataHexString: String,
            data: ByteArray
        ) {
            super.packetReceived(address, strValue, dataHexString, data)
        }

        /**
         *  OTA 升级进度
         *  @param address 设备地址
         *  @param percentage 进度
         *  @param status 状态
         */
        override fun otaProgressUpdate(address: String, percentage: Int, status: Int) {
            super.otaProgressUpdate(address, percentage, status)
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
        }

        /**
         *  AT 指令发送结束时触发
         */
        override fun endATCommand() {
            super.endATCommand()
        }

        /**
         *  开始发送AT指令时触发
         */
        override fun startATCommand() {
            super.startATCommand()
        }
    }

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
            _peripheralFound(sppDevice)
        }

        // 连接成功
        override fun sppPeripheralConnected(device: BluetoothDevice, type: ConnectType) {
            super.sppPeripheralConnected(device, type)
            _peripheralConnected(device.address, null, type)
        }

        // 断开连接
        override fun sppPeripheralDisconnected(address: String) {
            super.sppPeripheralDisconnected(address)
            _peripheralDisconnected(address, null)
        }

        /**
         *  发送文件进度
         *  @param address 设备地址
         *  @param percentage 进度
         *  @param data 源数据
         */
        override fun sendPacketProgress(address: String, percentage: Int, sendByte: ByteArray) {
            super.sendPacketProgress(address, percentage, sendByte)
        }

        /**
         *  发送数据
         *  @param address 设备地址
         *  @param strValue 字符串
         *  @param data 源数据
         */
        override fun packetSend(address: String, strValue: String, data: ByteArray) {
            super.packetSend(address, strValue, data)
        }

        /**
         * 发送数据
         * @param address       设备地址
         * @param strValue      发送的数据
         * @param hexString     发送的十六进制数据
         * @param data          原数据
         */
        override fun packetSend(
            address: String,
            strValue: String,
            packetReceived: String,
            data: ByteArray?
        ) {
            super.packetSend(address, strValue, packetReceived, data)
        }

        /**
         *  收到数据
         *  @param address 设备地址
         *  @param strValue 字符串
         *  @param dataHexString 十六进制
         *  @param data 源数据
         */
        override fun packetReceived(
            address: String,
            strValue: String,
            dataHexString: String,
            data: ByteArray
        ) {
            super.packetReceived(address, strValue, dataHexString, data)
        }

        /**
         *  OTA 升级进度
         *  @param address 设备地址
         *  @param percentage 进度
         *  @param status 状态
         */
        override fun otaProgressUpdate(address: String, percentage: Int, status: Int) {
            super.otaProgressUpdate(address, percentage, status)
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
        }

        /**
         *  AT 指令发送结束时触发
         */
        override fun endATCommand() {
            super.endATCommand()
        }

        /**
         *  开始发送AT指令时触发
         */
        override fun startATCommand() {
            super.startATCommand()
        }
    }

    /**蓝牙的状态*/
    val bleStateData: MutableLiveData<Int> = vmData(BLUETOOTH_STATE_NORMAL)

    /**扫描到的蓝牙设备*/
    val bleDeviceData: MutableOnceLiveData<FscDevice> = vmDataOnce(null)

    /**扫描到的所有蓝牙设备*/
    val bleDeviceListData: MutableLiveData<List<FscDevice>> = vmData(emptyList())

    /**连接状态监听*/
    val connectStateData: MutableOnceLiveData<DeviceConnectState> = vmDataOnce(null)

    val handle = Handler(Looper.getMainLooper())

    /**是否使用spp模式扫描蓝牙设备*/
    var useSppScan = false
        set(value) {
            field = value
            if (value && bleStateData.value == BLUETOOTH_STATE_SCANNING) {
                fscApi.stopScan()
                //fscApi.stopSend()
            }
        }

    /**是否激活spp模式, 走spp api, 并且激活sdp服务*/
    var useSppModel = false

    /**api*/
    val fscApi: FscApi<*>
        get() = if (useSppModel) sppApi else bleApi

    init {
        bleApi.setCallbacks(bleCallback)
        sppApi.setCallbacks(sppCallback)
    }

    //<editor-fold desc="操作方法">

    /**设备是否已连接*/
    fun isConnected(bleDevice: FscDevice?): Boolean {
        return bleDevice?.address?.run { fscApi.isConnected(this) } ?: false
    }

    /**开始扫描蓝牙设备
     * [delayStop] 多少毫秒后, 自动关闭扫描
     * [context] 使用 Activity 才会申请对应的权限*/
    fun startScan(delayStop: Long = 10_000, context: Context = app()) {
        if (bleStateData.value == BLUETOOTH_STATE_SCANNING) {
            //已经在扫描
            return
        }

        val instance = fscApi

        if (!isSupportBle()) {
            //不支持蓝牙的设备
            bleStateData.postValue(BLUETOOTH_STATE_UNAVAILABLE)
            return
        } else if (!isBlueEnable()) {
            //未开启蓝牙
            bleStateData.postValue(BLUETOOTH_STATE_UNAVAILABLE)
            enableBluetooth()

            //等待2s之后, 继续操作
            handle.postDelayed({
                if (isBlueEnable()) {
                    startScan(delayStop, context)
                }
            }, 1800L)
            return
        }

        //扫描
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkPermissions(context)) {
                instance.startScan()
            }
        } else {
            instance.startScan()
        }

        //自动停止扫描
        if (delayStop > 0) {
            handle.postDelayed({
                stopScan()
            }, delayStop)
        }
    }

    /**停止扫描*/
    fun stopScan() {
        if (bleStateData.value == BLUETOOTH_STATE_SCANNING) {
            //已经在扫描
            bleStateData.postValue(BLUETOOTH_STATE_NORMAL)
            fscApi.stopScan()
            return
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
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val permissionDeniedList: MutableList<String> = ArrayList()
        for (permission in permissions) {
            val permissionCheck = ContextCompat.checkSelfPermission(context, permission)
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
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

    /**连接设备*/
    fun connect(bleDevice: FscDevice) {
        if (isConnected(bleDevice)) {
            return
        }
        connectStateData.postValue(DeviceConnectState(bleDevice, CONNECT_STATE_START))
        fscApi.connect(bleDevice.address)
    }

    /**断开连接*/
    fun disconnect(bleDevice: FscDevice) {
        if (isConnected(bleDevice)) {
            fscApi.disconnect(bleDevice.address)
            connectStateData.postValue(
                DeviceConnectState(
                    bleDevice,
                    CONNECT_STATE_DISCONNECT,
                    null,
                    isActiveDisConnected = true
                )
            )
        }
    }

    override fun release() {
        fscApi.disconnect()
        bleDeviceListData.postValue(null)
    }

    //</editor-fold desc="操作方法">

    //<editor-fold desc="Callback">

    val cacheScanDeviceList = mutableListOf<FscDevice>()

    fun _startScan() {
        cacheScanDeviceList.clear()
        bleStateData.postValue(BLUETOOTH_STATE_SCANNING)
    }

    fun _stopScan() {
        bleStateData.postValue(BLUETOOTH_STATE_NORMAL)
        bleDeviceListData.postValue(cacheScanDeviceList)
    }

    fun _peripheralFound(device: FscDevice) {
        bleDeviceData.postValue(device)
        if (!cacheScanDeviceList.contains(device)) {
            cacheScanDeviceList.add(device)
        }
    }

    fun _peripheralConnected(address: String, gatt: BluetoothGatt?, type: ConnectType) {
        bleDeviceListData.value?.find { it.address == address }?.let { bleDevice ->
            connectStateData.postValue(
                DeviceConnectState(
                    bleDevice,
                    CONNECT_STATE_SUCCESS,
                    gatt,
                    type
                )
            )
        }
    }

    fun _peripheralDisconnected(address: String, gatt: BluetoothGatt?) {
        bleDeviceListData.value?.find { it.address == address }?.let { bleDevice ->
            connectStateData.postValue(
                DeviceConnectState(
                    bleDevice,
                    CONNECT_STATE_DISCONNECT,
                    gatt
                )
            )
        }
    }

    //</editor-fold desc="Callback">
}

/**连接状态*/
data class DeviceConnectState(
    val bleDevice: FscDevice,
    val state: Int = CONNECT_STATE_NORMAL,
    val gatt: BluetoothGatt? = null,
    val type: ConnectType? = null,
    val exception: Exception? = null,
    val isActiveDisConnected: Boolean = false //主动断开连接
) {
    companion object {
        const val CONNECT_STATE_NORMAL = 0
        const val CONNECT_STATE_START = 1
        const val CONNECT_STATE_SUCCESS = 2
        const val CONNECT_STATE_FAIL = 3
        const val CONNECT_STATE_DISCONNECT = 4
    }
}