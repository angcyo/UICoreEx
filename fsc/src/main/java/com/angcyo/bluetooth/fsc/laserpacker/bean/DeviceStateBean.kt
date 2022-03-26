package com.angcyo.bluetooth.fsc.laserpacker.bean

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.IPacketParse
import com.angcyo.library.component.reader

/**
 * 蓝牙设备状态数据结构
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/25
 */
data class DeviceStateBean(
    //设备当前工作模式
    //L1当前工作模式，0x01为打印模式，0x02为打印预览模式，0x04为调焦模式，
    //0x05为文件下载模式，0x06为空闲模式，0x07为关机状态，0x08为设置模式，0x09为下载模式。
    var mode: Int = 0x06,
    //当前模式下的工作状态, 暂停, 255:结束, 1:预览图片 2:预览范围 6:支架
    var workState: Int = 0,
    //打印进度百分比[0-100]
    var rate: Int = 0,
    //激光强度[1-100]
    var laser: Int = 0,
    //激光打印速度[1-100]
    var speed: Int = 0,
    //错误状态
    //0表示无错误状态
    //1表示不处于安全状态，且自由模式没开。
    //2打印超过边界报警
    //3激光工作温度报警
    //4打印过程中移动设备报警
    //5打印过程中遮挡激光报警
    //6打印数据错误
    //7文件编号查询错误
    //8陀螺仪自检错误
    //9 flash自检错误
    var error: Int = 0,
    //发送查询指令时传递的state
    //0时表示查询工作状态
    //1时表示查询文件列表
    //2时表示查询设置状态
    //3时表示查询版本
    //4时表示查询安全码与用户帐号
    var state: Int = 0,
    //当前正在打印的文件编号(文本编号对应本地存储的文件信息)
    var name: Int = 0,
    //工作温度, 精度1度
    var temp: Int = 0,
    //发送指令时, 自定义的数据
    var custom: Int = -1,
    //Z轴连接状态, 0未连接, 1链接
    var zConnect: Int = 0,
    //图片打印次数
    var printTimes: Int = 0,
    //设备与水平面的平角
    var angle: Int = 0
) : IPacketParse<DeviceStateBean> {
    //解析数据
    override fun parse(packet: ByteArray): DeviceStateBean? {
        return try {
            packet.reader {
                offset(LaserPeckerHelper.packetHeadSize)//偏移头部
                offset(1)//偏移长度
                offset(1)//偏移功能码
                mode = readInt(1)
                workState = readInt(1)
                rate = readInt(1)
                laser = readInt(1)
                speed = readInt(1)
                error = readInt(1)
                state = readInt(1)
                name = readInt(4)
                temp = readInt(1)
                custom = readInt(1)
                zConnect = readInt(1)
                printTimes = readInt(1)
                angle = readInt(1)
            }
            this
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}