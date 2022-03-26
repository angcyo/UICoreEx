package com.angcyo.bluetooth.fsc.laserpacker.command

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.checksum
import com.angcyo.library.ex.padHexString
import com.angcyo.library.ex.toHexString

/**
 * 查询指令
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/26
 */

data class StateCmd(
    val state: Byte, //0:表示查询工作状态 1:表示查询文件列表 2:表示查询设置状态 3:表示查询版本
    val custom: Byte = 0 //自定义的数据
) : IDeviceCommand {
    override fun toHexCommandString(): String {
        val dataLength = 8 //数据长度
        val func = "00" //功能码
        val data = buildString {
            append(func)
            append(state.toHexString())
            append(custom.toHexString())
        }.padHexString(dataLength - LaserPeckerHelper.CHECK_SIZE)
        val check = data.checksum() //“功能码”和“数据内容”在内的校验和
        val cmd = "${LaserPeckerHelper.PACKET_HEAD} ${dataLength.toHexString()} $data $check"
        return cmd
    }
}
