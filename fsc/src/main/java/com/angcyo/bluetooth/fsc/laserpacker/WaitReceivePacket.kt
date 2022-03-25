package com.angcyo.bluetooth.fsc.laserpacker

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.angcyo.bluetooth.fsc.FscBleApiModel
import com.angcyo.bluetooth.fsc.IPacketListener
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.checksum
import com.angcyo.library.ex.copyTo
import com.angcyo.library.ex.toHexByteArray
import com.angcyo.library.ex.toHexInt
import com.angcyo.library.ex.toHexString
import java.io.ByteArrayOutputStream

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
    //接收超时时间, 毫秒
    val receiveTimeOut: Long,
    //调用者线程回调, 通常在子线程
    val listener: IReceiveListener
) : IPacketListener {

    val handle = Handler(Looper.getMainLooper())

    var _isSend: Boolean = false
    var _isFinish: Boolean = false

    //头部字节大小
    val headSize = LaserPeckerHelper.PACKET_HEAD.toHexByteArray().size

    val _timeOutRunnable = Runnable {
        if (!_isFinish) {
            end()
            listener.onReceive(null, ReceiveTimeOutException("已超过接收时长:$receiveTimeOut ms"))
        }
    }

    //接收到的数据
    var receivePacketBean: ReceivePacketBean? = null

    fun start() {
        api.addPacketListener(this)
        if (autoSend) {
            api.send(address, sendPacket)
        }
        if (receiveTimeOut > 0) {
            handle.postDelayed(_timeOutRunnable, receiveTimeOut)
        }
        _isSend = true
    }

    fun end() {
        _isFinish = true
        handle.removeCallbacks(_timeOutRunnable)
        api.removePacketListener(this)
    }

    fun _checkReceiveFinish() {
        val bytes = _receiveStream.toByteArray()

        //数据头的位置 [AABB] 开始的位置
        var headStartIndex = -1

        val headByteArray = ByteArray(headSize)
        try {
            var index = 0

            //查找数据头
            for (byte in bytes) {
                bytes.copyTo(headByteArray, index, 0, headSize)
                if (headByteArray.toHexString(false) == LaserPeckerHelper.PACKET_HEAD) {
                    //数据头
                    headStartIndex = index
                    break
                }
                index++
            }

            if (headStartIndex >= 0) {
                //解析数据长度
                val dataCount = bytes[headStartIndex + headSize].toHexInt() //包含校验位的长度
                val dataStartIndex = headStartIndex + headSize + 1
                val receiveDataCount = bytes.size - dataStartIndex//收到的数据长度
                if (receiveDataCount >= dataCount) {
                    end()

                    //数据接收完成
                    val sumByteArray =
                        ByteArray(dataCount - LaserPeckerHelper.CHECK_SIZE) //去掉校验位后的数据
                    bytes.copyTo(sumByteArray, dataStartIndex)
                    val checkByteArray = ByteArray(LaserPeckerHelper.CHECK_SIZE) //校验位
                    val checkStartIndex = dataStartIndex + dataCount - LaserPeckerHelper.CHECK_SIZE
                    bytes.copyTo(checkByteArray, checkStartIndex)

                    val sumString = sumByteArray.checksum(hasSpace = false)
                    val checkString = checkByteArray.toHexString(false)

                    receivePacketBean?.apply {
                        receiveDataLength = dataCount
                        receiveFinishTime = SystemClock.elapsedRealtime()
                        receivePacket = ByteArray(headSize + 1 + dataCount).apply {
                            bytes.copyTo(this, headStartIndex)
                        }
                    }
                    if (sumString == checkString) {
                        //数据校验通过
                        listener.onReceive(receivePacketBean, null)
                    } else {
                        listener.onReceive(
                            null,
                            ReceiveVerifyException("数据校验失败: 计算值:$sumString 比较值:$checkString")
                        )
                    }
                } else {
                    _isFinish = false
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
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

    override fun onPacketSend(address: String, strValue: String, data: ByteArray) {
        super.onPacketSend(address, strValue, data)
        if (!_isFinish) {
            if (receivePacketBean == null) {
                receivePacketBean = ReceivePacketBean().apply {
                    this.address = address
                    this.sendPacket = this@WaitReceivePacket.sendPacket
                    this.sendStartTime = SystemClock.elapsedRealtime()
                }
            }
            receivePacketBean?.apply {
                sendPacketCount++
            }
        }
    }

    override fun onSendPacketProgress(address: String, percentage: Int, sendByte: ByteArray) {
        super.onSendPacketProgress(address, percentage, sendByte)
        if (!_isFinish) {
            receivePacketBean?.apply {
                sendPacketPercentage = percentage
                if (percentage >= 100) {
                    sendFinishTime = SystemClock.elapsedRealtime()
                }

                listener.onPacketProgress(this)
            }
        }
    }

    val _receiveStream = ByteArrayOutputStream()

    override fun onPacketReceived(
        address: String,
        strValue: String,
        dataHexString: String,
        data: ByteArray
    ) {
        super.onPacketReceived(address, strValue, dataHexString, data)
        if (!_isFinish) {
            receivePacketBean?.apply {
                receivePacketCount++
                if (receiveStartTime < 0) {
                    receiveStartTime = SystemClock.elapsedRealtime()
                }
            }
            _receiveStream.write(data)
            _checkReceiveFinish()
        }
    }

    //</editor-fold desc="packet">
}

/**接收超时异常*/
class ReceiveTimeOutException(message: String) : Exception(message)

/**接收校验异常*/
class ReceiveVerifyException(message: String) : Exception(message)

typealias ISendProgressAction = (bean: ReceivePacketBean) -> Unit
typealias IReceiveBeanAction = (bean: ReceivePacketBean?, error: Exception?) -> Unit

interface IReceiveListener {

    /**发包的进度回调*/
    fun onPacketProgress(bean: ReceivePacketBean) {
        bean.sendPacketPercentage //发包进度
    }

    /**接收完成的回调*/
    fun onReceive(bean: ReceivePacketBean?, error: Exception?) {

    }
}