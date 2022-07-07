package com.angcyo.bluetooth.fsc.laserpacker

import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.RectF
import androidx.core.graphics.scale
import com.angcyo.bluetooth.fsc.*
import com.angcyo.bluetooth.fsc.laserpacker.command.ICommand
import com.angcyo.bluetooth.fsc.laserpacker.command.QueryCmd
import com.angcyo.bluetooth.fsc.laserpacker.data.LaserPeckerProductInfo
import com.angcyo.bluetooth.fsc.laserpacker.data.PxInfo
import com.angcyo.bluetooth.fsc.laserpacker.parse.QuerySettingParser
import com.angcyo.bluetooth.fsc.laserpacker.parse.QueryStateParser
import com.angcyo.bluetooth.fsc.laserpacker.parse.QueryVersionParser
import com.angcyo.canvas.core.MmValueUnit
import com.angcyo.core.component.file.writeTo
import com.angcyo.core.vmApp
import com.angcyo.library.L
import com.angcyo.library.component.HawkPropertyValue
import com.angcyo.library.component.flow
import com.angcyo.library.ex.toHexByteArray
import com.angcyo.library.ex.toHexString
import com.angcyo.library.ex.wrapLog
import com.angcyo.library.utils.Constant

/**
 * https://docs.qq.com/doc/DWE1MVnVOQ3RJSXZ1
 * 产品型号/参数表
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/24
 */
object LaserPeckerHelper {

    //region ---产品名称---

    //1为450nm激光, 波长

    //---焦距 200mm
    const val LI = "LI"
    const val LI_Z = "LI-Z"           //spp 100*100mm
    const val LI_PRO = "LI-PRO"       //spp 100*100mm
    const val LI_Z_PRO = "LI-Z-PRO"   //spp 100*100mm

    //---焦距 110mm
    const val LII = "LII"             //spp 100*100mm
    const val LI_Z_ = "LI-Z模块"         //spp 100*100mm
    const val LII_M_ = "LII-M模块"       //spp 100*100mm

    //---焦距 115mm
    const val LIII_YT = "LIII-YT"       //spp 50*50mm
    const val LIII = "LIII"           //spp 90*70mm 椭圆

    //0为1064nm激光
    const val LIII_MAX = "LIII-MAX"   //spp 200mm*200mm 160*120mm 椭圆

    //---焦距 40mm
    const val CI = "CI"               //spp 300*400mm
    const val CII = "CII"
    const val UNKNOWN = "Unknown"

    //endregion ---产品名称---

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

    //设备支持的分辨率
    const val PX_4K: Byte = 0x01
    const val PX_2K: Byte = 0x02
    const val PX_1_3K: Byte = 0x03
    const val PX_1K: Byte = 0x04
    const val PX_0_8K: Byte = 0x05

    //默认的分辨率
    const val DEFAULT_PX: Byte = PX_1K //1k

    //所有支持的分辨率
    val pxInfoList = mutableListOf<PxInfo>().apply {
        add(PxInfo(0x01, 4000, 4000, "4K"))
        add(PxInfo(0x02, 2000, 2000, "2K"))
        add(PxInfo(0x03, 1300, 1300, "1.3K"))
        add(PxInfo(0x04, 1000, 1000, "1K"))
        add(PxInfo(0x05, 800, 800, "0.8K"))
    }

    /**预览光功率设置 [0~1f]*/
    var lastPwrProgress: Float by HawkPropertyValue<Any, Float>(0.5f)

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
        productInfo: LaserPeckerProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value
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

    /**
     * 根据固件软件版本号[softwareVersion], 解析出对应的产品信息.
     * 解析产品信息*/
    fun parseProductInfo(softwareVersion: Int): LaserPeckerProductInfo {
        val name = parseProductName(softwareVersion)
        val unit = MmValueUnit()
        val bounds = RectF()
        var isOriginCenter = true
        val limitPath: Path = when (name) {
            LI_Z, LI_PRO, LI_Z_PRO, LII, LI_Z_, LII_M_ -> {
                Path().apply {
                    val left = unit.convertValueToPixel(-50f)
                    val right = unit.convertValueToPixel(50f)
                    bounds.set(left, left, right, right)
                    addRect(bounds, Path.Direction.CW)
                }
            }
            LIII -> {
                Path().apply {
                    val left = unit.convertValueToPixel(-50f)
                    val right = unit.convertValueToPixel(50f)
                    bounds.set(left, left, right, right)

                    val l = unit.convertValueToPixel(-50f)
                    val t = unit.convertValueToPixel(-35f)
                    //addOval(l, t, -l, -t, Path.Direction.CW)
                    maxOvalPath(l, t, -l, -t, this)
                }
            }
            LIII_MAX -> {
                Path().apply {
                    val left = unit.convertValueToPixel(-100f)
                    val right = unit.convertValueToPixel(100f)
                    bounds.set(left, left, right, right)

                    val l = unit.convertValueToPixel(-80f)
                    val t = unit.convertValueToPixel(-60f)
                    //addOval(l, t, -l, -t, Path.Direction.CW)
                    maxOvalPath(l, t, -l, -t, this)
                }
            }
            CI -> {
                isOriginCenter = false
                Path().apply {
                    val width = unit.convertValueToPixel(300f)
                    val height = unit.convertValueToPixel(400f)
                    bounds.set(0f, 0f, width, height)
                    addRect(bounds, Path.Direction.CW)
                }
            }
            else -> Path()
        }
        val info = LaserPeckerProductInfo(softwareVersion, name, bounds, limitPath, isOriginCenter)
        return info
    }

    /**中间一个正方形, 左右各一个半圆*/
    fun maxOvalPath(left: Float, top: Float, right: Float, bottom: Float, path: Path) {
        val width = right - left
        val height = bottom - top
        if (width > height) {
            val x1 = (width - height) / 2
            path.moveTo(x1, bottom)
            path.addArc(left, top, left + x1 + x1, bottom, 90f, 180f)
            path.lineTo(left + x1 + height, top)

            path.addArc(right - x1 - x1, top, right, bottom, -90f, 180f)
            path.lineTo(left + x1, bottom)
        } else {
            val y1 = (height - width) / 2
            path.moveTo(left, y1)
            path.addArc(left, top, right, y1 + y1, 180f, 180f)
            path.lineTo(right, bottom - y1)

            path.addArc(left, bottom - y1 - y1, right, bottom, 0f, 180f)
            path.lineTo(left, top + y1)
        }
    }

    /**根据软件版本号, 解析成产品名称
     * Ⅰ、Ⅱ、Ⅲ、Ⅳ、Ⅴ、Ⅵ、Ⅶ、Ⅷ、Ⅸ、Ⅹ、Ⅺ、Ⅻ...
     * https://docs.qq.com/sheet/DT0htVG9tamZQTFBz*/
    fun parseProductName(version: Int): String? {
        val str = "$version"
        return if (str.startsWith("1")) { //1
            if (str.startsWith("15")) LI_Z else LI
        } else if (str.startsWith("2")) { //2
            if (str.startsWith("25")) LI_Z_PRO else LI_PRO
        } else if (str.startsWith("3")) { //3
            LII
        } else if (str.startsWith("4")) { //4
            if (str.startsWith("41")) LI_Z_ else if (str.startsWith("42")) LII_M_ else LIII_YT
        } else if (str.startsWith("5")) { //5
            LIII
        } else if (str.startsWith("6")) { //6
            LIII_MAX
        } else if (str.startsWith("7")) { //7
            if (str.startsWith("75")) CII else CI
        } else {
            null //UNKNOWN
        }
    }

    /**返回设备支持的分辨率列表*/
    fun findProductSupportPxList(): List<PxInfo> {
        val result = mutableListOf<PxInfo>()
        vmApp<LaserPeckerModel>().productInfoData.value?.let {
            result.add(findPxInfo(PX_1K)!!)
            if (!it.isLI()) {
                result.add(findPxInfo(PX_1_3K)!!)
                result.add(findPxInfo(PX_2K)!!)
            }
            if (!it.isLII()) {
                result.add(findPxInfo(PX_4K)!!)
            }
        }
        return result
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