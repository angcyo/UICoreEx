package com.angcyo.wifip2p.task

import com.angcyo.library.L
import com.angcyo.library.component.Speed
import com.angcyo.library.ex.fileSizeString
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

    override fun onAccept(socket: Socket) {
        this.socket = socket
        val thread = Thread(this, "ByteReceive")
        thread.start()
    }

    override fun run() {
        try {
            //If this code is reached, a client has connected and transferred data.
            L.v("A device is sending data...")
            val bufferedInputStream = BufferedInputStream(socket.getInputStream())

            //http://stackoverflow.com/a/35446009/4411645
            val result = ByteArrayOutputStream()
            val buffer = ByteArray(WifiP2pReceiveRunnable.BUFFER_SIZE)
            var length: Int
            while (bufferedInputStream.read(buffer).also { length = it } != -1) {
                speed.update(length.toLong())
                result.write(buffer, 0, length)
                L.i("接收速率：${speed.speed.fileSizeString()}/s")
            }
            bufferedInputStream.close()
            L.d("Successfully received data: ${result.size().toLong().fileSizeString()}\n")
        } catch (ex: Exception) {
            L.e("An error occurred while trying to receive data.")
            ex.printStackTrace()
        } finally {
            try {
                socket.close()
            } catch (ex: Exception) {
                L.e("Failed to close data socket.")
            }
        }
    }
}