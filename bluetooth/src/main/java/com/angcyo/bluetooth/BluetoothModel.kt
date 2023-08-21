package com.angcyo.bluetooth

import android.Manifest
import android.app.Activity
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.angcyo.bluetooth.DeviceConnectState.Companion.CONNECT_STATE_DISCONNECT
import com.angcyo.bluetooth.DeviceConnectState.Companion.CONNECT_STATE_DISCONNECT_START
import com.angcyo.bluetooth.DeviceConnectState.Companion.CONNECT_STATE_FAIL
import com.angcyo.bluetooth.DeviceConnectState.Companion.CONNECT_STATE_START
import com.angcyo.bluetooth.DeviceConnectState.Companion.CONNECT_STATE_SUCCESS
import com.angcyo.core.component.file.writeToLog
import com.angcyo.core.lifecycle.LifecycleViewModel
import com.angcyo.http.rx.doMain
import com.angcyo.library.L
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.component._removeMainRunnable
import com.angcyo.library.component.onMainDelay
import com.angcyo.library.ex.isDebug
import com.angcyo.viewmodel.MutableOnceLiveData
import com.angcyo.viewmodel.vmData
import com.angcyo.viewmodel.vmDataOnce
import com.clj.fastble.BleManager
import com.clj.fastble.callback.BleGattCallback
import com.clj.fastble.callback.BleNotifyCallback
import com.clj.fastble.callback.BleReadCallback
import com.clj.fastble.callback.BleScanCallback
import com.clj.fastble.callback.BleWriteCallback
import com.clj.fastble.data.BleDevice
import com.clj.fastble.exception.BleException
import com.clj.fastble.scan.BleScanRuleConfig
import java.nio.charset.Charset
import java.util.concurrent.CopyOnWriteArraySet

/**
 * 蓝牙模型
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/11
 */
class BluetoothModel : LifecycleViewModel() {

    companion object {

        /**蓝牙状态:蓝牙的状态*/
        const val BLUETOOTH_STATE_NORMAL = 0

        /**蓝牙状态:扫描中*/
        const val BLUETOOTH_STATE_SCANNING = 1

        /**蓝牙状态:蓝牙不可用*/
        const val BLUETOOTH_STATE_UNAVAILABLE = 2

        /**蓝牙状态:扫描完成*/
        const val BLUETOOTH_STATE_FINISH = 3

        /**权限请求码*/
        const val REQUEST_CODE_PERMISSION_LOCATION = 0x9902

        /**[BleManager]*/
        val bleManager = BleManager.getInstance()

        /**扫描时长*/
        var scanTimeout = 5_000L

        /**初始化方法*/
        @CallPoint
        fun init(application: Application, debug: Boolean = isDebug()) {
            bleManager.init(application)

            bleManager.enableLog(debug) //重连次数, 重连间隔时间
                .setReConnectCount(1, scanTimeout) //分包写入数量
                .setSplitWriteNum(20) //连接过渡时间
                .setConnectOverTime(10000)
                .setOperateTimeout(scanTimeout.toInt())

            val scanRuleConfig = BleScanRuleConfig.Builder() // 只扫描指定的服务的设备，可选
                //.setServiceUuids(serviceUuids)
                //.setDeviceName(true, names)
                //// 只扫描指定广播名的设备，可选
                //.setDeviceMac(mac)
                // 连接时的autoConnect参数，可选，默认false
                //.setAutoConnect(isAutoConnect)
                // 扫描超时时间，可选，默认10秒
                .setScanTimeOut(scanTimeout)
                .build()
            bleManager.initScanRule(scanRuleConfig)
        }

        /**手机是否有蓝牙设备*/
        fun isSupportBle() = bleManager.isSupportBle

        /**是否开启了蓝牙*/
        fun isBlueEnable() = bleManager.isBlueEnable

        /**GPS是否已打开*/
        fun checkGPSIsOpen(context: Context): Boolean {
            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
                    ?: return false
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        }

        /**设备是否已连接*/
        fun isConnected(bleDevice: BleDevice?): Boolean {
            return bleManager.isConnected(bleDevice)
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
    }

    //扫描回调
    val scanCallback = object : BleScanCallback() {
        override fun onScanStarted(success: Boolean) {
            if (!success) {
                bluetoothStateData.postValue(BLUETOOTH_STATE_NORMAL)
            }
        }

        override fun onLeScan(bleDevice: BleDevice) {
            super.onLeScan(bleDevice)
            bluetoothScanDeviceData.postValue(bleDevice)
        }

        override fun onScanning(bleDevice: BleDevice) {
            bluetoothDeviceData.postValue(bleDevice)
        }

        override fun onScanFinished(scanResultList: List<BleDevice>) {
            bluetoothDeviceListData.postValue(scanResultList)
            bluetoothStateData.postValue(BLUETOOTH_STATE_FINISH)
        }
    }

    /**蓝牙的状态*/
    val bluetoothStateData: MutableLiveData<Int> = vmData(BLUETOOTH_STATE_NORMAL)

    /**扫描到的蓝牙设备, 不重复的设备*/
    val bluetoothDeviceData: MutableOnceLiveData<BleDevice> = vmDataOnce(null)

    /**扫到了设备变化, 会出现重复的数据*/
    val bluetoothScanDeviceData: MutableOnceLiveData<BleDevice> = vmDataOnce(null)

    /**扫描到的所有蓝牙设备*/
    val bluetoothDeviceListData: MutableLiveData<List<BleDevice>> = vmData(emptyList())

    /**连接状态监听*/
    val connectStateData: MutableOnceLiveData<DeviceConnectState> = vmDataOnce(null)

    /**设备连接状态列表,包括已连接的设备*/
    val connectDeviceList = mutableListOf<DeviceConnectState>()

    /**处理超时的时长*/
    val handleTimeout: Long = scanTimeout

    //<editor-fold desc="操作方法">

    /**开始扫描蓝牙设备
     * [context] 使用 Activity 才会申请对应的权限*/
    fun startScan(context: Context = bleManager.context) {
        if (bluetoothStateData.value == BLUETOOTH_STATE_SCANNING) {
            //已经在扫描
            return
        }

        val instance = bleManager

        if (!instance.isSupportBle) {
            //不支持蓝牙的设备
            bluetoothStateData.postValue(BLUETOOTH_STATE_UNAVAILABLE)
            return
        } else if (!instance.isBlueEnable) {
            //未开启蓝牙
            bluetoothStateData.postValue(BLUETOOTH_STATE_UNAVAILABLE)
            instance.enableBluetooth()

            //等待2s之后, 继续操作
            Handler(Looper.getMainLooper()).postDelayed({
                if (instance.isBlueEnable) {
                    startScan(context)
                }
            }, 1800L)
            return
        }

        //扫描
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkPermissions(context)) {
                bluetoothStateData.postValue(BLUETOOTH_STATE_SCANNING)
                instance.scan(scanCallback)
            }
        } else {
            bluetoothStateData.postValue(BLUETOOTH_STATE_SCANNING)
            instance.scan(scanCallback)
        }
    }

    /**停止扫描*/
    fun stopScan() {
        if (bluetoothStateData.value == BLUETOOTH_STATE_SCANNING) {
            //已经在扫描
            bluetoothStateData.postValue(BLUETOOTH_STATE_NORMAL)
            bleManager.cancelScan()
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
        val permissions = bluetoothPermissionList()
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
    fun wrapStateDevice(device: BleDevice, action: DeviceConnectState.() -> Unit) {
        val state = connectDeviceList.find { it.device == device } ?: DeviceConnectState(device)
        state.action()
        connectDeviceList.remove(state)
        connectDeviceList.add(state)
    }

    fun connectState(bleDevice: BleDevice?): Int {
        val find = connectDeviceList.find { it.device == bleDevice }
        if (find == null) {
            if (isConnected(bleDevice)) {
                return CONNECT_STATE_SUCCESS
            }
            return CONNECT_STATE_DISCONNECT
        }
        return find.state
    }

    /**通过[address]获取[BleDevice]*/
    fun getBleDevice(address: String): BleDevice? {
        return bleManager.allConnectedDevice.find { it.device.address == address }
            ?: connectDeviceList.find { it.device.device.address == address }?.device
    }

    /**连接回调*/
    private val connectListenerList = CopyOnWriteArraySet<BleGattCallback>()

    fun addConnectListener(listener: BleGattCallback) {
        connectListenerList.add(listener)
    }

    fun removeConnectListener(listener: BleGattCallback) {
        connectListenerList.remove(listener)
    }

    /**连接设备*/
    fun connect(bleDevice: BleDevice) {
        if (isConnected(bleDevice)) {
            return
        }
        bleManager.connect(bleDevice, object : BleGattCallback() {
            override fun onStartConnect() {
                wrapStateDevice(bleDevice) {
                    state = CONNECT_STATE_START
                }
                connectStateData.postValue(DeviceConnectState(bleDevice, CONNECT_STATE_START))
                connectListenerList.forEach {
                    it.onStartConnect()
                }
            }

            override fun onConnectFail(bleDevice: BleDevice, exception: BleException) {
                wrapStateDevice(bleDevice) {
                    state = CONNECT_STATE_FAIL
                    this.exception = exception
                }
                connectStateData.postValue(
                    DeviceConnectState(
                        bleDevice,
                        CONNECT_STATE_FAIL,
                        exception = exception
                    )
                )
                connectListenerList.forEach {
                    it.onConnectFail(bleDevice, exception)
                }
            }

            override fun onConnectSuccess(bleDevice: BleDevice, gatt: BluetoothGatt, status: Int) {
                wrapStateDevice(bleDevice) {
                    this.state = CONNECT_STATE_SUCCESS
                    this.gatt = gatt
                }
                connectStateData.postValue(
                    DeviceConnectState(
                        bleDevice,
                        CONNECT_STATE_SUCCESS,
                        gatt
                    )
                )
                connectListenerList.forEach {
                    it.onConnectSuccess(bleDevice, gatt, status)
                }
            }

            override fun onDisConnected(
                isActiveDisConnected: Boolean,
                bleDevice: BleDevice,
                gatt: BluetoothGatt,
                status: Int
            ) {
                connectDeviceList.removeAll { it.device == bleDevice }
                connectStateData.postValue(
                    DeviceConnectState(
                        bleDevice,
                        CONNECT_STATE_DISCONNECT,
                        gatt,
                        isActiveDisConnected = isActiveDisConnected
                    )
                )
                connectListenerList.forEach {
                    it.onDisConnected(isActiveDisConnected, bleDevice, gatt, status)
                }
            }
        })
    }

    /**连接设备, 并直接回调*/
    fun connect(
        bleDevice: BleDevice,
        timeout: Long = handleTimeout,
        action: (connected: Boolean) -> Unit
    ): BleGattCallback? {
        return if (bleManager.isConnected(bleDevice)) {
            action(true)
            null
        } else {
            var isTimeOut = false
            var timeoutRunnable: Runnable? = null
            val listener = object : BleGattCallback() {
                override fun onStartConnect() {
                    "bleStartConnect...".writeToLog(logLevel = L.DEBUG)
                }

                override fun onConnectFail(bleDevice: BleDevice?, exception: BleException?) {
                    "bleConnectFail:${bleDevice?.device} $exception".writeToLog(logLevel = L.DEBUG)
                }

                override fun onDisConnected(
                    isActiveDisConnected: Boolean,
                    bleDevice: BleDevice?,
                    gatt: BluetoothGatt?,
                    status: Int
                ) {
                    "bleDisConnected[$status]:${bleDevice?.device} $isActiveDisConnected".writeToLog(
                        logLevel = L.DEBUG
                    )
                }

                override fun onConnectSuccess(
                    bleDevice: BleDevice?,
                    gatt: BluetoothGatt?,
                    status: Int
                ) {
                    "bleConnectSuccess[$status]:${bleDevice?.device}".writeToLog(logLevel = L.DEBUG)
                    removeConnectListener(this)
                    _removeMainRunnable(timeoutRunnable)
                    if (!isTimeOut) {
                        action(true)
                    }
                }
            }
            timeoutRunnable = Runnable {
                isTimeOut = true
                removeConnectListener(listener)
                action(bleManager.isConnected(bleDevice))
            }
            if (timeout > 0) {
                onMainDelay(timeout, timeoutRunnable)
            }
            addConnectListener(listener)
            connect(bleDevice)
            return listener
        }
    }

    /**断开连接*/
    fun disconnect(bleDevice: BleDevice) {
        connectStateData.postValue(
            DeviceConnectState(
                bleDevice,
                CONNECT_STATE_DISCONNECT_START,
                null,
                isActiveDisConnected = true
            )
        )
        bleManager.disconnect(bleDevice)
    }

    /**断开所有设备*/
    fun disconnectAllDevice() {
        bleManager.disconnectAllDevice()
    }

    override fun release(data: Any?) {
        bleManager.disconnectAllDevice()
        bleManager.destroy()
        bluetoothDeviceListData.postValue(emptyList())
    }

    //</editor-fold desc="操作方法">

    //<editor-fold desc="数据相关">

    /**notify回调*/
    private val notifyListenerList = CopyOnWriteArraySet<INotifyAction>()

    fun addNotifyListener(listener: INotifyAction) {
        notifyListenerList.add(listener)
    }

    fun removeNotifyListener(listener: INotifyAction) {
        notifyListenerList.remove(listener)
    }

    /**开始监听指定特性的数据通知
     * [notifyUuid] 同一特征, 只能设置一个监听*/
    fun listenerNotify(
        bleDevice: BleDevice,
        serviceUuid: String,
        notifyUuid: String,
        listener: INotifyAction  /*监听是否成功设置*/
    ): INotifyAction {
        addNotifyListener(listener)
        notify(bleDevice, serviceUuid, notifyUuid, object : BleNotifyCallback() {
            override fun onNotifySuccess() {
                notifyListenerList.forEach {
                    it(null, null)
                }
            }

            override fun onNotifyFailure(exception: BleException?) {
                notifyListenerList.forEach {
                    it(null, exception)
                }
            }

            override fun onCharacteristicChanged(data: ByteArray?) {
                notifyListenerList.forEach {
                    it(data, null)
                }
            }
        })
        //removeNotifyListener(listener)
        return listener
    }

    /**监听数据
     * https://github.com/Jasonchenlijian/FastBle/wiki/FastBle%E6%93%8D%E4%BD%9C%E8%AF%B4%E6%98%8E#%E8%AE%A2%E9%98%85%E9%80%9A%E7%9F%A5notify
     * */
    fun notify(
        bleDevice: BleDevice,
        serviceUuid: String,
        notifyUuid: String,
        callback: BleNotifyCallback
    ): BleNotifyCallback {
        bleManager.notify(bleDevice, serviceUuid, notifyUuid, callback)
        return callback
    }

    /**https://github.com/Jasonchenlijian/FastBle/wiki/FastBle%E6%93%8D%E4%BD%9C%E8%AF%B4%E6%98%8E#%E5%8F%96%E6%B6%88%E8%AE%A2%E9%98%85%E9%80%9A%E7%9F%A5notify%E5%B9%B6%E7%A7%BB%E9%99%A4%E6%95%B0%E6%8D%AE%E6%8E%A5%E6%94%B6%E7%9A%84%E5%9B%9E%E8%B0%83%E7%9B%91%E5%90%AC*/
    fun stopNotify(bleDevice: BleDevice, serviceUuid: String, notifyUuid: String) {
        bleManager.stopNotify(bleDevice, serviceUuid, notifyUuid)
    }

    /**写入数据, 并监听返回值*/
    fun writeAndListener(
        bleDevice: BleDevice,
        serviceUuid: String,
        writeUuid: String,
        notifyUuid: String,
        bytes: ByteArray,
        action: INotifyAction
    ) {
        listenerNotify(bleDevice, serviceUuid, notifyUuid) { data, exception ->
            if (data == null && exception == null) {
                //订阅通知成功, 开始写入
                write(bleDevice, serviceUuid, writeUuid, bytes, object : BleWriteCallback() {
                    override fun onWriteSuccess(current: Int, total: Int, justWrite: ByteArray?) {
                        "蓝牙写入数据[${bleDevice.device}][${current}/${total}]:${
                            justWrite?.toString(
                                Charset.defaultCharset()
                            )
                        }".writeToLog(
                            logLevel = L.INFO
                        )
                    }

                    override fun onWriteFailure(exception: BleException?) {
                        "蓝牙写入数据失败[${bleDevice.device}]:$exception".writeToLog(logLevel = L.ERROR)
                        doMain {
                            action(null, exception)
                        }
                    }
                })
            } else if (exception != null) {
                "蓝牙写入数据失败[${bleDevice.device}]:$exception".writeToLog(logLevel = L.ERROR)
                action(null, exception)
            } else {
                //收到了数据
                stopNotify(bleDevice, serviceUuid, notifyUuid)//自动停止监听
                action(data, null)
            }
        }
    }

    /**写入数据
     * https://github.com/Jasonchenlijian/FastBle/wiki/FastBle%E6%93%8D%E4%BD%9C%E8%AF%B4%E6%98%8E#%E5%86%99
     * ```
     * BleException { code=102, description='exception occur while writing: gatt writeCharacteristic fail'}
     * BleException { code=102, description='exception occur while writing: this characteristic not support write!'}
     * ```
     * */
    fun write(
        bleDevice: BleDevice,
        serviceUuid: String,
        writeUuid: String,
        bytes: ByteArray,
        callback: BleWriteCallback
    ): BleWriteCallback {
        bleManager.write(bleDevice, serviceUuid, writeUuid, bytes, callback)
        return callback
    }

    /**监听数据
     * https://github.com/Jasonchenlijian/FastBle/wiki/FastBle%E6%93%8D%E4%BD%9C%E8%AF%B4%E6%98%8E#%E8%AF%BB
     */
    fun read(
        bleDevice: BleDevice,
        serviceUuid: String,
        readUuid: String,
        callback: BleReadCallback
    ): BleReadCallback {
        bleManager.read(bleDevice, serviceUuid, readUuid, callback)
        return callback
    }

    //</editor-fold desc="数据相关">

}

/**连接状态*/
data class DeviceConnectState(
    val device: BleDevice,
    var state: Int = CONNECT_STATE_NORMAL,
    var gatt: BluetoothGatt? = null,
    var exception: BleException? = null,
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