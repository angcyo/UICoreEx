package com.angcyo.objectbox.laser.pecker.entity

import androidx.annotation.Keep
import com.angcyo.library.ex.nowTime
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

/**
 * 设备连接记录表
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/29
 */

@Keep
@Entity
data class DeviceConnectEntity(
    @Id var entityId: Long = 0L,

    /**设备连接的时间, 毫秒*/
    var connectTime: Long = nowTime(),

    /**设备的地址*/
    var deviceAddress: String? = null,

    /**设备的蓝牙名称*/
    var deviceName: String? = null,
)
