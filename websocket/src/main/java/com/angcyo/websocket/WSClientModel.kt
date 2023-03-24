package com.angcyo.websocket

import androidx.lifecycle.ViewModel
import com.angcyo.viewmodel.vmDataNull
import com.angcyo.viewmodel.vmDataOnce
import com.angcyo.websocket.data.WSServerMessageInfo
import com.angcyo.websocket.data.WSServerStateInfo

/**
 * [WSClient]数据模式
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2023/03/24
 * Copyright (c) 2020 angcyo. All rights reserved.
 */
class WSClientModel : ViewModel() {

    /**与服务端的连接状态与通知*/
    val serverStateData = vmDataNull<WSServerStateInfo>()

    //region ---message---

    /**收到服务端的消息通知*/
    val serverMessageData = vmDataOnce<WSServerMessageInfo>()

    //endregion ---message---

}