package com.angcyo.bluetooth.fsc.laserpacker.command

import com.angcyo.bluetooth.fsc.IReceiveBeanAction
import com.angcyo.bluetooth.fsc.ISendProgressAction
import com.angcyo.bluetooth.fsc.ReceivePacket
import com.angcyo.bluetooth.fsc.WaitReceivePacket
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.DEFAULT_RECEIVE_TIMEOUT
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.packetHeadSize
import com.angcyo.library.ex.toHexByteArray
import com.angcyo.library.toast

/**
 * 功能码(第四个字节): AABB 长度 功能码
 * 0x00 查询指令      无提示声
 * 0x01 开始打印指令
 * 0x02 打印预览指令
 * 0x04 调焦指令
 * 0x05 文件传输指令
 * 0x06 设置指令
 * 0x0f 出厂设置指令
 * 0xdd 固件更新指令
 * 0xff 退出指令
 *
 * 进入文件传输指令:
 * -> AA BB 09 05 01 00 0E B3 42 00 01 09
 * <- AA BB 08 05 01 01 00 00 00 00 07  //rev=1 时表示接收成功，rev=0 时表示接收失败
 * <- AA BB 08 05 02 01 00 00 00 00 08  //0x02 表示数据传输结束回应
 *
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/26
 */
interface ICommand {

    /**指令唯一标识符*/
    val uuid: String

    /**优先调用此方法, 如果为空, 则使用[toHexCommandString]*/
    fun toByteArray(): ByteArray = toHexCommandString().toHexByteArray()

    /**功能码*/
    fun commandFunc(): Byte = toByteArray()[packetHeadSize + 1]

    /**转换成十六进制指令, 可以包含空格.
     * 最后转换ByteArray时, 会剔除空格
     *
     *
     * 长度：指由功能码开始到较验和一共包含的字节数，这里为不定长Len,一般情况下最小长度为0x08。
     *
     * 功能码：用来指定该数据包功能
     *
     * Data ：数据包携带的(Len - 2)字节数据
     *
     * 较验和：包含“功能码”“数据内容”在内，由“功能码”开始至数据结束内容的较验和运算结果，高字节先发送。
     *
     * */
    fun toHexCommandString(): String = ""

    /**获取指令超时时长, 毫秒*/
    fun getReceiveTimeout(): Long = DEFAULT_RECEIVE_TIMEOUT

    /**转换成日志*/
    fun toCommandLogString(): String
}

/**发送一条指令, 未连接设备时, 返回空*/
fun ICommand.sendCommand(
    address: String? = null,
    progress: ISendProgressAction? = null,
    action: IReceiveBeanAction? = { bean: ReceivePacket?, error: Exception? ->
        error?.let { toast(it.message) }
    }
): WaitReceivePacket? {
    return LaserPeckerHelper.sendCommand(this, address, progress, action)
}