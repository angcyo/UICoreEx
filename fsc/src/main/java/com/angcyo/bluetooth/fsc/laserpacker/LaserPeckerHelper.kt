package com.angcyo.bluetooth.fsc.laserpacker

import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.RectF
import androidx.core.graphics.scale
import com.angcyo.bluetooth.fsc.*
import com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.ICommand
import com.angcyo.bluetooth.fsc.laserpacker.command.QueryCmd
import com.angcyo.bluetooth.fsc.laserpacker.data.LaserPeckerProductInfo
import com.angcyo.bluetooth.fsc.laserpacker.data.LaserTypeInfo
import com.angcyo.bluetooth.fsc.laserpacker.data.PxInfo
import com.angcyo.bluetooth.fsc.laserpacker.parse.*
import com.angcyo.canvas.data.CanvasProjectItemBean.Companion.MM_UNIT
import com.angcyo.core.component.file.writeToLog
import com.angcyo.core.vmApp
import com.angcyo.http.rx.doBack
import com.angcyo.library.annotation.MM
import com.angcyo.library.component.flow
import com.angcyo.library.ex.*
import com.angcyo.library.utils.LogFile
import com.angcyo.objectbox.findFirst
import com.angcyo.objectbox.laser.pecker.LPBox
import com.angcyo.objectbox.laser.pecker.entity.CommandEntity
import com.angcyo.objectbox.laser.pecker.entity.CommandEntity_
import com.angcyo.objectbox.laser.pecker.lpSaveEntity

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
    const val LI = "L1"               //"LI"
    const val LI_Z = "L1-Z"           //"LI-Z"       //spp 100*100mm
    const val LI_PRO = "L1-Pro"       //"LI-PRO"     //spp 100*100mm
    const val LI_Z_PRO = "L1-Z-Pro"   //"LI-Z-PRO"   //spp 100*100mm

    //---焦距 110mm
    const val LII = "L2"           //"LII"             //spp 100*100mm
    const val LI_Z_ = "L1-Z模块"    //"LI-Z模块"        //spp 100*100mm
    const val LII_M_ = "L2-M模块"   //"LII-M模块"       //spp 100*100mm

    //---焦距 115mm
    const val LIII_YT = "L3-YT" //"LIII-YT"       //spp 50*50mm
    const val LIII = "L3"       //"LIII"          //spp 90*70mm 椭圆

    //0为1064nm激光, LIII_MAX 改名 LIV
    const val LIII_MAX = "L4"      //"LIV"   //spp 160mm*160mm 160*120mm 椭圆
    const val LIV = LIII_MAX

    //---焦距 40mm
    const val CI = "C1"            //"CI"               //spp 300*400mm
    const val CII = "C2"           //"CII"
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
    @Deprecated("请使用dpi")
    const val PX_4K: Byte = 0x01

    @Deprecated("请使用dpi")
    const val PX_2K: Byte = 0x02

    @Deprecated("请使用dpi")
    const val PX_1_3K: Byte = 0x03

    @Deprecated("请使用dpi")
    const val PX_1K: Byte = 0x04

    @Deprecated("请使用dpi")
    const val PX_0_8K: Byte = 0x05

    //推荐DPI:

    /**[PX_0_8K]*/
    const val DPI_158 = 158.75f

    /**[PX_1K]*/
    const val DPI_254 = 254f

    /**[PX_1_3K]*/
    const val DPI_317 = 317.5f
    const val DPI_423 = 423.33333f

    /**[PX_2K]*/
    const val DPI_508 = 508f
    const val DPI_635 = 635f

    const val DPI_846 = 846.66666f

    /**[PX_4K]*/
    const val DPI_1270 = 1270f

    //雕刻激光类型选择
    const val LASER_TYPE_WHITE = 0x01.toByte() //1为1064nm激光 (白光-雕)

    const val LASER_TYPE_BLUE = 0x00.toByte() //0为450nm激光 (蓝光-烧)

    /**滚动轴
     * 2米, mm单位, Z轴最大的Y坐标
     * 2m = 2000mm*/
    @MM
    const val Z_MAX_Y = 2_00_0

    /**旋转轴
     * 200 * 3.14 [Math.PI]*/
    @MM
    const val R_MAX_Y = 628

    /**滑台*/
    @MM
    const val S_MAX_Y = 300

    /**C1移动平台/小车模式
     * 5米*/
    @MM
    const val CAR_MAX_Y = 5_00_0

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
    fun findPxInfo(dpi: Float?): PxInfo =
        PxInfo(dpi ?: DPI_254, vmApp<LaserPeckerModel>().productInfoData.value?.widthPhys ?: 100)

    /**缩放图片到[px]指定的宽高*/
    fun bitmapScale(
        bitmap: Bitmap,
        dpi: Float,
        productInfo: LaserPeckerProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value
    ): Bitmap {
        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height

        val productWidth = productInfo?.bounds?.width()?.toInt() ?: bitmapWidth
        val productHeight = productInfo?.bounds?.height()?.toInt() ?: bitmapHeight

        var newWidth = bitmapWidth
        var newHeight = bitmapHeight

        val pxInfo = findPxInfo(dpi)
        if (bitmapWidth > 1) {
            val scaleWidth = bitmapWidth * 1f / productWidth
            val width = pxInfo.devicePxWidth(productInfo)
            newWidth = (width * scaleWidth).toInt()
        }

        if (bitmapHeight > 1) {
            val scaleHeight = bitmapHeight * 1f / productHeight
            val height = pxInfo.devicePxHeight(productInfo)
            newHeight = (height * scaleHeight).toInt()
        }

        //图片缩放到指定宽高
        return bitmap.scale(newWidth, newHeight)
    }

    /**
     * 根据固件软件版本号[softwareVersion], 解析出对应的产品信息.
     * 解析产品信息*/
    fun parseProductInfo(softwareVersion: Int, center: Boolean? = null): LaserPeckerProductInfo {
        val name = parseProductName(softwareVersion)

        //激光类型, 默认是蓝光
        //蓝光
        val blueInfo = LaserTypeInfo(LASER_TYPE_BLUE, 450, _string(R.string.laser_type_blue))
        //白光
        val whiteInfo = LaserTypeInfo(LASER_TYPE_WHITE, 1064, _string(R.string.laser_type_white))
        var laserTypeList: List<LaserTypeInfo> = listOf(blueInfo)

        //所有支持的分辨率
        val pxList: MutableList<PxInfo> = mutableListOf()

        val mmValueUnit = MM_UNIT
        val bounds = RectF()
        val previewBounds = RectF()
        val carPreviewBounds = RectF()
        var isOriginCenter = center ?: false

        val limitPath = Path()
        val zLimitPath = Path()
        val rLimitPath = Path()
        val sLimitPath = Path()
        val carLimitPath = Path()

        val zMax = mmValueUnit.convertValueToPixel(Z_MAX_Y.toFloat())
        val rMax = mmValueUnit.convertValueToPixel(R_MAX_Y.toFloat())
        val sMax = mmValueUnit.convertValueToPixel(S_MAX_Y.toFloat())
        val carMax = mmValueUnit.convertValueToPixel(CAR_MAX_Y.toFloat())

        //物理尺寸宽高mm单位
        var wPhys = 0
        var hPhys = 0

        when (name) {
            LI_Z, LI_PRO, LI_Z_PRO, LII, LI_Z_, LII_M_, LIII -> {
                wPhys = 100
                hPhys = 100
                isOriginCenter = center ?: false
            }
            LIII_MAX, LIV -> {
                //160*160
                wPhys = 160
                hPhys = 160
                isOriginCenter = center ?: false
            }
            CI -> {
                wPhys = 400
                hPhys = 420
                isOriginCenter = center ?: false
            }
        }

        //bounds
        val left =
            mmValueUnit.convertValueToPixel(if (isOriginCenter) -wPhys / 2f else 0f)
        val top = mmValueUnit.convertValueToPixel(if (isOriginCenter) -hPhys / 2f else 0f)
        val right =
            mmValueUnit.convertValueToPixel(if (isOriginCenter) wPhys / 2f else wPhys.toFloat())
        val bottom =
            mmValueUnit.convertValueToPixel(if (isOriginCenter) hPhys / 2f else hPhys.toFloat())
        bounds.set(left, top, right, bottom)
        previewBounds.set(left, top, right, bottom)

        limitPath.addRect(bounds, Path.Direction.CW)
        zLimitPath.addRect(left, top, right, zMax, Path.Direction.CW)
        rLimitPath.addRect(left, top, right, rMax, Path.Direction.CW)
        sLimitPath.addRect(left, top, right, sMax, Path.Direction.CW)
        //C1移动平台模式限制大小
        val carWPhys = wPhys - 50
        val carRight =
            mmValueUnit.convertValueToPixel(if (isOriginCenter) carWPhys / 2f else carWPhys.toFloat())
        carLimitPath.addRect(left, top, carRight, carMax, Path.Direction.CW)
        carPreviewBounds.set(left, top, carRight, bottom)

        //最佳预览范围设置
        when (name) {
            LIII -> {
                //最佳打印范围是椭圆
                limitPath.apply {
                    //2022-9-27 试产版范围：90x60，量产版范围：100x75
                    rewind()
                    val rW = 100f
                    val rH = 75f
                    val tOffset = (rW - rH) / 2 //mm
                    val l = mmValueUnit.convertValueToPixel(if (isOriginCenter) -rW / 2f else 0f)
                    val t =
                        mmValueUnit.convertValueToPixel(if (isOriginCenter) -rH / 2f else tOffset)
                    val r = mmValueUnit.convertValueToPixel(if (isOriginCenter) rW / 2f else rW)
                    val b =
                        mmValueUnit.convertValueToPixel(if (isOriginCenter) rH / 2f else rH + tOffset)
                    previewBounds.set(l, t, r, b)
                    maxOvalPath(l, t, r, b, this)
                }
                laserTypeList = listOf(whiteInfo)
            }
            LIII_MAX, LIV -> {
                //最佳打印范围是椭圆
                //160*160 mm
                limitPath.apply {
                    rewind()
                    val rW = 160f
                    val rH = 120f
                    val tOffset = (rW - rH) / 2 //mm
                    val l = mmValueUnit.convertValueToPixel(if (isOriginCenter) -rW / 2f else 0f)
                    val t =
                        mmValueUnit.convertValueToPixel(if (isOriginCenter) -rH / 2f else tOffset)
                    val r = mmValueUnit.convertValueToPixel(if (isOriginCenter) rW / 2f else rW)
                    val b =
                        mmValueUnit.convertValueToPixel(if (isOriginCenter) rH / 2f else rH + tOffset)
                    previewBounds.set(l, t, r, b)
                    maxOvalPath(l, t, r, b, this)
                }
                laserTypeList = listOf(blueInfo, whiteInfo)
            }
        }

        when (name) {
            //LI, LI_PRO -> Unit
            LI_Z, LI_Z_PRO, LII -> {
                pxList.add(PxInfo(DPI_317, wPhys))
                pxList.add(PxInfo(DPI_508, wPhys))
            }
            LIII -> {
                pxList.add(PxInfo(DPI_254, wPhys))
                pxList.add(PxInfo(DPI_317, wPhys))
                pxList.add(PxInfo(DPI_508, wPhys))
                pxList.add(PxInfo(DPI_1270, wPhys))
            }
            LIV -> {
                if (isDebug()) {
                    pxList.add(PxInfo(DPI_158, wPhys))
                }
                pxList.add(PxInfo(DPI_254, wPhys))
                if (isDebug()) {
                    pxList.add(PxInfo(DPI_317, wPhys))
                    pxList.add(PxInfo(DPI_423, wPhys))
                }
                pxList.add(PxInfo(DPI_508, wPhys))
                if (isDebug()) {
                    pxList.add(PxInfo(DPI_635, wPhys))
                    pxList.add(PxInfo(DPI_846, wPhys))
                }
                pxList.add(PxInfo(DPI_1270, wPhys))
            }
            else -> {
                pxList.add(PxInfo(DPI_254, wPhys))
            }
        }

        //result
        return LaserPeckerProductInfo(
            softwareVersion,
            name,
            laserTypeList,
            pxList,
            wPhys,
            hPhys,
            bounds,
            previewBounds,
            limitPath
        ).apply {
            this.isOriginCenter = isOriginCenter
            this.zLimitPath = zLimitPath
            this.rLimitPath = rLimitPath
            this.sLimitPath = sLimitPath
            this.carLimitPath = carLimitPath
            this.carPreviewBounds = carPreviewBounds
        }
    }

    /**切换设备中心点
     * @return 切换是否成功*/
    fun switchDeviceCenter(): Boolean {
        val productInfoData = vmApp<LaserPeckerModel>().productInfoData
        val productInfo = productInfoData.value ?: return false
        val center = !productInfo.isOriginCenter
        val newProductInfo = parseProductInfo(productInfo.softwareVersion, center)
        //赋值
        newProductInfo.deviceName = productInfo.deviceName
        newProductInfo.deviceAddress = productInfo.deviceAddress
        newProductInfo.softwareVersion = productInfo.softwareVersion
        newProductInfo.hardwareVersion = productInfo.hardwareVersion
        productInfoData.postValue(newProductInfo)
        return true
    }

    /**保持有效宽高下,  4个角用曲线连接*/
    fun maxOvalPath(left: Float, top: Float, right: Float, bottom: Float, path: Path) {
        val l = left.floor()
        val t = top.floor()
        val r = right.ceil()
        val b = bottom.ceil()

        val width = r - l
        val height = b - t

        val centerX = (l + r) / 2
        val centerY = (t + b) / 2

        //一定能雕刻上的有效宽高, 在中心位置
        val validWidth = width * 2 / 3
        val validHeight = height * 2 / 3

        //底部左右2边的点
        val blX = centerX - validWidth / 2
        val blY = b

        val brX = centerX + validWidth / 2
        val brY = b

        //顶部左右2边的点
        val tlX = centerX - validWidth / 2
        val tlY = t

        val trX = centerX + validWidth / 2
        val trY = t

        //左边的2个点
        val ltX = l
        val ltY = centerY - validHeight / 2

        val lbX = l
        val lbY = centerY + validHeight / 2

        //右边的2个点
        val rtX = r
        val rtY = centerY - validHeight / 2

        val rbX = r
        val rbY = centerY + validHeight / 2

        //
        path.rewind()

        path.moveTo(blX, blY)
        val lbcX = l
        val lbcY = b
        path.quadTo(lbcX, lbcY, lbX, lbY)

        path.lineTo(ltX, ltY)
        val ltcX = l
        val ltcY = t
        path.quadTo(ltcX, ltcY, tlX, tlY)

        path.lineTo(trX, trY)
        val rtcX = r
        val rtcY = t
        path.quadTo(rtcX, rtcY, rtX, rtY)

        path.lineTo(rbX, rbY)
        val rbcX = r
        val rbcY = b
        path.quadTo(rbcX, rbcY, brX, brY)

        path.lineTo(blX, blY)
        path.close()
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

    /**返回设备支持的光源列表*/
    fun findProductSupportLaserTypeList(): List<LaserTypeInfo> {
        val result = mutableListOf<LaserTypeInfo>()
        vmApp<LaserPeckerModel>().productInfoData.value?.laserTypeList?.let {
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
        val uuid = command.uuid
        val commandLogString = command.toCommandLogString()
        "发送指令:$address->$uuid $commandLogString".writeBleLog()
        CommandEntity().apply {
            this.uuid = uuid
            this.command = command.toHexCommandString().removeAll()
            des = commandLogString
            sendTime = nowTime()
            lpSaveEntity()
        }
        return waitCmdReturn(
            api,
            address,
            command.toByteArray(),
            true,
            command.commandFunc(),
            true,
            command.getReceiveTimeout(), //数据返回超时时长
            progress
        ) { bean, error ->
            val log = "${bean?.parse<MiniReceiveParser>() ?: error}"
            "指令返回:${uuid}->${log}".writeBleLog()
            CommandEntity::class.findFirst(LPBox.PACKAGE_NAME) {
                apply(CommandEntity_.uuid.equal(uuid))
            }?.apply {
                resultTime = nowTime()
                result = "${bean?.receivePacket?.toHexString(false) ?: ""} ${error ?: ""}"
                lpSaveEntity()
            }
            action?.invoke(bean, error)
        }
    }

    /**发送一条指令, 未连接设备时, 返回空 [ICommand]
     * [command] 需要发送的指令
     * [address] 设备地址, 如果不传, 则使用最后一台连接的设备
     *
     * [com.angcyo.bluetooth.fsc.laserpacker.command.ICommandKt.sendCommand]
     * */
    fun sendCommand(
        command: ICommand,
        address: String? = null,
        progress: ISendProgressAction? = null,
        action: IReceiveBeanAction? = null
    ): WaitReceivePacket? {
        val apiModel = vmApp<FscBleApiModel>()
        var deviceAddress = address
        if (deviceAddress.isNullOrEmpty()) {
            val deviceState = apiModel.connectDeviceListData.value?.lastOrNull()
            if (deviceState == null) {
                action?.invoke(null, NoDeviceException(_string(R.string.blue_no_device_connected)))
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
            laserPeckerModel.initializeOnceData.postValue(it == null)
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
    fun initDeviceSetting(end: IReceiveBeanAction? = null) {
        sendCommand(QueryCmd.settingState) { bean, error ->
            bean?.let {
                it.parse<QuerySettingParser>()?.let {
                    vmApp<LaserPeckerModel>().updateDeviceSettingState(it)
                }
            }
            end?.invoke(bean, error)
        }
    }

    //</editor-fold desc="packet">
}

/**当前矩形, 是否超出了设备物理雕刻范围*/
fun RectF?.isOverflowProductBounds() = EngravePreviewCmd.adjustRectRange(this).isOverflowBounds

/**将日志写入到[ble.log]
 * [log] 是否还需要输出到控制台
 * [String.writeErrorLog]
 * */
fun String.writeBleLog(log: Boolean = true): String = writeToLog(LogFile.ble, log)

/**写入雕刻日志, 记录数据传输的索引及信息和雕刻的索引及信息*/
fun String.writeEngraveLog(log: Boolean = false): String = writeToLog(LogFile.engrave, log)