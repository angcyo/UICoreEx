package com.angcyo.bluetooth.fsc.laserpacker

import android.graphics.Bitmap
import androidx.core.graphics.scale
import com.angcyo.bluetooth.fsc.*
import com.angcyo.bluetooth.fsc.laserpacker.command.ICommand
import com.angcyo.bluetooth.fsc.laserpacker.command.QueryCmd
import com.angcyo.bluetooth.fsc.laserpacker.data.ProductInfo
import com.angcyo.bluetooth.fsc.laserpacker.data.PxInfo
import com.angcyo.bluetooth.fsc.laserpacker.parse.QuerySettingParser
import com.angcyo.bluetooth.fsc.laserpacker.parse.QueryStateParser
import com.angcyo.bluetooth.fsc.laserpacker.parse.QueryVersionParser
import com.angcyo.core.component.file.writeTo
import com.angcyo.core.vmApp
import com.angcyo.library.L
import com.angcyo.library.component.flow
import com.angcyo.library.ex.toHexByteArray
import com.angcyo.library.ex.toHexString
import com.angcyo.library.ex.wrapLog
import com.angcyo.library.utils.Constant

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

    //数据返回超时时长, 毫秒
    const val DEFAULT_RECEIVE_TIMEOUT = 3_000L

    //默认的分辨率
    const val DEFAULT_PX: Byte = 0x04

    //所有支持的分辨率
    val pxInfoList = mutableListOf<PxInfo>().apply {
        add(PxInfo(0x01, 4000, 4000, "4K"))
        add(PxInfo(0x02, 2000, 2000, "2K"))
        add(PxInfo(0x03, 1300, 1300, "1.3K"))
        add(PxInfo(0x04, 1000, 1000, "1K"))
        add(PxInfo(0x05, 800, 800, "0.8K"))
    }

    /**预览光功率设置 [0~1f]*/
    var lastPwrProgress: Float = 0.5f

    //<editor-fold desc="operate">

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

    /**查找[PxInfo]*/
    fun findPxInfo(px: Byte?): PxInfo? = pxInfoList.find { it.px == px }

    /**根据选中的分辨率, 转换输入的大小*/
    fun transformWidth(value: Int, px: Byte): Int {
        return findPxInfo(px)?.transformWidth(value) ?: value
    }

    fun transformHeight(value: Int, px: Byte): Int {
        return findPxInfo(px)?.transformHeight(value) ?: value
    }

    /**根据选中的分辨率, 转换图片起始x坐标*/
    fun transformX(x: Int, px: Byte): Int {
        return findPxInfo(px)?.transformX(x) ?: x
    }

    fun transformY(y: Int, px: Byte): Int {
        return findPxInfo(px)?.transformY(y) ?: y
    }

    /**缩放图片到[px]指定的宽高*/
    fun bitmapScale(
        bitmap: Bitmap,
        px: Byte,
        productInfo: ProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value
    ): Bitmap {
        val productWidth = productInfo?.bounds?.width()?.toInt() ?: bitmap.width
        val productHeight = productInfo?.bounds?.height()?.toInt() ?: bitmap.height

        val scaleWidth = bitmap.width * 1f / productWidth
        val scaleHeight = bitmap.height * 1f / productHeight

        val width = findPxInfo(px)?.pxWidth ?: productWidth
        val height = findPxInfo(px)?.pxHeight ?: productHeight

        val newWidth = (width * scaleWidth).toInt()
        val newHeight = (height * scaleHeight).toInt()

        return bitmap.scale(newWidth, newHeight)
    }

    //</editor-fold desc="operate">

    //<editor-fold desc="packet">

    /**发送指令, 并且等待指令返回
     * [com.angcyo.bluetooth.fsc.FscBleApiModel.send)]*/
    fun waitCmdReturn(
        api: FscBleApiModel,
        address: String,
        sendPacket: ByteArray,
        autoSend: Boolean = true,
        func: Byte? = null,
        checkFunc: Boolean = true,
        receiveTimeOut: Long = 10_000,
        progress: ISendProgressAction = {}, //发包的进度回调
        action: IReceiveBeanAction
    ): WaitReceivePacket {
        return WaitReceivePacket(
            api,
            address,
            sendPacket,
            autoSend,
            func,
            checkFunc,
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
        L.i("发送指令:$address->${command.hashCode()} ${command.toCommandLogString()}".writeBleLog())
        return waitCmdReturn(
            api,
            address,
            command.toByteArray(),
            true,
            command.commandFunc(),
            true,
            command.getReceiveTimeout(), //数据返回超时时长
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
        val deviceState = apiModel.connectDeviceListData.value?.firstOrNull()
        if (deviceState == null) {
            action(null, IllegalArgumentException("未连接设备"))
            return null
        }
        return sendCommand(
            deviceState.device.address,
            command,
            apiModel,
            progress,
            action
        )
    }

    /**发送初始化的指令, 读取设备的基础信息*/
    fun sendInitCommand(address: String, end: (Throwable?) -> Unit = {}) {
        val laserPeckerModel = vmApp<LaserPeckerModel>()
        flow { chain ->
            //读取设备版本
            sendCommand(address, QueryCmd.version) { bean, error ->
                bean?.let {
                    it.parse<QueryVersionParser>()?.let {
                        laserPeckerModel.updateDeviceVersion(it)
                    }
                }
                chain(error)
            }
        }.flow { chain ->
            //读取设备工作状态
            sendCommand(address, QueryCmd.workState) { bean, error ->
                bean?.let {
                    it.parse<QueryStateParser>()?.let {
                        laserPeckerModel.updateDeviceState(it)
                    }
                }
                chain(error)
            }
        }.flow { chain ->
            //读取设备设置状态
            sendCommand(address, QueryCmd.settingState) { bean, error ->
                bean?.let {
                    it.parse<QuerySettingParser>()?.let {
                        laserPeckerModel.updateDeviceSettingState(it)
                    }
                }
                chain(error)
            }
        }.start {
            end(it)
        }
    }

    //</editor-fold desc="packet">
}

/**将日志写入到[ble.log]*/
fun String.writeBleLog(): String {
    wrapLog().writeTo(Constant.LOG_FOLDER_NAME, "ble.log")
    return this
}