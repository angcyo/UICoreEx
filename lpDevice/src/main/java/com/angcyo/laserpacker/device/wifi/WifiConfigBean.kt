package com.angcyo.laserpacker.device.wifi

import android.os.Parcelable
import com.clj.fastble.data.BleDevice
import kotlinx.parcelize.Parcelize

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/07/31
 */
@Parcelize
data class WifiConfigBean(
    val device: BleDevice,
    val name: String,
    val password: String
) : Parcelable
