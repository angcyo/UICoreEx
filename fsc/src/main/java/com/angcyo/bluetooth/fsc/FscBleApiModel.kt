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
import android.os.SystemClock
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.angcyo.bluetooth.fsc.DeviceConnectState.Companion.CONNECT_STATE_DISCONNECT
import com.angcyo.bluetooth.fsc.DeviceConnectState.Companion.CONNECT_STATE_DISCONNECT_START
import com.angcyo.bluetooth.fsc.DeviceConnectState.Companion.CONNECT_STATE_START
import com.angcyo.bluetooth.fsc.DeviceConnectState.Companion.CONNECT_STATE_SUCCESS
import com.angcyo.bluetooth.fsc.DevicePacketState.Companion.PACKET_STATE_PAUSE
import com.angcyo.bluetooth.fsc.DevicePacketState.Companion.PACKET_STATE_PROGRESS
import com.angcyo.bluetooth.fsc.DevicePacketState.Companion.PACKET_STATE_RECEIVED
import com.angcyo.bluetooth.fsc.DevicePacketState.Companion.PACKET_STATE_START
import com.angcyo.bluetooth.fsc.DevicePacketState.Companion.PACKET_STATE_STOP
import com.angcyo.library.L
import com.angcyo.library.app
import com.angcyo.library.ex.isDebug
import com.angcyo.library.ex.toHexString
import com.angcyo.viewmodel.IViewModel
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
import java.io.InputStream
import java.util.concurrent.CopyOnWriteArraySet

/**
 * 蓝牙模型
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

        const val REQUEST_CODE_PERMISSION_LOCATION = 0x9902

        /**默认情况下, 34748 bytes/s */
        val bleApi: FscBleCentralApi
            get() = FscBleCentralApiImp.getInstance()

        /**默认情况下, 36412 45544 70826 bytes/s */
        val sppApi: FscSppCentralApi
            get() = FscSppCentralApiImp.getInstance()

        /**初始化方法*/
        fun init(application: Application = app(), debug: Boolean = isDebug()) {
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
        fun isSupportBle() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
                && app().packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

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
            _sendPacketProgress(address, percentage, sendByte)
        }

        /**
         *  发送数据
         *  @param address 设备地址
         *  @param strValue 字符串
         *  @param data 源数据
         */
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
            L.i("AT...${command}")
        }

        /**
         *  AT 指令发送结束时触发
         */
        override fun endATCommand() {
            super.endATCommand()
            L.i("AT...")
        }

        /**
         *  开始发送AT指令时触发
         */
        override fun startATCommand() {
            super.startATCommand()
            L.i("AT...")
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
            _sendPacketProgress(address, percentage, sendByte)
        }

        /**
         *  发送数据
         *  @param address 设备地址
         *  @param strValue 字符串
         *  @param data 源数据
         */
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
            L.i("AT...${command}")
        }

        /**
         *  AT 指令发送结束时触发
         */
        override fun endATCommand() {
            super.endATCommand()
            L.i("AT...")
        }

        /**
         *  开始发送AT指令时触发
         */
        override fun startATCommand() {
            super.startATCommand()
            L.i("AT...")
        }
    }

    /**蓝牙的状态*/
    val bleStateData: MutableLiveData<Int> = vmData(BLUETOOTH_STATE_NORMAL)

    /**扫描到的蓝牙设备, 不重复的设备*/
    val bleDeviceData: MutableOnceLiveData<FscDevice> = vmDataOnce(null)

    /**扫到了设备变化, 会出现重复的数据*/
    val bleScanDeviceData: MutableOnceLiveData<FscDevice> = vmDataOnce(null)

    /**扫描到的所有蓝牙设备*/
    val bleDeviceListData: MutableLiveData<List<FscDevice>> = vmData(emptyList())

    /**连接状态监听*/
    val connectStateData: MutableOnceLiveData<DeviceConnectState> = vmDataOnce(null)

    /**设备连接状态列表,包括已连接的设备*/
    val connectDeviceList = mutableListOf<DeviceConnectState>()

    val handle = Handler(Looper.getMainLooper())

    /**扫描时长*/
    val scanTimeOut = 20_000L

    /**是否使用spp模式扫描蓝牙设备*/
    var useSppScan = false
        set(value) {
            fscApi.stopScan()
            field = value
            if (value) {
                if (bleStateData.value == BLUETOOTH_STATE_SCANNING) {
                    //fscApi.stopSend()
                }
            }
        }

    /**是否激活spp模式, 走spp api, 并且激活sdp服务*/
    var useSppModel = false
        set(value) {
            useSppScan = value
            field = value
        }

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
        return bleDevice?.run {
            connectDeviceList.find { it.device == this }?.run { state == CONNECT_STATE_SUCCESS }
            //address?.run { fscApi.isConnected(this) }
        } ?: false
    }

    /**开始扫描蓝牙设备
     * [delayStop] 多少毫秒后, 自动关闭扫描
     * [context] 使用 Activity 才会申请对应的权限*/
    fun startScan(context: Context = app(), delayStop: Long = scanTimeOut) {
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
                    startScan(context, delayStop)
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
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
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

    /**包裹[DeviceConnectState]*/
    fun wrapStateDevice(device: FscDevice, action: DeviceConnectState.() -> Unit) {
        val state = connectDeviceList.find { it.device == device } ?: DeviceConnectState(device)
        state.action()
        connectDeviceList.remove(state)
        connectDeviceList.add(state)
    }

    fun connectState(bleDevice: FscDevice?): Int {
        val find = connectDeviceList.find { it.device == bleDevice }
        if (find == null) {
            if (isConnected(bleDevice)) {
                return CONNECT_STATE_SUCCESS
            }
            return CONNECT_STATE_DISCONNECT
        }
        return find.state
    }

    /**连接设备*/
    fun connect(bleDevice: FscDevice?) {
        if (bleDevice == null) {
            return
        }
        if (isConnected(bleDevice)) {
            return
        }
        wrapStateDevice(bleDevice) {
            state = CONNECT_STATE_START
        }
        connectStateData.postValue(DeviceConnectState(bleDevice, CONNECT_STATE_START))
        fscApi.connect(bleDevice.address)
    }

    /**断开连接*/
    fun disconnect(bleDevice: FscDevice?) {
        if (bleDevice == null) {
            return
        }
        if (isConnected(bleDevice)) {
            connectStateData.postValue(
                DeviceConnectState(
                    bleDevice,
                    CONNECT_STATE_DISCONNECT_START,
                    null,
                    isActiveDisConnected = true
                )
            )
            stopSend(bleDevice.address)
            fscApi.disconnect(bleDevice.address)
        }
    }

    override fun release() {
        fscApi.stopScan()
        fscApi.stopSend()
        fscApi.disconnect()
        connectDeviceList.clear()
        devicePacketProgressCacheList.clear()
        bleDeviceListData.postValue(emptyList())
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
        bleScanDeviceData.postValue(device)
        if (!cacheScanDeviceList.contains(device)) {
            bleDeviceData.postValue(device)
            cacheScanDeviceList.add(device)
        }
    }

    fun _peripheralConnected(address: String, gatt: BluetoothGatt?, type: ConnectType) {
        bleDeviceListData.value?.find { it.address == address }?.let { bleDevice ->
            wrapStateDevice(bleDevice) {
                this.state = CONNECT_STATE_SUCCESS
                this.gatt = gatt
                this.type = type
            }
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
            connectDeviceList.removeAll { it.device == bleDevice }
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

    //<editor-fold desc="send and received">

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

    /**数据发送状态,进度监听*/
    val devicePacketStateData: MutableOnceLiveData<DevicePacketState> = vmDataOnce(null)

    //发送数据缓存
    val devicePacketProgressCacheList = mutableListOf<DevicePacketProgress>()

    //接收数据缓存
    val devicePacketReceiveCacheList = mutableListOf<DevicePacketReceive>()

    val packetListenerList = CopyOnWriteArraySet<IPacketListener>()

    fun addPacketListener(listener: IPacketListener) {
        packetListenerList.add(listener)
    }

    fun removePacketListener(listener: IPacketListener) {
        packetListenerList.remove(listener)
    }

    fun clearProgressCache(address: String?) {
        devicePacketProgressCacheList.removeAll { it.address == address }
    }

    fun clearReceiveCache(address: String?) {
        devicePacketReceiveCacheList.removeAll { it.address == address }
    }

    /**进度缓存*/
    fun findProgressCache(address: String?): DevicePacketProgress? {
        return devicePacketProgressCacheList.find { it.address == address }
    }

    fun findReceiveCache(address: String?): DevicePacketReceive? {
        return devicePacketReceiveCacheList.find { it.address == address }
    }

    /**包裹[DevicePacketProgress]*/
    fun wrapProgressDevice(address: String, action: DevicePacketProgress.() -> Unit) {
        val element = devicePacketProgressCacheList.find { it.address == address }
            ?: DevicePacketProgress(address)
        element.action()
        devicePacketProgressCacheList.remove(element)
        devicePacketProgressCacheList.add(element)
    }

    fun wrapReceiveDevice(address: String, action: DevicePacketReceive.() -> Unit) {
        val element = devicePacketReceiveCacheList.find { it.address == address }
            ?: DevicePacketReceive(address)
        element.action()
        devicePacketReceiveCacheList.remove(element)
        devicePacketReceiveCacheList.add(element)
    }

    fun _packetSend(address: String, strValue: String, data: ByteArray) {
        L.i("$address 发送:${data.toHexString(true)} ${data.size}bytes")
        wrapProgressDevice(address) {
            sendBytesSize += data.size
            sendPacketCount++
            if (percentage == -1) {
                //记录开始发送的时间
                percentage = 0
                startTime = SystemClock.elapsedRealtime()
            }
        }
        devicePacketStateData.postValue(DevicePacketState(address, data, -1, PACKET_STATE_START))
        packetListenerList.forEach {
            it.onPacketSend(address, strValue, data)
        }
    }

    fun _sendPacketProgress(address: String, percentage: Int, sendByte: ByteArray) {
        L.i("$address 发送进度:$percentage% ${sendByte.size}bytes")
        wrapProgressDevice(address) {
            this.percentage = percentage
            if (percentage == 100) {
                //记录完成发送的时间
                finishTime = SystemClock.elapsedRealtime()
            }
        }
        devicePacketStateData.postValue(
            DevicePacketState(
                address,
                sendByte,
                percentage,
                PACKET_STATE_PROGRESS
            )
        )
        packetListenerList.forEach {
            it.onSendPacketProgress(address, percentage, sendByte)
        }
    }

    /**
     * [address] DC:0D:30:10:19:5E
     * [dataHexString] [AA BB 13 00 06 00 00 00 00 00 00 00 00 00 00 00 06 00 01 00 00 0D ]*/
    fun _packetReceived(address: String, strValue: String, dataHexString: String, data: ByteArray) {
        L.i("$address 收到:$dataHexString ${data.size}bytes")
        wrapReceiveDevice(address) {
            if (startTime == -1L) {
                startTime = SystemClock.elapsedRealtime()
            }
            receiveBytesSize += data.size
            receivePacketCount++
        }
        devicePacketStateData.postValue(DevicePacketState(address, data, -1, PACKET_STATE_RECEIVED))
        packetListenerList.forEach {
            it.onPacketReceived(address, strValue, dataHexString, data)
        }
    }

    //</editor-fold desc="send and received">
}

/**数据包监听回调*/
interface IPacketListener {

    fun onPacketSend(address: String, strValue: String, data: ByteArray) {
    }

    fun onSendPacketProgress(address: String, percentage: Int, sendByte: ByteArray) {
    }

    fun onPacketReceived(
        address: String,
        strValue: String,
        dataHexString: String,
        data: ByteArray
    ) {

    }
}

/**连接状态*/
data class DeviceConnectState(
    val device: FscDevice,
    var state: Int = CONNECT_STATE_NORMAL,
    var gatt: BluetoothGatt? = null,
    var type: ConnectType? = null,
    var exception: Exception? = null,
    var isActiveDisConnected: Boolean = false //主动断开连接
) {
    companion object {
        const val CONNECT_STATE_NORMAL = 0
        const val CONNECT_STATE_START = 1
        const val CONNECT_STATE_SUCCESS = 2
        const val CONNECT_STATE_FAIL = 3
        const val CONNECT_STATE_DISCONNECT_START = 4
        const val CONNECT_STATE_DISCONNECT = 5
    }
}

/**数据发送状态*/
data class DevicePacketState(
    val address: String,
    val bytes: ByteArray,
    /**数据发送的进度[0-100]
     * -1表示发送的数据包
     * [0-100]表示发送包的进度*/
    val percentage: Int = -1,
    val state: Int = PACKET_STATE_NORMAL,
) {
    companion object {
        const val PACKET_STATE_NORMAL = 0

        /**开始发送数据*/
        const val PACKET_STATE_START = 1

        /**发送数据的进度*/
        const val PACKET_STATE_PROGRESS = 2

        /**暂停发送*/
        const val PACKET_STATE_PAUSE = 3

        /**停止发送*/
        const val PACKET_STATE_STOP = 4

        /**接收的数据*/
        const val PACKET_STATE_RECEIVED = 5
    }
}

/**数据发送进度,速度*/
data class DevicePacketProgress(
    val address: String,
    /**已发送成功的字节大小*/
    var sendBytesSize: Long = 0,
    /**发送包的数量*/
    var sendPacketCount: Int = 0,
    /**发送进度[0-100]*/
    var percentage: Int = -1,
    /**发送的开始时间, 毫秒*/
    var startTime: Long = -1,
    /**完成时的时间, 毫秒*/
    var finishTime: Long = -1,
    /**是否暂停了*/
    var isPause: Boolean = false,
)

/**数据接收*/
data class DevicePacketReceive(
    val address: String,
    /**已接收的字节数量*/
    var receiveBytesSize: Long = 0,
    /**接收包的数量*/
    var receivePacketCount: Int = 0,
    /**接收的开始时间, 毫秒*/
    var startTime: Long = -1
)
