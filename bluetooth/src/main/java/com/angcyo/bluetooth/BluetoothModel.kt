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
import com.angcyo.core.lifecycle.LifecycleViewModel
import com.angcyo.library.ex.isDebug
import com.angcyo.viewmodel.MutableOnceLiveData
import com.angcyo.viewmodel.vmData
import com.angcyo.viewmodel.vmDataOnce
import com.clj.fastble.BleManager
import com.clj.fastble.callback.BleGattCallback
import com.clj.fastble.callback.BleScanCallback
import com.clj.fastble.data.BleDevice
import com.clj.fastble.exception.BleException
import com.clj.fastble.scan.BleScanRuleConfig

/**
 * 蓝牙模型
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/11
 */
class BluetoothModel : LifecycleViewModel() {

    companion object {

        /**蓝牙的状态*/
        const val BLUETOOTH_STATE_NORMAL = 0

        /**扫描中*/
        const val BLUETOOTH_STATE_SCANNING = 1

        /**蓝牙不可用*/
        const val BLUETOOTH_STATE_UNAVAILABLE = 2

        const val REQUEST_CODE_PERMISSION_LOCATION = 0x9902

        /**初始化方法*/
        fun init(application: Application, debug: Boolean = isDebug()) {
            BleManager.getInstance().init(application)

            BleManager.getInstance()
                .enableLog(debug) //重连次数, 重连间隔时间
                .setReConnectCount(1, 5000) //分包写入数量
                .setSplitWriteNum(20) //连接过渡时间
                .setConnectOverTime(10000)
                .setOperateTimeout(5000)

            val scanRuleConfig = BleScanRuleConfig.Builder() // 只扫描指定的服务的设备，可选
                //.setServiceUuids(serviceUuids)
                //.setDeviceName(true, names)
                //// 只扫描指定广播名的设备，可选
                //.setDeviceMac(mac)
                // 连接时的autoConnect参数，可选，默认false
                //.setAutoConnect(isAutoConnect)
                // 扫描超时时间，可选，默认10秒
                .setScanTimeOut(10000)
                .build()
            BleManager.getInstance().initScanRule(scanRuleConfig)
        }

        /**手机是否有蓝牙设备*/
        fun isSupportBle() = BleManager.getInstance().isSupportBle

        /**是否开启了蓝牙*/
        fun isBlueEnable() = BleManager.getInstance().isBlueEnable

        /**GPS是否已打开*/
        fun checkGPSIsOpen(context: Context): Boolean {
            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
                    ?: return false
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        }

        /**设备是否已连接*/
        fun isConnected(bleDevice: BleDevice?): Boolean {
            return BleManager.getInstance().isConnected(bleDevice)
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
            bluetoothStateData.postValue(BLUETOOTH_STATE_NORMAL)
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

    //<editor-fold desc="操作方法">

    /**开始扫描蓝牙设备
     * [context] 使用 Activity 才会申请对应的权限*/
    fun startScan(context: Context = BleManager.getInstance().context) {
        if (bluetoothStateData.value == BLUETOOTH_STATE_SCANNING) {
            //已经在扫描
            return
        }

        val instance = BleManager.getInstance()

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
            BleManager.getInstance().cancelScan()
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

    /**连接设备*/
    fun connect(bleDevice: BleDevice) {
        if (isConnected(bleDevice)) {
            return
        }
        BleManager.getInstance().connect(bleDevice, object : BleGattCallback() {
            override fun onStartConnect() {
                wrapStateDevice(bleDevice) {
                    state = CONNECT_STATE_START
                }
                connectStateData.postValue(DeviceConnectState(bleDevice, CONNECT_STATE_START))
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
            }
        })
    }

    /**断开连接*/
    fun disconnect(bleDevice: BleDevice) {
        BleManager.getInstance().disconnect(bleDevice)
        connectStateData.postValue(
            DeviceConnectState(
                bleDevice,
                CONNECT_STATE_DISCONNECT_START,
                null,
                isActiveDisConnected = true
            )
        )
    }

    override fun release() {
        BleManager.getInstance().disconnectAllDevice()
        BleManager.getInstance().destroy()
        bluetoothDeviceListData.postValue(emptyList())
    }

    //</editor-fold desc="操作方法">
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