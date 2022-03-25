package com.angcyo.bluetooth.fsc.laserpacker.bean

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.library.component.reader

/**
 * 查询安全码与用户帐号
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/25
 */
data class DeviceSafeCodeBean(
    //Safe_code：占4个字节Data0为高字节。
    var safeCode: Int = 0,
    //Admin：用户帐号，占用40个字节。
    var account: String? = null,
    var custom: Int = -1,
    var state: Int = 0
) {
    companion object {
        //解析数据
        fun parse(packet: ByteArray): DeviceSafeCodeBean? {
            return try {
                DeviceSafeCodeBean().apply {
                    packet.reader {
                        offset(LaserPeckerHelper.packetHeadSize)//偏移头部
                        offset(1)//偏移长度
                        offset(1)//偏移功能码
                        safeCode = readInt(4)
                        account = readString(40)
                        custom = readInt(1)
                        state = readInt(1)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
