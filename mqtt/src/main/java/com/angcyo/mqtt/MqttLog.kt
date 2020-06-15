package com.angcyo.mqtt

import android.util.Log

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/12/09
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */

open class MqttLog {
    open fun i(data: String?) {
        Log.i("mqtt", data ?: "null")
    }

    open fun e(data: String?) {
        Log.e("mqtt", data ?: "null")
    }
}