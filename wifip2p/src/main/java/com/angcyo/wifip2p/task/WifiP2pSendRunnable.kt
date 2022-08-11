package com.angcyo.wifip2p.task

import com.angcyo.core.vmApp
import com.angcyo.library.L
import com.angcyo.library.component.Speed
import com.angcyo.library.ex.fileSizeString
import com.angcyo.wifip2p.WifiP2pModel
import com.angcyo.wifip2p.data.ProgressInfo
import com.angcyo.wifip2p.task.WifiP2pProgressListener.Companion.FINISH_CANCEL
import com.angcyo.wifip2p.task.WifiP2pProgressListener.Companion.FINISH_ERROR
import com.angcyo.wifip2p.task.WifiP2pProgressListener.Companion.FINISH_SUCCESS
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket

/**
 * 发送数据的[Runnable]
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/08/09
 */
class WifiP2pSendRunnable(
    /**设备的mac地址*/
    val deviceAddress: String?,
    /**Ip地址*/
    val serviceAddress: String,
    /**服务端口*/
    val servicePort: Int,
    /**数据流*/
    val dataStream: SendDataStream
) : Runnable {

    /**是否取消*/
    var isCancel: Boolean = false

    var wifiP2pProgressListener: WifiP2pProgressListener? = null

    /**速率计算*/
    val speed = Speed()

    val wifiP2pModel = vmApp<WifiP2pModel>()

    override fun run() {
        val socket = Socket()
        try {
            socket.connect(InetSocketAddress(serviceAddress.replace("/", ""), servicePort))
            socket.receiveBufferSize = WifiP2pReceiveRunnable.BUFFER_SIZE
            socket.sendBufferSize = WifiP2pReceiveRunnable.BUFFER_SIZE

            //If this code is reached, a client has connected and transferred data.
            val outputStream = BufferedOutputStream(socket.getOutputStream()) //
            val inputStream = dataStream.getInputStream()
            val size = dataStream.getLength()//数据包总大小
            var total: Long = 0 //已发送总大小
            val buffer = ByteArray(WifiP2pReceiveRunnable.BUFFER_SIZE) //buffer
            var len: Int

            wifiP2pModel.progressData.postValue(
                ProgressInfo(deviceAddress, 0, 0, speed, null)
            )

            L.d("Connected, transferring data:${size.fileSizeString()}")
            while (inputStream.read(buffer).also { len = it } != -1 && !isCancel) {
                outputStream.write(buffer, 0, len)
                total += len.toLong()
                if (speed.update(len.toLong(), size)) {
                    //控制回调速度
                    val progress = (total * 100 / size).toInt()
                    L.i("发送进度：${progress}% ${speed.speed.fileSizeString()}/s")
                    wifiP2pProgressListener?.onProgress(progress, speed, size)

                    wifiP2pModel.progressData.postValue(
                        ProgressInfo(deviceAddress, 0, progress, speed, null)
                    )
                }
            }

            inputStream.close()
            outputStream.flush()
            outputStream.close()
            L.d("Successfully sent data.")

            if (!isCancel) {
                wifiP2pProgressListener?.onProgress(100, speed, size)//this
                wifiP2pProgressListener?.onFinish(FINISH_SUCCESS, null)

                wifiP2pModel.progressData.postValue(
                    ProgressInfo(deviceAddress, FINISH_SUCCESS, 100, speed, null)
                )
            }
        } catch (ex: Exception) {
            L.d("An error occurred while sending data to a device.")
            ex.printStackTrace()
            wifiP2pProgressListener?.onFinish(FINISH_ERROR, ex)

            wifiP2pModel.progressData.postValue(
                ProgressInfo(deviceAddress, FINISH_ERROR, -1, speed, ex)
            )
        } finally {
            try {
                socket.close()
            } catch (ex: Exception) {
                L.e("Failed to close data socket.")
            }
        }
    }

    /**取消数据发送*/
    fun cancel() {
        isCancel = true
        wifiP2pProgressListener?.onFinish(FINISH_CANCEL, null)

        wifiP2pModel.progressData.postValue(
            ProgressInfo(deviceAddress, FINISH_CANCEL, -1, speed, null)
        )
    }

    /**开始发送数据*/
    fun start() {
        val thread = Thread(this, "WifiP2pSend-$servicePort")
        thread.start()
    }
}

/**需要发送的数据流*/
interface SendDataStream {
    /**获取数据流的长度*/
    fun getLength(): Long = -1

    fun getInputStream(): InputStream
}

/**发送[ByteArray]*/
class ByteSendDataStream(val bytes: ByteArray) : SendDataStream {

    override fun getLength(): Long = bytes.size.toLong()

    override fun getInputStream(): InputStream = ByteArrayInputStream(bytes)
}

/**发送[File]*/
class FileSendDataStream(val file: File) : SendDataStream {

    override fun getLength(): Long = file.length()

    override fun getInputStream(): InputStream = FileInputStream(file)
}