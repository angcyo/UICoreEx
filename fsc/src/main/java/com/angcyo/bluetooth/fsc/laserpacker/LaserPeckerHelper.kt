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
import com.angcyo.core.component.file.writeToLog
import com.angcyo.core.vmApp
import com.angcyo.http.rx.doBack
import com.angcyo.library.L
import com.angcyo.library.component.flow
import com.angcyo.library.ex.toHexByteArray
import com.angcyo.library.ex.toHexString

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

    //0为1064nm激光, LIII_MAX 改名 LIV
    const val LIII_MAX = "LIV"   //spp 160mm*160mm 160*120mm 椭圆
    const val LIV = LIII_MAX

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

    //雕刻激光类型选择
    const val LASER_TYPE_WHITE = 0x01.toByte() //1为1064nm激光 (白光-雕)

    const val LASER_TYPE_BLUE = 0x00.toByte() //0为450nm激光 (蓝光-烧)

    /**2米, mm单位
     * 2m = 2000mm*/
    const val Z_MAX_Y = 2_00_0

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
    fun findPxInfo(px: Byte?): PxInfo? =
        vmApp<LaserPeckerModel>().productInfoData.value?.pxList?.find { it.px == px }

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

        //激光类型
        var typeList: List<Byte> = listOf(LASER_TYPE_BLUE)

        //所有支持的分辨率
        val pxList: MutableList<PxInfo> = mutableListOf()

        val unit = MmValueUnit()
        val bounds = RectF()
        var isOriginCenter = true

        val limitPath = Path()
        val zLimitPath = Path()

        val zMax = unit.convertValueToPixel(Z_MAX_Y.toFloat())

        //物理尺寸宽高mm单位
        var wPhys = 0
        var hPhys = 0

        when (name) {
            LI_Z, LI_PRO, LI_Z_PRO, LII, LI_Z_, LII_M_ -> {
                wPhys = 100
                hPhys = 100
                val left = unit.convertValueToPixel(-wPhys / 2f)
                val right = unit.convertValueToPixel(wPhys / 2f)
                limitPath.apply {
                    bounds.set(left, left, right, right)
                    addRect(bounds, Path.Direction.CW)
                }
                zLimitPath.addRect(left, left, right, zMax, Path.Direction.CW)
            }
            LIII -> {
                wPhys = 100
                hPhys = 100
                val left = unit.convertValueToPixel(-wPhys / 2f)
                val right = unit.convertValueToPixel(wPhys / 2f)
                limitPath.apply {
                    bounds.set(left, left, right, right)

                    val l = unit.convertValueToPixel(-50f)
                    val t = unit.convertValueToPixel(-35f)
                    //addOval(l, t, -l, -t, Path.Direction.CW)
                    maxOvalPath(l, t, -l, -t, this)
                }
                zLimitPath.addRect(left, left, right, zMax, Path.Direction.CW)
                typeList = listOf(LASER_TYPE_WHITE)
            }
            LIII_MAX -> {
                //160*160
                wPhys = 160
                hPhys = 160
                val left = unit.convertValueToPixel(-wPhys / 2f)
                val right = unit.convertValueToPixel(wPhys / 2f)
                limitPath.apply {
                    bounds.set(left, left, right, right)

                    val l = unit.convertValueToPixel(-80f)
                    val t = unit.convertValueToPixel(-60f)
                    //addOval(l, t, -l, -t, Path.Direction.CW)
                    maxOvalPath(l, t, -l, -t, this)
                }
                zLimitPath.addRect(left, left, right, zMax, Path.Direction.CW)
                typeList = listOf(LASER_TYPE_BLUE, LASER_TYPE_WHITE)
            }
            CI -> {
                wPhys = 300
                hPhys = 400
                isOriginCenter = false
                val width = unit.convertValueToPixel(wPhys.toFloat())
                val height = unit.convertValueToPixel(hPhys.toFloat())
                limitPath.apply {
                    bounds.set(0f, 0f, width, height)
                    addRect(bounds, Path.Direction.CW)
                }
                zLimitPath.addRect(0f, 0f, width, zMax, Path.Direction.CW)
            }
        }

        pxList.add(PxInfo(PX_1K, wPhys * 10, hPhys * 10, PX_1K.toPxDes()))

        when (name) {
            LI, LI_PRO -> {
                //pxList.add(PxInfo(0x04, 1000, 1000, "1K"))
            }
            LI_Z, LI_Z_PRO, LII -> {
                pxList.add(PxInfo(PX_1_3K, wPhys * 13, hPhys * 13, PX_1_3K.toPxDes()))
                pxList.add(PxInfo(PX_2K, wPhys * 20, hPhys * 20, PX_2K.toPxDes()))
            }
            LIII -> {
                pxList.add(PxInfo(PX_1_3K, wPhys * 13, hPhys * 13, PX_1_3K.toPxDes()))
                pxList.add(PxInfo(PX_2K, wPhys * 20, hPhys * 20, PX_2K.toPxDes()))
                pxList.add(PxInfo(PX_4K, wPhys * 40, hPhys * 40, PX_4K.toPxDes()))
            }
            LIV -> {
                pxList.add(PxInfo(PX_2K, wPhys * 20, hPhys * 20, PX_2K.toPxDes()))
                pxList.add(PxInfo(PX_4K, wPhys * 40, hPhys * 40, PX_4K.toPxDes()))
            }
        }

        return LaserPeckerProductInfo(
            softwareVersion,
            name,
            typeList,
            pxList,
            wPhys,
            hPhys,
            bounds,
            limitPath,
            zLimitPath,
            isOriginCenter
        )
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
        vmApp<LaserPeckerModel>().productInfoData.value?.pxList?.let {
            result.addAll(it)
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
        progress: ISendProgressAction?, //发包的进度回调
        action: IReceiveBeanAction?
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
                    progress?.invoke(bean)
                }

                override fun onReceive(bean: ReceivePacket?, error: Exception?) {
                    action?.invoke(bean, error)
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
        progress: ISendProgressAction? = null,
        action: IReceiveBeanAction?
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

    /**发送一条指令, 未连接设备时, 返回空 [ICommand]
     * [command] 需要发送的指令
     * [address] 设备地址, 如果不传, 则使用最后一台连接的设备
     * */
    fun sendCommand(
        command: ICommand,
        address: String? = null,
        progress: ISendProgressAction? = null,
        action: IReceiveBeanAction?
    ): WaitReceivePacket? {
        val apiModel = vmApp<FscBleApiModel>()
        var deviceAddress = address
        if (deviceAddress.isNullOrEmpty()) {
            val deviceState = apiModel.connectDeviceListData.value?.lastOrNull()
            if (deviceState == null) {
                action?.invoke(null, IllegalArgumentException("未连接设备"))
                return null
            }
            deviceAddress = deviceState.device.address
        }
        return sendCommand(
            deviceAddress!!,
            command,
            apiModel,
            progress,
            action
        )
    }

    /**初始化的蓝牙设备名称*/
    var initDeviceName: String? = null

    /**初始化的蓝牙设备地址*/
    var initDeviceAddress: String? = null

    /**发送初始化的指令, 读取设备的基础信息
     * [name] 蓝牙设备的名称
     * [address] 蓝牙设备的地址
     * */
    fun sendInitCommand(name: String, address: String, end: (Throwable?) -> Unit = {}) {
        val laserPeckerModel = vmApp<LaserPeckerModel>()
        initDeviceName = name
        initDeviceAddress = address
        flow { chain ->
            //读取设备设置状态
            sendCommand(address, QueryCmd.settingState) { bean, error ->
                bean?.let {
                    it.parse<QuerySettingParser>()?.let {
                        laserPeckerModel.updateDeviceSettingState(it)
                    }
                }
                chain(error)
            }
        }.flow { chain ->
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
        }.start {
            laserPeckerModel.initializeData.postValue(it == null)
            if (it == null) {
                //初始化完成
                end(it)
            } else {
                //再来一次
                doBack {
                    sendInitCommand(name, address, end)
                }
            }
        }
    }

    /**初始化设备设置数据*/
    fun initDeviceSetting() {
        sendCommand(QueryCmd.settingState) { bean, error ->
            bean?.let {
                it.parse<QuerySettingParser>()?.let {
                    vmApp<LaserPeckerModel>().updateDeviceSettingState(it)
                }
            }
        }
    }

    //</editor-fold desc="packet">
}

/**分辨率转描述字符串*/
fun Byte.toPxDes() = when (this) {
    LaserPeckerHelper.PX_4K -> "4K"
    LaserPeckerHelper.PX_2K -> "2K"
    LaserPeckerHelper.PX_1_3K -> "1.3K"
    LaserPeckerHelper.PX_1K -> "1K"
    LaserPeckerHelper.PX_0_8K -> "0.8K"
    else -> "~1K"
}

/**将日志写入到[ble.log]*/
fun String.writeBleLog(): String = writeToLog("ble.log")