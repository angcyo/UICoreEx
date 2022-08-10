package com.angcyo.wifip2p.task

import androidx.annotation.WorkerThread
import java.net.Socket

/**
 * 接收监听
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/08/10
 */

@WorkerThread
interface ReceiveListener {

    /**当有客户端连接时回调*/
    fun onAccept(socket: Socket)

}