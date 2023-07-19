package com.angcyo.bluetooth.fsc.laserpacker.parse

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.checksum
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.command.BaseCommand
import com.angcyo.bluetooth.fsc.laserpacker.command.IPacketParser
import com.angcyo.bluetooth.fsc.laserpacker.command.QueryCmd
import com.angcyo.core.vmApp
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
    var func: Byte = 0, //功能码
    //1为自由模式开，0关，默认为0。
    var free: Int = 0,
    //工作提示音,1为打开提示音，0为关，默认为0。
    var buzzer: Int = 0,
    //1为连续预览，0为单次预览，默认为0。
    var view: Int = 0,
    //0为G code 预览开，1为G code边界预览，默认为0。
    var gcodeView: Int = GCODE_PREVIEW,
    //1为安全状态，0为非安全状态。
    var safe: Int = 1,
    var custom: Int = -1,
    //0为Z轴关闭，1为Z轴打开。
    var zFlag: Int = 0,
    //0为打直板，1为打印圆柱。
    var zDir: Int = 0,
    //0为旋转轴关闭，1为打开。
    var rFlag: Int = 0,
    //0为滑台关闭，1为打开。
    var sFlag: Int = 0,
    //旋转轴方向控制，0为反转，1为正转。
    var dir: Int = 0,
    //滑台多文件雕刻模式，0为关，1为开。
    var sRep: Int = 0,
    //gcode激光功率值选择位，0从app输入值取值，1从gcode指令中取值。
    var gcodePower: Int = 0,
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
    //CAR_flag：C1小车, 移动平台模式开关，0为关，1为开。
    var carFlag: Int = 0,
    //2023-2-28 L2 雕刻方向设置位，0为默认方向（从机尾向机头雕刻），1则反之。
    var printDir: Int = 0,
    //2023-7-19 忽略LX1的温度传感器，0为不忽略，1为忽略。 @ignoreTempSensor#b=true
    var ignoreTempSensor: Int = 0,
) : BaseCommand(), IPacketParser<QuerySettingParser> {

    companion object {

        /**第三轴模式状态存储
         * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel.updateDeviceSettingState] 在此会初始化*/
        //var Z_MODEL: Int by HawkPropertyValue<Any, Int>(-1)

        /**[Z_MODEL] 字符串数据*/
        var Z_MODEL_STR: String by HawkPropertyValue<Any, String>("")

        //平板, 对应string中的资源
        const val Z_MODEL_FLAT = "device_z_model_flat"

        //小车, 对应string中的资源
        const val Z_MODEL_CAR = "device_z_model_car"

        //圆柱, 对应string中的资源
        const val Z_MODEL_CYLINDER = "device_z_model_cylinder"

        //---

        /**扩展设备 z轴*/
        const val EX_Z = "z"

        /**扩展设备 r轴*/
        const val EX_R = "r"

        /**扩展设备 滑台*/
        const val EX_S = "s"

        /**扩展设备 移动平台*/
        const val EX_CAR = "car"

        /**GCode向量预览*/
        const val GCODE_PREVIEW = 0

        /**GCode边界预览*/
        const val GCODE_RECT_PREVIEW = 1
    }

    //解析数据
    override fun parse(packet: ByteArray): QuerySettingParser? {
        return try {
            packet.reader {
                keepLastSize = LaserPeckerHelper.CHECK_SIZE
                offset(LaserPeckerHelper.packetHeadSize)//偏移头部
                offset(1)//偏移长度
                func = readByte()//偏移功能码

                if (func != QueryCmd.workState.commandFunc()) {
                    throw IllegalStateException("非查询指令!")
                }

                val laserPeckerModel = vmApp<LaserPeckerModel>()
                val productInfo = laserPeckerModel.productInfoData.value

                free = readInt(1, 0)
                buzzer = readInt(1, 0)
                view = readInt(1, 0)
                gcodeView = readInt(1, GCODE_PREVIEW)
                safe = readInt(1, safe)
                custom = readInt(1, custom)
                zFlag = if (productInfo?.isSupportZ() == true) readInt(1, 0) else 0
                zDir = if (productInfo?.isSupportZ() == true) readInt(1, 0) else 0
                keyView = readInt(1, 0)
                irDst = readInt(1, 0)
                keyPrint = readInt(1, 0)
                rFlag = if (productInfo?.isSupportR() == true) readInt(1, 0) else 0
                sFlag = if (productInfo?.isSupportS() == true) readInt(1, 0) else 0
                dir = readInt(1, 0)
                gcodePower = readInt(1, 0)
                sRep = if (productInfo?.isSupportS() == true) readInt(1, 0) else 0
                carFlag = if (productInfo?.isCSeries() == true) readInt(1, 0) else 0
                printDir = readInt(1, 0)
                ignoreTempSensor = readInt(1, 0)
                //---最后位
                state = readInt(1, 0)
            }
            this
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    //功能码
    override fun commandFunc(): Byte = 0x06

    /**超时时长, 毫秒*/
    var receiveTimeout: Long? = null

    override fun getReceiveTimeout(): Long {
        return receiveTimeout ?: super.getReceiveTimeout()
    }

    //转换成指令
    override fun toHexCommandString(): String {
        var dataLength: Int  //数据长度, 指由功能码开始到较验和一共包含的字节数
        val data = buildString {
            append(commandFunc().toHexString())

            //当state为1时表示功能设置
            //当state为2时表示安全码与用户设置
            append(state.toHexString())

            if (state == 0x02) {
                //todo 安全码与用户设置
                dataLength = 0x27
            } else {
                dataLength = 0x16 //数据长度

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
                //2022-7-28
                append(rFlag.toHexString())
                append(sFlag.toHexString())
                append(dir.toHexString())
                append(gcodePower.toHexString())
                append(sRep.toHexString())
                append(carFlag.toHexString())
                //2023-2-28
                append(printDir.toHexString())
                //2023-7-19
                append(ignoreTempSensor.toHexString())
                //dataLength 数据长度需要手动+1
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

    /**清理设备标识*/
    fun clearFlag() {
        //互斥标识
        gcodeView = GCODE_RECT_PREVIEW
        zFlag = 0
        rFlag = 0
        sFlag = 0
        sRep = 0
        carFlag = 0
    }
}
