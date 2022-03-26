package com.angcyo.bluetooth.fsc.laserpacker.bean

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.IPacketParse
import com.angcyo.library.component.reader

/**
 * 设备版本信息
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/25
 */
data class DeviceVersionBean(
    //软件版本号占两个字节，Data0为高字节，Data1为低字节。
    //L1基础版本号为100如有更新版本号自加1，版本表示V1.0.0
    //L1P基础版本号为200如上。
    //L2基础版本号为300。
    //300 - 399 正式生产版本
    //3000 - 3199为内部测试版本
    //3200 - 3299 PC端调试版
    //3300 - 3399 为用户测试版
    var softwareVersion: Int = 0,
    //硬件版号前3字节表示生产日期，最后字节为版本号。
    //L1硬件版本19050800 ，190508 表示版本生成日期 00表示V0.0版
    //L1P硬件版本19111110 ，191111表示版本生成日期 10表示V1.0版
    var hardwareVersion: Int = 0,
    var custom: Int = -1,
    var state: Int = 0
) : IPacketParse<DeviceVersionBean> {
    //解析数据
    override fun parse(packet: ByteArray): DeviceVersionBean? {
        return try {
            packet.reader {
                offset(LaserPeckerHelper.packetHeadSize)//偏移头部
                offset(1)//偏移长度
                offset(1)//偏移功能码
                softwareVersion = readInt(2)
                hardwareVersion = readInt(4)
                custom = readInt(1)
                state = readInt(1)
            }
            this
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
