package com.angcyo.bluetooth.fsc.laserpacker.command

import com.angcyo.bluetooth.fsc.IReceiveBeanAction
import com.angcyo.bluetooth.fsc.ISendProgressAction
import com.angcyo.bluetooth.fsc.WaitReceivePacket
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.DEFAULT_RECEIVE_TIMEOUT
import com.angcyo.library.ex.toHexByteArray

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/26
 */
interface ICommand {

    /**优先调用此方法, 如果为空, 则使用[toHexCommandString]*/
    fun toByteArray(): ByteArray = toHexCommandString().toHexByteArray()

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

    /**获取指令超时时长*/
    fun getReceiveTimeout(): Long = DEFAULT_RECEIVE_TIMEOUT
}

/**发送一条指令, 未连接设备时, 返回空*/
fun ICommand.send(
    progress: ISendProgressAction = {},
    action: IReceiveBeanAction
): WaitReceivePacket? {
    return LaserPeckerHelper.sendCommand(this, progress, action)
}