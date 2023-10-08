package com.angcyo.bluetooth.fsc.laserpacker.parse

import com.angcyo.bluetooth.fsc.R
import com.angcyo.library.ex._string

/**
 * 无设备连接异常
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/25
 */
class NoDeviceException(message: String? = _string(R.string.blue_no_device_connected)) :
    Exception(message)