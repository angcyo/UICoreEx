package com.angcyo.bluetooth.fsc.laserpacker

import com.angcyo.bluetooth.fsc.*
import com.angcyo.bluetooth.fsc.laserpacker.command.ICommand
import com.angcyo.bluetooth.fsc.laserpacker.data.ProductInfo
import com.angcyo.core.vmApp
import com.angcyo.library.ex.toHexByteArray
import com.angcyo.library.ex.toHexString

/**
 * https://docs.qq.com/doc/DWE1MVnVOQ3RJSXZ1
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/24
 */
object LaserPeckerHelper {

    /**蓝牙名称前缀, 只显示指定前缀的蓝牙设备*/
    const val PRODUCT_PREFIX = "LaserPecker"

    //固定数据头
    const val PACKET_HEAD = "AABB"

    //固定文件头的字节数量
    const val PACKET_FILE_HEAD_SIZE = 64

    //固定头的字节数
    val packetHeadSize: Int
        get() = PACKET_HEAD.toHexByteArray().size

    //校验位的占用字节数量
    const val CHECK_SIZE = 2

    //数据返回超时时长
    const val DEFAULT_RECEIVE_TIMEOUT = 1_000L

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

    /**根据选中的分辨率, 转换输入的大小*/
    fun transformHorizontalPixel(
        value: Int,
        px: Byte,
        productInfo: ProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value
    ): Int {
        if (productInfo == null) {
            return value
        }
        val scale = value * 1f / productInfo.bounds.width()
        val ref: Int = when (px) {
            0x01.toByte() -> 4000
            0x02.toByte() -> 2000
            0x03.toByte() -> 1300
            0x04.toByte() -> 1000
            0x05.toByte() -> 800
            else -> productInfo.bounds.width().toInt()
        }
        return (ref * scale).toInt()
    }

    fun transformVerticalPixel(
        value: Int,
        px: Byte,
        productInfo: ProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value
    ): Int {
        if (productInfo == null) {
            return value
        }
        val scale = value * 1f / productInfo.bounds.height()
        val ref: Int = when (px) {
            0x01.toByte() -> 4000
            0x02.toByte() -> 2000
            0x03.toByte() -> 1300
            0x04.toByte() -> 1000
            0x05.toByte() -> 800
            else -> productInfo.bounds.height().toInt()
        }
        return (ref * scale).toInt()
    }

    //<editor-fold desc="packet">

    /**发送指令, 并且等待指令返回
     * [com.angcyo.bluetooth.fsc.FscBleApiModel.send)]*/
    fun waitCmdReturn(
        api: FscBleApiModel,
        address: String,
        sendPacket: ByteArray,
        autoSend: Boolean = true,
        receiveTimeOut: Long = 10_000,
        progress: ISendProgressAction = {}, //发包的进度回调
        action: IReceiveBeanAction
    ): WaitReceivePacket {
        return WaitReceivePacket(
            api,
            address,
            sendPacket,
            autoSend,
            receiveTimeOut,
            object : IReceiveListener {
                override fun onPacketProgress(bean: ReceivePacket) {
                    progress(bean)
                }

                override fun onReceive(bean: ReceivePacket?, error: Exception?) {
                    action(bean, error)
                }
            }).apply {
            start()
        }
    }

    /**发送指令*/
    fun sendCommand(
        address: String,
        command: ICommand,
        api: FscBleApiModel = vmApp(),
        progress: ISendProgressAction = {},
        action: IReceiveBeanAction
    ): WaitReceivePacket {
        return waitCmdReturn(
            api,
            address,
            command.toByteArray(),
            true,
            DEFAULT_RECEIVE_TIMEOUT, //数据返回超时时长
            progress,
            action
        )
    }

    /**发送一条指令, 未连接设备时, 返回空
     * [ICommand]*/
    fun sendCommand(
        command: ICommand,
        progress: ISendProgressAction = {},
        action: IReceiveBeanAction
    ): WaitReceivePacket? {
        val apiModel = vmApp<FscBleApiModel>()
        val deviceState = apiModel.connectDeviceListData.value?.firstOrNull() ?: return null
        return sendCommand(deviceState.device.address, command, apiModel, progress, action)
    }

    //</editor-fold desc="packet">

}