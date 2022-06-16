package com.angcyo.bluetooth.fsc.laserpacker.parse

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.checksum
import com.angcyo.bluetooth.fsc.laserpacker.command.ICommand
import com.angcyo.bluetooth.fsc.laserpacker.command.IPacketParser
import com.angcyo.library.component.HawkPropertyValue
import com.angcyo.library.component.reader
import com.angcyo.library.ex.padHexString
import com.angcyo.library.ex.removeAll
import com.angcyo.library.ex.toHexString

/**
 * 设备设置状态信息
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/25
 */
data class QuerySettingParser(
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
    var state: Int = 0,

    //Safe_code：为安全码，占用了4个字节，Data1为高字节。
    var safeCode: Int = 0,
    //Admin：为用户帐号占用40
    var admin: Int = 0,
) : IPacketParser<QuerySettingParser>, ICommand {

    companion object {

        /**自动连接设备状态存储*/
        var AUTO_CONNECT_DEVICE: Boolean by HawkPropertyValue<Any, Boolean>(false)

        /**第三轴模式状态存储*/
        var Z_MODEL: Int by HawkPropertyValue<Any, Int>(-1)
    }

    //解析数据
    override fun parse(packet: ByteArray): QuerySettingParser? {
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

    //转换成指令
    override fun toHexCommandString(): String {
        var dataLength = 0x0E  //数据长度
        val func = "06" //功能码
        val data = buildString {
            append(func)
            //当state为1时表示功能设置
            //当state为2时表示安全码与用户设置
            append(state.toHexString())

            if (state == 0x02) {
                //todo 安全码与用户设置
                dataLength = 0x27
            } else {
                dataLength = 0x0E

                //1为自由模式，为0时安全模式。
                append(free.toHexString())
                //1为打开提示音，0为关，默认为0。
                append(buzzer.toHexString())
                //1为连续预览，0为单次预览，默认为0。
                append(view.toHexString())
                //0为G code 预览开，1为G code边界预览，默认为0。
                append(gcodeView.toHexString())
                //custom
                append(custom.toHexString())
                // 0为未使用Z轴，1为使用Z轴。
                append(zFlag.toHexString())
                //第三轴打印方式：0为打直板，1为打印圆柱。
                append(zDir.toHexString())
                //触摸按键启动预览使能位，1为开启，0为关闭。
                append(keyView.toHexString())
                //红光测距指示激光。（L3测距功能）。
                append(irDst.toHexString())
                //一键打印使能位，1为开，0为关。
                append(keyPrint.toHexString())
            }
        }.padHexString(dataLength - LaserPeckerHelper.CHECK_SIZE)
        val check = data.checksum() //“功能码”和“数据内容”在内的校验和
        val cmd = "${LaserPeckerHelper.PACKET_HEAD} ${dataLength.toHexString()} $data $check"
        return cmd
    }

    override fun toCommandLogString(): String = buildString {
        append(toHexCommandString().removeAll())
        append(" 功能设置:${this@QuerySettingParser}")
    }

    /**当state为1时表示功能设置*/
    fun functionSetting() {
        state = 0x01
    }

    /**当state为2时表示安全码与用户设置*/
    fun safeCodeSetting() {
        state = 0x02
    }

}