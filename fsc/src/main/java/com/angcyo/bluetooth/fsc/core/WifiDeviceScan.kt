package com.angcyo.bluetooth.fsc.core

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.angcyo.bluetooth.fsc.WaitReceivePacket
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.command.QueryCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.parser
import com.angcyo.bluetooth.fsc.laserpacker.parse.QueryDeviceNameParser
import com.angcyo.bluetooth.fsc.laserpacker.writeBleLog
import com.angcyo.http.rx.doMain
import com.angcyo.http.tcp.TcpDevice
import com.angcyo.http.tcp.tcpSend
import com.angcyo.library.annotation.ThreadDes
import com.angcyo.library.component.RConcurrentTask
import com.angcyo.library.ex.clamp
import com.angcyo.library.ex.getWifiIP
import com.angcyo.library.ex.size
import com.angcyo.library.ex.syncSingle
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.max
import kotlin.math.min

/**
 * wifi设备扫描
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/08/02
 */
class WifiDeviceScan {

    companion object {
        const val STATE_SCAN_START = 1
        const val STATE_SCAN_FINISH = 2
        const val STATE_SCAN_ERROR = 3
    }

    var lifecycleOwner: LifecycleOwner? = null

    private val lifecycleObserver = LifecycleEventObserver { source, event ->
        if (event == Lifecycle.Event.ON_DESTROY) {
            //销毁之后, 自动移除
            cancel()
        }
    }

    /**扫描状态回调*/
    @ThreadDes("工作线程回调")
    var scanStateAction: (state: Int) -> Unit = {}

    /**扫到的设备回调*/
    @ThreadDes("工作线程回调")
    var scanDeviceAction: (device: TcpDevice) -> Unit = {}

    /**
     * [com.angcyo.bluetooth.fsc.core.WifiDeviceScan.STATE_SCAN_START]
     * [com.angcyo.bluetooth.fsc.core.WifiDeviceScan.STATE_SCAN_FINISH]
     * [com.angcyo.bluetooth.fsc.core.WifiDeviceScan.STATE_SCAN_ERROR]
     * */
    var _state = 0
    private var scanTask: RConcurrentTask? = null

    /**开始扫描*/
    fun startScan(port: Int): Boolean {
        if (_state == STATE_SCAN_START) {
            return false
        }

        val wifiIP = getWifiIP()
        val forepartIp = wifiIP?.substring(0, wifiIP.lastIndexOf("."))

        return if (forepartIp.isNullOrBlank()) {
            _state = STATE_SCAN_ERROR
            scanStateAction(_state)
            false
        } else {
            //开始扫描
            lifecycleOwner?.lifecycle?.addObserver(lifecycleObserver)
            _state = STATE_SCAN_START
            scanStateAction(_state)
            val queue = ConcurrentLinkedQueue<ScanTask>()

            val rangeList = HawkEngraveKeys.scanIpRange?.split("~")
            val _min = 1
            val _max = 254
            val v1 = rangeList?.get(0)?.toIntOrNull() ?: _min
            val v2 = rangeList?.get(1)?.toIntOrNull() ?: _max
            val min = min(v1, v2)
            val max = max(v1, v2)

            val startIp = clamp(HawkEngraveKeys.scanStartIp, min, max)
            for (i in startIp..max) {
                queue.add(ScanTask("$forepartIp.$i", port))
            }
            for (i in (startIp - 1) downTo min) {
                queue.add(ScanTask("$forepartIp.$i", port))
            }
            scanTask = RConcurrentTask(queue, onFinish = {
                doMain {
                    lifecycleOwner?.lifecycle?.removeObserver(lifecycleObserver)
                }
                _state = if (it == null) {
                    STATE_SCAN_FINISH
                } else {
                    STATE_SCAN_ERROR
                }
                scanStateAction(_state)
            })
            true
        }
    }

    /**取消扫描*/
    fun cancel() {
        lifecycleOwner?.lifecycle?.removeObserver(lifecycleObserver)
        lifecycleOwner = null
        scanTask?.cancel()
        scanTask = null
    }

    /**扫描任务*/
    inner class ScanTask(val ip: String, val port: Int) : Runnable {
        override fun run() {
            syncSingle {
                //L.w("开始扫描:$ip:$port")
                tcpSend(ip, port, QueryCmd.deviceName.toByteArray()) { receiveBytes, error ->
                    val bytes = WaitReceivePacket.checkReceiveFinish(receiveBytes)
                    bytes?.let {
                        //L.w("扫描结果[${it.size()}]:$ip:$port ${it.toHexString()}")
                        if (scanTask?.isCancel?.get() == false) {
                            val parser = it.parser<QueryDeviceNameParser>()
                            val deviceName = parser?.deviceName ?: "LP5-$ip"
                            "扫描结果[${it.size()}]:$ip:$port $deviceName".writeBleLog()
                            scanDeviceAction(TcpDevice(ip, port, deviceName))
                        }
                    }
                    it.countDown()
                }.apply {
                    log = false
                    soTimeout = HawkEngraveKeys.scanPortTimeout
                }
            }
        }
    }
}