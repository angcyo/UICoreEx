package com.angcyo.tim.model

import com.angcyo.core.lifecycle.LifecycleViewModel
import com.angcyo.viewmodel.vmDataNull

/**
 * 聊天数据共享model
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/12
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class ChatModel : LifecycleViewModel() {

    /**自己的头像全路径url*/
    var selfFaceUrlData = vmDataNull<String>()

    /**无头像时, 需要绘制的昵称文本*/
    var selfShowNameData = vmDataNull<String>()

}