package com.angcyo.wifip2p.task

import com.angcyo.core.vmApp
import com.angcyo.library.L
import com.angcyo.library.component.Speed
import com.angcyo.library.ex.fileSizeString
import com.angcyo.wifip2p.WifiP2pModel
import com.angcyo.wifip2p.data.ProgressInfo
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.net.Socket

/**
 * 字节数据接收
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/08/10
 */
/**接收[ByteArray]*/
class ByteReceiveListener : ReceiveListener, Runnable {

    lateinit var socket: Socket

    /**速率计算*/
    val speed = Speed()

    /**进度回调*/
    var wifiP2pProgressListener: WifiP2pProgressListener? = null

    val wifiP2pModel = vmApp<WifiP2pModel>()

    override fun onAccept(socket: Socket) {
        this.socket = socket
        val thread = Thread(this, "ByteReceive")
        thread.start()
    }

    override fun run() {
        try {
            //If this code is reached, a client has connected and transferred data.
            L.v("A device is sending data...")
            val inputStream = BufferedInputStream(socket.getInputStream())

            //http://stackoverflow.com/a/35446009/4411645
            val outputStream = ByteArrayOutputStream()
            val buffer = ByteArray(WifiP2pReceiveRunnable.BUFFER_SIZE)
            var length: Int

            speed.reset()
            wifiP2pModel.progressData.postValue(
                ProgressInfo(null, 0, 0, speed, null)
            )

            while (inputStream.read(buffer).also { length = it } != -1 && !isCancel) {
                outputStream.write(buffer, 0, length)
                if (speed.update(length.toLong(), -1)) {
                    L.i("接收速率：${speed.speed.fileSizeString()}/s")
                    wifiP2pProgressListener?.onProgress(-1, speed, speed.total)

                    wifiP2pModel.progressData.postValue(
                        ProgressInfo(null, 0, -1, speed, null)
                    )
                }
            }
            inputStream.close()
            outputStream.flush()
            outputStream.close()
            L.d("Successfully received data: ${outputStream.size().toLong().fileSizeString()}")

            if (!isCancel) {
                wifiP2pProgressListener?.onProgress(100, speed, speed.total)//this
                wifiP2pProgressListener?.onFinish(WifiP2pProgressListener.FINISH_SUCCESS, null)

                wifiP2pModel.progressData.postValue(
                    ProgressInfo(
                        null,
                        WifiP2pProgressListener.FINISH_SUCCESS, 100, speed, null
                    )
                )
            }
        } catch (ex: Exception) {
            L.e("An error occurred while trying to receive data.")
            ex.printStackTrace()
            wifiP2pProgressListener?.onFinish(WifiP2pProgressListener.FINISH_ERROR, ex)

            wifiP2pModel.progressData.postValue(
                ProgressInfo(null, WifiP2pProgressListener.FINISH_ERROR, -1, speed, ex)
            )
        } finally {
            try {
                socket.close()
            } catch (ex: Exception) {
                L.e("Failed to close data socket.")
            }
        }
    }

    /**是否取消*/
    var isCancel: Boolean = false

    /**取消数据接收*/
    fun cancel() {
        isCancel = true
        wifiP2pProgressListener?.onFinish(WifiP2pProgressListener.FINISH_CANCEL, null)

        wifiP2pModel.progressData.postValue(
            ProgressInfo(null, WifiP2pProgressListener.FINISH_CANCEL, -1, speed, null)
        )
    }

}