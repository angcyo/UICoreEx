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

    /**是否是自动连接*/
    var isAutoConnect: Boolean = false,

    /**设备连接的时间, 毫秒*/
    var connectTime: Long = nowTime(),

    /**连接的设备类型,
     * 蓝牙/wifi/http
     * [String.deviceType]*/
    var deviceType: Int = 0,

    /**当时连接设备时的端口*/
    var wifiPort: Int = 0,

    /**设备的地址,
     * wifi连接,此地址就是ip地址
     * http连接, 则是http域名地址*/
    var deviceAddress: String? = null,

    /**设备的蓝牙名称*/
    var deviceName: String? = null,

    /**主动断开的时间, 用来判断取消自动连接的时间*/
    var disconnectTime: Long? = null,

    /**是否是wifi连接类型*/
    var isWifiConnect: Boolean = false,
)
