package com.angcyo.acc2.app.model

import com.angcyo.core.lifecycle.LifecycleViewModel
import com.angcyo.viewmodel.vmDataOnce

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/12/23
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class AccServerModel : LifecycleViewModel() {

    /**收到了新的客户端发过来的消息, 不存储*/
    val newAcceptData = vmDataOnce<String>(null)

}