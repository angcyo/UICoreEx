package com.angcyo.bluetooth.fsc.laserpacker

import com.angcyo.bluetooth.fsc.FscBleApiModel
import com.angcyo.bluetooth.fsc.laserpacker.bean.ReceivePacketBean
import com.angcyo.library.ex.padHexString
import com.angcyo.library.ex.toHexByteArray
import com.angcyo.library.ex.toHexString

/**
 * https://docs.qq.com/doc/DWE1MVnVOQ3RJSXZ1
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/24
 */
object LaserPeckerHelper {

    //固定数据头
    const val PACKET_HEAD = "AABB"

    //固定头的字节数
    val packetHeadSize: Int
        get() = PACKET_HEAD.toHexByteArray().size

    //校验位的占用字节数量
    const val CHECK_SIZE = 2

    /**移除所有空格*/
    fun String.trimCmd() = replace(" ", "")

    fun ByteArray.checksum(length: Int = CHECK_SIZE, hasSpace: Boolean = true): String =
        sumCheck(this, length).toHexString(hasSpace)

    fun String.checksum(length: Int = CHECK_SIZE, hasSpace: Boolean = true): String =
        sumCheck(toHexByteArray(), length).toHexString(hasSpace)

    /**
     * 校验和，经亲自验证数据没有错
     * https://blog.csdn.net/educast/article/details/52909524?locationNum=2&fps=1
     * @param bytes 需要计算校验和的byte数组
     * @param length 校验和结果的数据存储的字节数
     * @return 计算出的校验和数组
     */
    fun sumCheck(bytes: ByteArray, length: Int = CHECK_SIZE): ByteArray {
        var sum: Long = 0
        val result = ByteArray(length)

        /** 逐Byte添加位数和  */
        for (byteMsg in bytes) {
            val num = if (byteMsg.toLong() >= 0) byteMsg.toLong() else byteMsg.toLong() + 256
            sum += num
        }
        /** end of for (byte byteMsg : msg)  */

        /** 位数和转化为Byte数组  */
        for (count in 0 until length) {
            result[length - count - 1] = (sum shr count * 8 and 0xff).toByte()
        }
        /** end of for (int liv_Count = 0; liv_Count < length; liv_Count++)  */
        return result
    }

    /**查询指令
     * [state] 0:表示查询工作状态 1:表示查询文件列表 2:表示查询设置状态 3:表示查询版本
     * [custom] 自定义的数据*/
    fun stateCmd(state: Byte, custom: Byte = 0): String {
        val dataLength = 8
        val data = "00 ${state.toHexString()} ${custom.toHexString()}".padHexString(dataLength - 2)
        val check = data.checksum() //“功能码”和“数据内容”在内的校验和
        val cmd = "$PACKET_HEAD ${dataLength.toHexString()} $data $check"
        return cmd
    }

    //<editor-fold desc="packet">

    /**发送指令, 并且等待指令返回*/
    fun waitCmdReturn(
        api: FscBleApiModel,
        address: String,
        sendPacket: ByteArray,
        autoSend: Boolean = true,
        receiveTimeOut: Long = 10_000,
        progress: ISendProgressAction = {},
        action: IReceiveBeanAction
    ): WaitReceivePacket {
        return WaitReceivePacket(
            api,
            address,
            sendPacket,
            autoSend,
            receiveTimeOut,
            object : IReceiveListener {
                override fun onPacketProgress(bean: ReceivePacketBean) {
                    progress(bean)
                }

                override fun onReceive(bean: ReceivePacketBean?, error: Exception?) {
                    action(bean, error)
                }
            }).apply {
            start()
        }
    }

    //</editor-fold desc="packet">

}