package com.angcyo.bluetooth.fsc.laserpacker.parse

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.IPacketParser
import com.angcyo.library.component.reader

/**
 * 设备设置状态信息
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/25
 */
data class DeviceSettingParser(
    //1为自由模式开，0关，默认为0。
    var free: Int = 0,
    //工作提示音,1为打开提示音，0为关，默认为0。
    var buzzer: Int = 0,
    //1为连续预览，0为单次预览，默认为0。
    var view: Int = 0,
    //0为G code 预览开，1为G code边界预览，默认为0。
    var gcodeView: Int = 0,
    //1为安全状态，0为非安全状态。
    var safe: Int = 1,
    var custom: Int = -1,
    //0为Z轴关闭，1为Z轴打开。
    var zFlag: Int = 0,
    //0为打直板，1为打印圆柱。
    var zDir: Int = 0,
    //触摸按键启动预览使能位，1为开启，0为关闭。
    var keyView: Int = 0,
    //测距红外指示光开关，0为关，1为开，上电默认关。(L3设置测距红光开关)
    var irDst: Int = 0,
    //一键打印使能开关，1为开，0为关。
    var keyPrint: Int = 0,
    var state: Int = 0
) : IPacketParser<DeviceSettingParser> {
    //解析数据
    override fun parse(packet: ByteArray): DeviceSettingParser? {
        return try {
            packet.reader {
                offset(LaserPeckerHelper.packetHeadSize)//偏移头部
                offset(1)//偏移长度
                offset(1)//偏移功能码
                free = readInt(1)
                buzzer = readInt(1)
                view = readInt(1)
                gcodeView = readInt(1)
                safe = readInt(1)
                custom = readInt(1)
                zFlag = readInt(1)
                zDir = readInt(1)
                keyView = readInt(1)
                irDst = readInt(1)
                keyPrint = readInt(1)
                state = readInt(1)
            }
            this
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}