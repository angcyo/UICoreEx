package com.angcyo.bluetooth.fsc

import android.os.Handler
import android.os.Looper
import androidx.annotation.WorkerThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.angcyo.bluetooth.fsc.core.DevicePacketProgress
import com.angcyo.bluetooth.fsc.core.IPacketListener
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.checksum
import com.angcyo.bluetooth.fsc.laserpacker.parse.QueryStateParser
import com.angcyo.bluetooth.fsc.laserpacker.writeBleLog
import com.angcyo.core.vmApp
import com.angcyo.http.tcp.Tcp
import com.angcyo.http.tcp.TcpConnectInfo
import com.angcyo.http.tcp.TcpState
import com.angcyo.library.L
import com.angcyo.library.ex._string
import com.angcyo.library.ex.copyTo
import com.angcyo.library.ex.isDebuggerConnected
import com.angcyo.library.ex.toHexInt
import com.angcyo.library.ex.toHexString
import com.angcyo.library.ex.toStr
import com.angcyo.lifecycle.isDestroy
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue
import java.io.ByteArrayOutputStream
import kotlin.math.roundToLong

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/25
 */
class WaitReceivePacket(
    //蓝牙操作api
    val api: FscBleApiModel,
    //发送数据的设备地址
    val address: String,
    //需要发送的指令
    val sendPacket: ByteArray,
    //自动发送数据
    val autoSend: Boolean,
    //用来验证的功能码, 保证收到的数据是相同指令发送过来的, null 则自动取第4个字节
    //需要先开启[checkFunc]
    val func: Byte?,
    //是否检查功能码
    val checkFunc: Boolean,
    //接收超时时间, 大于0生效, 毫秒
    val receiveTimeout: Long,
    //调用者线程回调, 通常在子线程
    val listener: IReceiveListener
) : IPacketListener {

    companion object {

        /**检查机器返回的数据是否有效
         * [bytes] 收到的数据
         * [dataHead] 返回的数据头
         * @return 校验通过返回对应的数据, 否则返回null
         * */
        fun checkReceiveFinish(bytes: ByteArray?, dataHead: String): ByteArray? {
            bytes ?: return null
            //数据头的位置 [AABB] 开始的位置
            var headStartIndex = -1
            var headHexString = ""

            val headDataSize = dataHead.length / 2
            val headByteArray = ByteArray(headDataSize)
            try {
                var index = 0

                //查找数据头
                for (byte in bytes) {
                    if (index + headDataSize >= bytes.size) {
                        break
                    }
                    bytes.copyTo(headByteArray, index, 0, headDataSize)
                    val hexString = headByteArray.toHexString(false)
                    if (hexString == dataHead) { //数据头
                        headHexString = hexString
                        headStartIndex = index
                        break
                    }
                    index++
                }

                //数据位, 占用的长度
                val dataBitLength = if (headHexString == LaserPeckerHelper.PACKET_HEAD_BIG) 4 else 1

                if (headStartIndex >= 0) {
                    //解析数据长度
                    val dataLengthArray = ByteArray(dataBitLength)
                    bytes.copyTo(dataLengthArray, headStartIndex + headDataSize)
                    val dataLength = dataLengthArray.toHexInt() //包含校验位的长度
                    val dataStartIndex = headStartIndex + headDataSize + dataBitLength
                    val receiveDataCount = bytes.size - dataStartIndex //收到的数据长度

                    if (receiveDataCount >= dataLength) {
                        //数据接收完成
                        val sumByteArray =
                            ByteArray(dataLength - LaserPeckerHelper.CHECK_SIZE) //去掉校验位后的数据
                        bytes.copyTo(sumByteArray, dataStartIndex)
                        val checkByteArray = ByteArray(LaserPeckerHelper.CHECK_SIZE) //校验位
                        val checkStartIndex =
                            dataStartIndex + dataLength - LaserPeckerHelper.CHECK_SIZE
                        bytes.copyTo(checkByteArray, checkStartIndex)

                        val sumString = sumByteArray.checksum(hasSpace = false)
                        val checkString = checkByteArray.toHexString(false)

                        if (sumString == checkString) {
                            //数据校验通过
                            return ByteArray(headDataSize + 1 + dataLength).apply {
                                bytes.copyTo(this, headStartIndex)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }
    }

    /**是否要一直监听数据*/
    var waitForever: Boolean = false

    val handle = Handler(Looper.getMainLooper())

    /**数据头*/
    var dataHead: String = LaserPeckerHelper.PACKET_HEAD

    /**是否被取消*/
    @Volatile
    var isCancel: Boolean = false
        set(value) {
            field = value
            if (!_isFinish) {
                end()
            }
        }

    //数据是否发送
    @Volatile
    var _isSend: Boolean = false

    //数据是否接收完成
    @Volatile
    var _isFinish: Boolean = false

    //头部字节大小
    val headSize: Int
        get() = dataHead.length / 2

    //超时处理
    val _timeOutRunnable = Runnable {
        if (!_isFinish) {
            UMEvent.APP_ERROR.umengEventValue {
                put(UMEvent.KEY_COMMAND_ERROR, "指令超时")
            }
            end()
            error(ReceiveTimeOutException())
        }
    }

    /**接收到的数据, 在发送第一包后赋值
     * [com.angcyo.bluetooth.fsc.WaitReceivePacket.onPacketSend]*/
    var receivePacket: ReceivePacket? = null

    private val wifiApi = vmApp<WifiApiModel>()

    private val wifiListener = object : Tcp.TcpListener {

        override fun onConnectStateChanged(tcp: Tcp, state: TcpState, info: TcpConnectInfo?) {
            if (state.state == Tcp.CONNECT_STATE_ERROR || state.state == Tcp.CONNECT_STATE_DISCONNECT) {
                end()
                error(IllegalArgumentException())
            }
        }

        override fun onSendStateChanged(
            tcp: Tcp,
            state: Int,
            sendAllSize: Long,
            error: Exception?
        ) {
            if (state == Tcp.SEND_STATE_ERROR) {
                end()
                error(error ?: IllegalArgumentException())
            } else if (state == Tcp.SEND_STATE_START) {
                onSendPacket(0)
            }
        }

        override fun onReceiveBytes(tcp: Tcp, bytes: ByteArray) {
            onReceive(bytes)
        }

        override fun onSendProgress(tcp: Tcp, allSize: Long, sendSize: Long, progress: Float) {
            onSendPacket(sendSize)
            onSendPacketProgress(progress.toInt())
        }
    }

    /**开始数据发送, 并等待*/
    fun start() {
        //监听数据, 可以用来单独监听回调
        if (WifiApiModel.useWifi()) {
            wifiApi.tcp.listeners.add(wifiListener)
        } else {
            api.addPacketListener(this)
        }
        //自动发送数据
        if (autoSend) {
            if (WifiApiModel.useWifi()) {
                wifiApi.initTcpConfig()
                wifiApi.tcp.send(sendPacket, receiveTimeout)
            } else {
                api.send(address, sendPacket)
            }
        }
        if (receiveTimeout > 0) {
            if (!isDebuggerConnected()) {
                //非调试下, 防止debug中断
                handle.postDelayed(_timeOutRunnable, receiveTimeout)
            }
        }
        _isSend = true
    }

    /**取消监听*/
    fun cancel() {
        if (isCancel) {
            return
        }
        isCancel = true
    }

    /**结束数据接收监听*/
    fun end() {
        _isFinish = true
        handle.removeCallbacks(_timeOutRunnable)
        api.removePacketListener(this)
        wifiApi.tcp.listeners.remove(wifiListener)
        if (isCancel) {
            error(ReceiveCancelException())
        }
    }

    /**错误返回*/
    fun error(e: Exception) {
        e.message?.writeBleLog(L.WARN)
        listener.onReceive(null, e)
    }

    @WorkerThread
    private fun _checkReceiveFinish() {
        val bytes = _receiveStream.toByteArray()

        //数据头的位置 [AABB] 开始的位置
        var headStartIndex = -1
        var headHexString = ""

        val headDataSize = headSize
        val headByteArray = ByteArray(headDataSize)
        try {
            var index = 0

            //查找数据头
            for (byte in bytes) {
                if (index + headDataSize >= bytes.size) {
                    break
                }
                bytes.copyTo(headByteArray, index, 0, headDataSize)
                val hexString = headByteArray.toHexString(false)
                if (hexString == dataHead) {
                    //数据头
                    if (checkFunc) {
                        //AABB Len Fun
                        val funcIndex = index + headDataSize + 1 //功能码的数据索引
                        if (bytes.size > funcIndex) {
                            //对比func功能码, 确保收到的指令数据和返回的数据是统一类型
                            val commandFunc = func ?: sendPacket[headDataSize + 1]//命令的功能码
                            val receiveFunc = bytes[funcIndex]//接收到的数据功能码

                            if (commandFunc == receiveFunc) {
                                headHexString = hexString
                                headStartIndex = index
                                break
                            }
                        } else {
                            //功能码数据还未接收到, 数据不完整,继续等待...
                        }
                    } else {
                        headHexString = hexString
                        headStartIndex = index
                        break
                    }
                }
                index++
            }

            //数据位, 占用的长度
            val dataBitLength = if (headHexString == LaserPeckerHelper.PACKET_HEAD_BIG) 4 else 1

            if (headStartIndex >= 0) {
                //解析数据长度
                val dataLengthArray = ByteArray(dataBitLength)
                bytes.copyTo(dataLengthArray, headStartIndex + headDataSize)
                val dataLength = dataLengthArray.toHexInt() //包含校验位的长度
                val dataStartIndex = headStartIndex + headDataSize + dataBitLength
                val receiveDataCount = bytes.size - dataStartIndex //收到的数据长度
                if (receiveDataCount >= dataLength) {
                    if (!waitForever) {
                        end()
                    }

                    //数据接收完成
                    val sumByteArray =
                        ByteArray(dataLength - LaserPeckerHelper.CHECK_SIZE) //去掉校验位后的有效数据
                    bytes.copyTo(sumByteArray, dataStartIndex)
                    val checkByteArray = ByteArray(LaserPeckerHelper.CHECK_SIZE) //校验位
                    val checkStartIndex =
                        dataStartIndex + dataLength - LaserPeckerHelper.CHECK_SIZE
                    bytes.copyTo(checkByteArray, checkStartIndex)

                    val sumString = sumByteArray.checksum(hasSpace = false) //收到数据计算出来的校验位
                    val checkString = checkByteArray.toHexString(false) //指令返回的校验位

                    receivePacket?.apply {
                        receiveDataLength = dataLength
                        receiveFinishTime = System.currentTimeMillis()
                        receivePacket = ByteArray(headDataSize + 1 + dataLength).apply {
                            bytes.copyTo(this, headStartIndex)
                        }
                    }
                    _main {
                        //切一下线程, 方便[isCancel]判断
                        if (!isCancel) {
                            if (sumString == checkString) {
                                //数据校验通过
                                listener.onReceive(receivePacket, null)
                            } else {
                                UMEvent.APP_ERROR.umengEventValue {
                                    put(UMEvent.KEY_COMMAND_ERROR, "数据校验失败")
                                }
                                val hex = receivePacket?.receivePacket?.toHexString()
                                error(ReceiveVerifyException("数据校验失败[$sumString:$checkString]:\n$hex"))
                            }
                        }
                    }
                } else {
                    //_isFinish = false
                    /*UMEvent.APP_ERROR.umengEventValue {
                        put(UMEvent.KEY_COMMAND_ERROR, "数据校验失败")
                    }
                    error(ReceiveVerifyException("数据长度校验失败[$receiveDataCount:${dataCount}]"))*/
                    "数据不完整,继续等待...[$receiveDataCount:${dataLength}]".writeBleLog(L.WARN)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _isFinish = true
            error(e)
        }
    }

    fun _main(action: () -> Unit) {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            action()
        } else {
            handle.post(action)
        }
    }

    //<editor-fold desc="packet">

    @WorkerThread
    override fun onPacketSend(
        packetProgress: DevicePacketProgress,
        address: String,
        strValue: String,
        data: ByteArray
    ) {
        super.onPacketSend(packetProgress, address, strValue, data)
        if (!_isFinish) {
            onSendPacket(packetProgress.sendBytesSize)
        }
    }

    @WorkerThread
    override fun onSendPacketProgress(
        packetProgress: DevicePacketProgress,
        address: String,
        percentage: Int,
        sendByte: ByteArray
    ) {
        super.onSendPacketProgress(packetProgress, address, percentage, sendByte)
        onSendPacketProgress(percentage)
    }

    //接收到的所有数据流
    val _receiveStream = ByteArrayOutputStream()

    override fun onPacketReceived(
        address: String,
        strValue: String,
        dataHexString: String,
        data: ByteArray
    ) {
        super.onPacketReceived(address, strValue, dataHexString, data)
        onReceive(data)
    }

    /**开始接收数据, 初始化工作*/
    private fun onSendPacket(sendBytesSize: Long) {
        if (receivePacket == null) {
            receivePacket = ReceivePacket().apply {
                this.address = this@WaitReceivePacket.address
                this.sendPacket = this@WaitReceivePacket.sendPacket
                this.sendStartTime = System.currentTimeMillis()
            }
        }
        receivePacket?.apply {
            sendPacketCount++
            val nowTime = System.currentTimeMillis()
            val duration = nowTime - sendStartTime
            val sendSize = sendBytesSize * 1f
            if (duration > 0) {
                //每毫秒发送的字节数量
                val speed = sendSize / duration
                //剩余需要发送的字节大小
                val remainingSize = sendPacket.size - sendBytesSize
                sendSpeed = speed * 1000
                remainingTime = (remainingSize / speed).roundToLong()
            } else {
                sendSpeed = sendSize
                remainingTime = 0
            }
            //listener.onPacketProgress(this) //need?
        }
    }

    /**接收数据*/
    private fun onReceive(data: ByteArray) {
        if (!_isFinish) {
            receivePacket?.apply {
                receivePacketCount++
                if (receiveStartTime < 0) {
                    receiveStartTime = System.currentTimeMillis()
                }
            }
            _receiveStream.write(data)
            _checkReceiveFinish()
        }
    }

    /**[percentage] 发送的进度[0~100]*/
    private fun onSendPacketProgress(percentage: Int) {
        if (!_isFinish) {
            receivePacket?.apply {
                sendPacketPercentage = percentage
                if (percentage >= 100) {
                    sendFinishTime = System.currentTimeMillis()
                }
                listener.onPacketProgress(this)
            }
        }
    }

    //</editor-fold desc="packet">
}

/**接收超时异常*/
class ReceiveTimeOutException(message: CharSequence = _string(R.string.device_busy_tip).writeBleLog()) :
    Exception(message.toStr())

/**接收取消异常*/
class ReceiveCancelException(message: CharSequence = _string(R.string.command_cancel_tip).writeBleLog()) :
    Exception(message.toStr())

/**接收校验异常*/
class ReceiveVerifyException(message: CharSequence) : Exception(message.toStr())

/**发包的进度*/
typealias ISendProgressAction = (bean: ReceivePacket) -> Unit

/**接收回调*/
typealias IReceiveBeanAction = (bean: ReceivePacket?, error: Exception?) -> Unit

@WorkerThread
interface IReceiveListener {

    /**发包的进度回调*/
    @WorkerThread
    fun onPacketProgress(bean: ReceivePacket) {
        bean.sendPacketPercentage //发包进度
    }

    /**接收完成的回调*/
    @WorkerThread
    fun onReceive(bean: ReceivePacket?, error: Exception?) {

    }
}

/**监听设备发来的数据包, 直到主动停止监听为止
 * [com.angcyo.bluetooth.fsc.WaitReceivePacket.isCancel] 取消监听
 * [receiveTimeout] 超时时长, 大于0生效
 * */
fun listenerReceivePacket(
    receiveTimeout: Long = 10 * 60 * 1_000,
    lifecycleOwner: LifecycleOwner? = null,
    progress: ISendProgressAction = {},
    action: (receivePacket: WaitReceivePacket, bean: ReceivePacket?, error: Exception?) -> Unit
): WaitReceivePacket {
    var waitReceivePacket: WaitReceivePacket? = null
    var receiveCallback: WaitReceivePacket? = null

    lifecycleOwner?.let {
        it.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                receiveCallback?.isCancel = true
            }
        })
    }

    waitReceivePacket = WaitReceivePacket(
        vmApp(),
        "",
        byteArrayOf(),
        false,
        null,
        false,
        receiveTimeout,
        object : IReceiveListener {
            override fun onPacketProgress(bean: ReceivePacket) {
                progress(bean)
            }

            override fun onReceive(bean: ReceivePacket?, error: Exception?) {
                //回调
                if (lifecycleOwner == null || !lifecycleOwner.lifecycle.isDestroy()) {
                    action(waitReceivePacket!!, bean, error)
                }
                receiveCallback = null
            }
        }).apply {
        start()
    }
    receiveCallback = waitReceivePacket
    return waitReceivePacket
}

/**监听设备进入空闲状态, 需要主动查询设备状态*/
fun listenerIdleMode(
    receiveTimeout: Long = 10 * 60 * 1_000,
    lifecycleOwner: LifecycleOwner? = null,
    action: (idle: Boolean, error: Exception?) -> Unit
): WaitReceivePacket {
    return listenerReceivePacket(receiveTimeout, lifecycleOwner) { receivePacket, bean, error ->
        try {
            val isIdle = bean?.parse<QueryStateParser>()?.mode == QueryStateParser.WORK_MODE_IDLE
            if (isIdle || (error != null && error !is ReceiveCancelException)) {
                receivePacket.isCancel = true
            }
            action(isIdle, error)
        } catch (e: Exception) {
            //no op
        }
    }
}