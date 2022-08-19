package com.angcyo.websocket

import androidx.annotation.AnyThread
import androidx.lifecycle.ViewModel
import com.angcyo.viewmodel.notify
import com.angcyo.viewmodel.updateValue
import com.angcyo.viewmodel.vmData
import com.angcyo.viewmodel.vmDataOnce
import com.angcyo.websocket.data.WSClientInfo
import com.angcyo.websocket.data.WSMessageInfo
import org.java_websocket.WebSocket

/**
 * [WebSocket]数据模式
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/08/19
 */
class WSServerModel : ViewModel() {

    //region ---client---

    /**所有客户端的信息, 包括服务端信息*/
    val clientListData = vmData<MutableList<WSClientInfo>>(mutableListOf())

    /**客户端连接的通知*/
    val clientConnectData = vmDataOnce<WSClientInfo>()

    /**服务端停止的通知*/
    val stopServerData = vmDataOnce<WSClientInfo>()

    /**添加一个连接成功的客户端信息*/
    @AnyThread
    fun addClient(info: WSClientInfo) {
        clientListData.value?.add(info)
        clientListData.notify()

        //通知连接
        clientConnectData.updateValue(info)
    }

    fun removeClient(client: WebSocket) {
        clientListData.value?.removeIf { it.client == client }
        clientListData.notify()
    }

    /**服务停止通知*/
    fun stopServer(server: WSServer) {
        val clientInfo = clientListData.value?.find { it.server == server }
        clientInfo?.let {
            stopServerData.updateValue(it)
        }
    }

    //endregion ---client---

    //region ---message---

    /**收到的消息通知*/
    val clientMessageData = vmDataOnce<WSMessageInfo>()

    /**收到消息的处理*/
    fun onMessage(client: WebSocket, message: String?) {
        val clientInfo = clientListData.value?.find { it.client == client }
        clientInfo?.let {
            val messageInfo = WSMessageInfo(it, message)
            clientMessageData.updateValue(messageInfo)
        }
    }

    //endregion ---message---

}