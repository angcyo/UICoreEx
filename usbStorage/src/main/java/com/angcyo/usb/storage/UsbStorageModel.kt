package com.angcyo.usb.storage

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import com.angcyo.library.L
import com.angcyo.library.app
import com.angcyo.library.component.pendingIntentMutableFlag
import com.angcyo.viewmodel.updateValue
import com.angcyo.viewmodel.vmDataNull
import com.angcyo.viewmodel.vmDataOnce
import me.jahnen.libaums.core.UsbMassStorageDevice
import java.io.IOException

/**
 * Usb存储数据模型
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/23
 */
class UsbStorageModel : ViewModel() {

    companion object {
        private const val ACTION_USB_PERMISSION = "me.jahnen.libaums.USB_PERMISSION"
    }

    /**上下文*/
    var context: Context? = app()

    /**USB存储设备, 列表*/
    val massStorageDevicesData = vmDataNull<Array<UsbMassStorageDevice>>(null)

    /**选中的存储设备*/
    val selectedMassStorageDevicesData = vmDataNull<UsbMassStorageDevice>(null)

    /**USB存储设备, 可以访问的通知*/
    val usbStorageDevicesOnceData = vmDataOnce<UsbMassStorageDevice>(null)

    val massStorageDevices: Array<UsbMassStorageDevice>
        get() = massStorageDevicesData.value ?: emptyArray()

    val selectedDevice: UsbMassStorageDevice?
        get() = selectedMassStorageDevicesData.value

    /**是否有USB存储设备, 并且具有访问权限*/
    val haveUsbDevice: Boolean
        get() = selectedDevice != null

    private val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (ACTION_USB_PERMISSION == action) {
                val device =
                    intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice?
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    if (device != null) {
                        setupDevice()
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED == action) {
                val device =
                    intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice?
                L.d("USB device attached")
                // determine if connected device is a mass storage devuce
                if (device != null) {
                    discoverDevice()
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
                val device =
                    intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice?
                L.d("USB device detached")

                // determine if connected device is a mass storage devuce
                if (device != null) {
                    selectedDevice?.close()
                    selectedMassStorageDevicesData.updateValue(null)
                    massStorageDevicesData.updateValue(null)
                    // check if there are other devices or set action bar title
                    // to no device if not
                    discoverDevice()
                }
            }
        }
    }

    /**开始监听USB设备(OTG)连接*/
    fun startReceiveUsb() {
        if (context == null) {
            context = app()
        }
        val context = context ?: return
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(usbReceiver, filter)
        }
        discoverDevice()
    }

    /**停止监听USB设备*/
    fun stopReceiveUsb() {
        val context = context ?: return
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (e: Exception) {
            L.e(e)
        }
    }

    /**释放资源*/
    fun release() {
        stopReceiveUsb()
        selectedDevice?.close()
        selectedMassStorageDevicesData.updateValue(null)
        massStorageDevicesData.updateValue(null)
        context = null
    }

    //---

    /**
     * 设置设备并显示根目录的内容。
     * Sets the device up and shows the contents of the root directory. */
    private fun setupDevice() {
        try {
            selectedDevice?.let { device ->
                device.init()
                usbStorageDevicesOnceData.updateValue(device)

                // we always use the first partition of the device
                /*val currentFs = device.partitions[0].fileSystem.also {
                    L.d("Capacity: " + it.capacity)
                    L.d("Occupied Space: " + it.occupiedSpace)
                    L.d("Free Space: " + it.freeSpace)
                    L.d("Chunk size: " + it.chunkSize)
                }
                val root = currentFs.rootDirectory*/
            }
        } catch (e: IOException) {
            L.e("error setting up device", e)
        }
    }

    /**
     * 搜索连接的大容量存储设备，如果可以找到则初始化它们。
     * Searches for connected mass storage devices, and initializes them if it
     * could find some.
     */
    private fun discoverDevice() {
        val context = context ?: return
        val usbManager = context.getSystemService(AppCompatActivity.USB_SERVICE) as UsbManager
        massStorageDevicesData.updateValue(UsbMassStorageDevice.getMassStorageDevices(context))
        if (massStorageDevices.isEmpty()) {
            return
        }
        val usbMassStorageDevice = massStorageDevices.firstOrNull()
        selectedMassStorageDevicesData.updateValue(usbMassStorageDevice)
        val usbDevice = usbMassStorageDevice?.usbDevice
        if (usbDevice != null && usbManager.hasPermission(usbDevice)) {
            L.d("received usb device via intent")
            // requesting permission is not needed in this case
            setupDevice()
        } else {
            // first request permission from user to communicate with the underlying UsbDevice
            val permissionIntent = PendingIntent.getBroadcast(
                context,
                0,
                Intent(ACTION_USB_PERMISSION),
                0.pendingIntentMutableFlag()
            )
            //USB存储设备权限申请
            usbManager.requestPermission(selectedDevice?.usbDevice, permissionIntent)
        }
    }
}