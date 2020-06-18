package com.angcyo.mqtt

import com.angcyo.library.L

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/12/09
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */

open class MqttLog {
    open fun i(data: String?) {
        L.i("mqtt ", data ?: "null")
    }

    open fun e(data: String?) {
        L.e("mqtt ", data ?: "null")
    }
}