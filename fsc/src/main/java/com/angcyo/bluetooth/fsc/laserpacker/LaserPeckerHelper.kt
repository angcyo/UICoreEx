package com.angcyo.bluetooth.fsc.laserpacker

import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.RectF
import androidx.core.graphics.scale
import com.angcyo.bluetooth.fsc.*
import com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.ICommand
import com.angcyo.bluetooth.fsc.laserpacker.command.QueryCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.parseResultPacketLog
import com.angcyo.bluetooth.fsc.laserpacker.data.*
import com.angcyo.bluetooth.fsc.laserpacker.parse.*
import com.angcyo.core.component.file.writeToLog
import com.angcyo.core.component.model.DataShareModel
import com.angcyo.core.vmApp
import com.angcyo.drawable.StateBarDrawable
import com.angcyo.http.rx.doBack
import com.angcyo.library.L
import com.angcyo.library.annotation.MM
import com.angcyo.library.component.RBackground
import com.angcyo.library.component.VersionMatcher
import com.angcyo.library.component.flow
import com.angcyo.library.component.hawk.LibLpHawkKeys
import com.angcyo.library.ex.*
import com.angcyo.library.toastQQ
import com.angcyo.library.unit.IValueUnit.Companion.MM_UNIT
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

    /**初始化指令, 失败后重试的次数*/
    var INIT_RETRY_COUNT = 3

    //region ---产品名称---

    //1为450nm激光, 波长

    //---焦距 200mm
    const val LI = "LP1"               //"LI"
    const val LI_Z = "LP1-Z"           //"LI-Z"       //spp 100*100mm
    const val LI_PRO = "LP1-Pro"       //"LI-PRO"     //spp 100*100mm
    const val LI_Z_PRO = "LP1-Z-Pro"   //"LI-Z-PRO"   //spp 100*100mm

    //---焦距 110mm
    const val LII = "LP2"           //"LII"             //spp 100*100mm
    const val LI_Z_ = "LP1-Z模块"    //"LI-Z模块"        //spp 100*100mm
    const val LII_M_ = "LP2-M模块"   //"LII-M模块"       //spp 100*100mm

    //---焦距 115mm 新 130mm
    const val LIII_YT = "LP3-YT" //"LIII-YT"       //spp 50*50mm
    const val LIII = "LP3"       //"LIII"          //spp 90*70mm 椭圆

    //---焦距 150mm
    //0为1064nm激光, LIII_MAX 改名 LIV
    const val LIII_MAX = "LP4"      //"LIV"   //spp 160mm*160mm 160*120mm 椭圆
    const val LIV = LIII_MAX

    //---焦距 40mm
    //2023-2-17  C系列改名为LX  C1重命名为LX1
    const val CI_OLD = "C1"            //"CI"               //spp 300*400mm
    const val CI = "LX1"               //"LX1"              //spp 300*400mm
    const val CII = "LX2"               //"CII"
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

    //指令返回超时最大时长, 毫秒
    const val DEFAULT_MAX_RECEIVE_TIMEOUT = 30_000L

    //设备支持的分辨率, 请优先使用dpi
    const val PX_4K: Byte = 0x01
    const val PX_2K: Byte = 0x02
    const val PX_1_3K: Byte = 0x03
    const val PX_1K: Byte = 0x04
    const val PX_0_8K: Byte = 0x05

    //推荐DPI:

    /**[PX_0_8K]*/
    const val DPI_158 = 158.75f

    /**[PX_1K]*/
    const val DPI_254 = 254f

    /**[PX_1_3K]*/
    const val DPI_317 = 317.5f

    /**L2 1.3k [PX_1_3K]*/
    const val DPI_333 = 333.33333f

    /**L3 1.3k [PX_1_3K]*/
    const val DPI_355 = 355f

    /**1.6K [PX_1_3K]*/
    const val DPI_423 = 423.33333f

    /**[PX_2K]*/
    const val DPI_508 = 508f

    /**2.5k [PX_2K]*/
    const val DPI_635 = 635f

    /**3.3k 4k [PX_4K]*/
    const val DPI_846 = 846.66666f

    /**L3 4k [PX_4K]*/
    const val DPI_889 = 889f

    /**[PX_4K]*/
    const val DPI_1016 = 1016f

    /**5K [PX_4K], 宣传8k*/
    const val DPI_1270 = 1270f

    /**10K [PX_4K]*/
    const val DPI_2540 = 2540f

    //雕刻激光类型选择
    const val LASER_TYPE_WHITE = 0x01.toByte() //1为1064nm激光 (白光-雕)

    const val LASER_TYPE_BLUE = 0x00.toByte() //0为450nm激光 (蓝光-烧)

    /**默认指令初始化配置*/
    val initCommandFlowList = mutableListOf<Byte>().apply {
        add(QueryCmd.settingState.state)
        add(QueryCmd.version.state)
        add(QueryCmd.workState.state)
    }

    //<editor-fold desc="operate">

    /**移除所有空格*/
    fun String.trimCmd() = replace(" ", "")

    /**[hasSpace] 输出16进制字符是否需要空格*/
    fun ByteArray.checksum(length: Int = CHECK_SIZE, hasSpace: Boolean = true): String =
        sumCheck(this, length).toHexString(hasSpace)

    /**[hasSpace] 输出16进制字符是否需要空格*/
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
    fun findPxInfo(dpi: Float?, debug: Boolean = false): PxInfo {
        val _d = if (debug) "′" else ""
        return when (dpi) {
            DPI_158 -> PxInfo(DPI_158, PX_0_8K, "0.8K$_d")
            DPI_317 -> PxInfo(DPI_317, PX_1_3K, "1.3K$_d")
            DPI_333 -> PxInfo(DPI_333, PX_1_3K, "1.3K$_d")
            DPI_355 -> PxInfo(DPI_355, PX_1_3K, "1.3K$_d")
            DPI_423 -> PxInfo(DPI_423, PX_1_3K, "1.6K$_d")
            DPI_508 -> PxInfo(DPI_508, PX_2K, "2K$_d")
            DPI_635 -> PxInfo(DPI_635, PX_2K, "2.5K$_d")
            DPI_846 -> PxInfo(DPI_846, PX_4K, "4K$_d")
            DPI_889 -> PxInfo(DPI_889, PX_4K, "4K$_d")
            DPI_1016 -> PxInfo(DPI_1016, PX_4K, "4K$_d")
            DPI_1270 -> PxInfo(DPI_1270, PX_4K, "8K$_d")
            DPI_2540 -> PxInfo(DPI_2540, PX_4K, "10K$_d")
            else -> PxInfo(DPI_254, PX_1K, "1K$_d")
        }
    }
    //PxInfo(dpi ?: DPI_254, vmApp<LaserPeckerModel>().productInfoData.value ?.widthPhys ?: 100)

    /**缩放图片到[px]指定的宽高*/
    fun bitmapScale(
        bitmap: Bitmap,
        dpi: Float,
        productInfo: LaserPeckerProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value
    ): Bitmap {
        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height

        if (productInfo == null) {
            return bitmap.scale(bitmapWidth, bitmapHeight)
        }

        val bounds = productInfo.bounds
        val productWidth = bounds.width()
        val productHeight = bounds.height()

        var newWidth = bitmapWidth
        var newHeight = bitmapHeight

        val pxInfo = findPxInfo(dpi)
        if (bitmapWidth > 1) {
            val scaleWidth = bitmapWidth * 1f / productWidth
            val width = pxInfo.devicePxWidth(productInfo)
            newWidth = (width * scaleWidth).floor().toInt()
        }

        if (bitmapHeight > 1) {
            val scaleHeight = bitmapHeight * 1f / productHeight
            val height = pxInfo.devicePxHeight(productInfo)
            newHeight = (height * scaleHeight).floor().toInt()
        }

        //图片缩放到指定宽高
        return bitmap.scale(newWidth, newHeight)
    }

    /**
     * 根据固件软件版本号[softwareVersion], 解析出对应的产品信息.
     * 解析产品信息
     * [com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd.Companion.getBoundsPath]
     * [com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd.Companion.getLimitPath]
     *
     * 根据软件版本号, 解析成产品名称
     * Ⅰ、Ⅱ、Ⅲ、Ⅳ、Ⅴ、Ⅵ、Ⅶ、Ⅷ、Ⅸ、Ⅹ、Ⅺ、Ⅻ...
     * https://docs.qq.com/sheet/DT0htVG9tamZQTFBz
     * */
    fun parseProductInfo(
        softwareVersion: Int,
        hardwareVersion: Int,
        center: Boolean? = null
    ): LaserPeckerProductInfo? {
        val configList = LaserPeckerConfigHelper.readDeviceConfig() ?: return null
        val configBean = configList.find { bean ->
            VersionMatcher.matches(
                softwareVersion,
                bean.softwareVersionRange,
                false
            ) && VersionMatcher.matches(hardwareVersion, bean.hardwareVersionRange, true)
        } ?: return null

        //L.i("匹配到配置:$configBean")

        val name = configBean.name ?: "Unknown"

        val mmValueUnit = MM_UNIT
        val bounds = RectF()
        val previewBounds = RectF()
        val carPreviewBounds = RectF()
        val penBounds = RectF()

        val limitPath = Path()
        val zLimitPath = Path()
        val rLimitPath = Path()
        val sLimitPath = Path()
        val carLimitPath = Path()

        val zMax = mmValueUnit.convertValueToPixel(
            (LibLpHawkKeys.zMaxY ?: configBean.zMaxHeight).toFloat()
        ).ceil()
        val rMax = mmValueUnit.convertValueToPixel(
            (LibLpHawkKeys.rMaxY ?: configBean.rMaxHeight).toFloat()
        ).ceil()
        val sMax = mmValueUnit.convertValueToPixel(
            (LibLpHawkKeys.sMaxY ?: configBean.sMaxHeight).toFloat()
        ).ceil()
        val carMax = mmValueUnit.convertValueToPixel(
            (LibLpHawkKeys.carMaxY ?: configBean.carMaxHeight).toFloat()
        ).ceil()

        //物理尺寸宽高mm单位
        @MM
        var wPhys = configBean.widthPhys

        @MM
        var hPhys = configBean.heightPhys

        val focalDistance = configBean.focalDistance

        //物理尺寸/焦距
        when (name) {
            LI, LI_Z_ -> {
                wPhys = LibLpHawkKeys.l1Width ?: configBean.widthPhys
                hPhys = LibLpHawkKeys.l1Height ?: configBean.heightPhys
            }

            LII, LII_M_ -> {
                wPhys = LibLpHawkKeys.l2Width ?: configBean.widthPhys
                hPhys = LibLpHawkKeys.l2Height ?: configBean.heightPhys
            }

            LIII, LIII_YT -> {
                wPhys = LibLpHawkKeys.l3Width ?: configBean.widthPhys
                hPhys = LibLpHawkKeys.l3Height ?: configBean.heightPhys
            }

            LIII_MAX, LIV -> {
                //160*160
                wPhys = LibLpHawkKeys.l4Width ?: configBean.widthPhys
                hPhys = LibLpHawkKeys.l4Height ?: configBean.heightPhys
            }

            CI -> {
                wPhys = LibLpHawkKeys.c1Width ?: configBean.widthPhys
                hPhys = if (hardwareVersion == 800) LibLpHawkKeys.c1LHeight
                    ?: configBean.heightPhys else LibLpHawkKeys.c1Height
                    ?: configBean.heightPhys
            }
        }

        //bounds
        val left = mmValueUnit.convertValueToPixel(0f).floor()
        val top = mmValueUnit.convertValueToPixel(0f).floor()
        val right = mmValueUnit.convertValueToPixel(wPhys.toFloat()).ceil()
        val bottom = mmValueUnit.convertValueToPixel(hPhys.toFloat()).ceil()
        bounds.set(left, top, right, bottom)
        previewBounds.set(left, top, right, bottom)

        limitPath.addRect(bounds, Path.Direction.CW)
        zLimitPath.addRect(left, top, right, zMax, Path.Direction.CW)
        rLimitPath.addRect(left, top, right, rMax, Path.Direction.CW)
        sLimitPath.addRect(left, top, right, sMax, Path.Direction.CW)
        //C1移动平台模式限制宽度大小
        val carWPhys = configBean.carMaxWidth
        val carRight = mmValueUnit.convertValueToPixel(carWPhys.toFloat())
        carLimitPath.addRect(left, top, carRight, carMax, Path.Direction.CW)
        carPreviewBounds.set(left, top, carRight, bottom)
        //C1画笔模式限制高度大小
        val penHPhys = configBean.penMaxHeight
        val penBottom = mmValueUnit.convertValueToPixel(penHPhys.toFloat())
        penBounds.set(left, top, right, penBottom)

        //最佳预览范围设置
        if (configBean.bestWidthPhys > 0 && configBean.bestHeightPhys > 0) {
            val lOffset = (wPhys - configBean.bestWidthPhys) / 2 //mm
            val tOffset = (hPhys - configBean.bestHeightPhys) / 2 //mm
            val l = mmValueUnit.convertValueToPixel(lOffset.toFloat()).floor()
            val t = mmValueUnit.convertValueToPixel(tOffset.toFloat()).floor()
            val r = mmValueUnit.convertValueToPixel((configBean.bestWidthPhys + lOffset).toFloat())
                .ceil()
            val b =
                mmValueUnit.convertValueToPixel((configBean.bestHeightPhys + tOffset).toFloat())
                    .ceil()
            previewBounds.set(l, t, r, b)
            limitPath.rewind()
            maxOvalPath(
                l,
                t,
                r,
                b,
                configBean.validWidthRatio,
                configBean.validHeightRatio,
                limitPath
            )
        }

        //result
        return LaserPeckerProductInfo(
            softwareVersion,
            name,
            configBean.laserTypeList ?: emptyList(),
            configBean.dpiList?.filter { if (it.debug) isDebug() else true } ?: emptyList(),
            wPhys,
            hPhys,
            bounds,
            previewBounds,
            limitPath
        ).apply {
            this.isOriginCenter = false
            this.zLimitPath = zLimitPath
            this.rLimitPath = rLimitPath
            this.sLimitPath = sLimitPath
            this.carLimitPath = carLimitPath
            this.carPreviewBounds = carPreviewBounds
            this.penBounds = penBounds
            this.supportDithering = if (configBean.supportDitheringRange == null) false
            else if (configBean.supportDitheringRange.isNullOrEmpty()) true else VersionMatcher.matches(
                softwareVersion,
                configBean.supportDitheringRange,
                false
            )
            this.focalDistance = focalDistance
            this.softwareVersion = softwareVersion
            this.hardwareVersion = hardwareVersion

            this.ex = configBean.ex
            val modeList = mutableListOf<ZModel>()

            configBean.zDirList?.lowercase()?.split(",")?.let {
                if (it.contains("flat")) {
                    modeList.add(ZModel(QuerySettingParser.Z_MODEL_FLAT))
                }
                if (it.contains("car")) {
                    modeList.add(ZModel(QuerySettingParser.Z_MODEL_CAR))
                }
                if (it.contains("cylinder")) {
                    modeList.add(ZModel(QuerySettingParser.Z_MODEL_CYLINDER))
                }
            }
            this.zModeList = modeList
        }
    }

    /**动态更新产品信息, 更新C1的工作模块*/
    fun updateProductInfo(info: LaserPeckerProductInfo?, deviceState: QueryStateParser?) {
        info ?: return
        if (info.isCI()) {
            //com/angcyo/bluetooth/fsc/laserpacker/parse/QueryStateParser.kt:81
            when (deviceState?.moduleState) {
                //0 5W激光
                0 -> info.laserTypeList = listOf(
                    LaserTypeInfo(LASER_TYPE_BLUE, 450, 5f, _string(R.string.laser_type_blue))
                )
                //1 10W激光
                1 -> info.laserTypeList = listOf(
                    LaserTypeInfo(LASER_TYPE_BLUE, 450, 10f, _string(R.string.laser_type_blue))
                )
                //2 20W激光
                2 -> info.laserTypeList = listOf(
                    LaserTypeInfo(LASER_TYPE_BLUE, 450, 20f, _string(R.string.laser_type_blue))
                )
                //3 1064激光
                3 -> info.laserTypeList = listOf(
                    LaserTypeInfo(LASER_TYPE_WHITE, 1064, 2f, _string(R.string.laser_type_white))
                )
            }
        }
    }

    /**切换设备中心点
     * @return 切换是否成功*/
    fun switchDeviceCenter(): Boolean {
        val productInfoData = vmApp<LaserPeckerModel>().productInfoData
        val productInfo = productInfoData.value ?: return false
        val center = !productInfo.isOriginCenter
        val newProductInfo =
            parseProductInfo(productInfo.softwareVersion, productInfo.hardwareVersion, center)
        //赋值
        newProductInfo?.deviceName = productInfo.deviceName
        newProductInfo?.deviceAddress = productInfo.deviceAddress
        newProductInfo?.softwareVersion = productInfo.softwareVersion
        newProductInfo?.hardwareVersion = productInfo.hardwareVersion
        productInfoData.postValue(newProductInfo)
        return true
    }

    /**保持有效宽高下,  4个角用曲线连接
     * [validWidthRatio] [validHeightRatio] 有效宽高的比例*/
    fun maxOvalPath(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        validWidthRatio: Float,
        validHeightRatio: Float,
        path: Path
    ) {
        val l = left.floor()
        val t = top.floor()
        val r = right.ceil()
        val b = bottom.ceil()

        val width = r - l
        val height = b - t

        val centerX = (l + r) / 2
        val centerY = (t + b) / 2

        //一定能雕刻上的有效宽高, 在中心位置
        val validWidth = width * validWidthRatio
        val validHeight = height * validHeightRatio

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

    /**返回设备支持的分辨率列表*/
    fun findProductSupportPxList(): List<PxInfo> {
        val result = mutableListOf<PxInfo>()
        vmApp<LaserPeckerModel>().productInfoData.value?.pxList?.let {
            it.filterTo(result) {
                if (it.debug) {
                    isDebug()
                } else {
                    true
                }
            }
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
                    LaserPeckerCmdStatistics.onReceive(bean, error)
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
        val dataShareModel = vmApp<DataShareModel>()
        dataShareModel.shareStateOnceData.postValue(StateBarDrawable.STATE_ING) //通讯中

        val uuid = command.uuid
        val commandLogString = command.toCommandLogString()
        "发送指令:$address->$uuid $commandLogString".writeBleLog()

        val func = command.commandFunc().toInt()
        val state = if (command is QueryCmd) {
            command.state.toInt()
        } else {
            null
        }
        CommandEntity().apply {
            this.uuid = uuid
            this.command = command.toHexCommandString().removeAll()
            this.func = func
            this.state = state
            this.address = address
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
            //通讯结束
            dataShareModel.shareStateOnceData.postValue(if (error == null) StateBarDrawable.STATE_NORMAL else StateBarDrawable.STATE_ERROR)

            val result = bean?.receivePacket?.toHexString(false) ?: ""
            val resultDes = result.parseResultPacketLog(func, state)
            "指令返回:${uuid}->$result\n$resultDes".writeBleLog()
            CommandEntity::class.findFirst(LPBox.PACKAGE_NAME) {
                apply(CommandEntity_.uuid.equal(uuid))
            }?.apply {
                resultTime = nowTime()
                this.result = result
                this.resultDes = "$resultDes ${error ?: ""}"
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
            deviceAddress = apiModel.lastDeviceAddress()
            if (deviceAddress.isNullOrBlank()) {
                action?.invoke(null, NoDeviceException(_string(R.string.blue_no_device_connected)))
                return null
            }
        }
        return sendCommand(
            deviceAddress,
            command,
            apiModel,
            progress,
            action
        )
    }

    /**初始化的蓝牙设备名称*/
    var initDeviceName: String? = null

    /**初始化的蓝牙设备地址
     * [lastDeviceAddress]*/
    var initDeviceAddress: String? = null

    /**发送初始化的指令, 读取设备的基础信息
     * [name] 蓝牙设备的名称
     * [address] 蓝牙设备的地址
     * [isAutoConnect] 是否是自动连接触发的初始化
     * [count] 重试的次数
     * */
    fun sendInitCommand(
        name: String,
        address: String,
        isAutoConnect: Boolean,
        count: Int = 0,
        end: (Throwable?) -> Unit = {}
    ) {
        val laserPeckerModel = vmApp<LaserPeckerModel>()
        val deviceStateModel = vmApp<DeviceStateModel>()

        initDeviceName = name
        initDeviceAddress = address

        var queryStateParser: QueryStateParser? = null
        flow { chain ->
            //读取设备工作状态, 优先判断设备是否正忙, 异常
            if (initCommandFlowList.contains(QueryCmd.workState.state)) {
                sendCommand(address, QueryCmd.workState) { bean, error ->
                    if (bean != null) {
                        bean.parse<QueryStateParser>()?.let {
                            if (it.usbConnect == QueryStateParser.CONNECT_TYPE_USB) {
                                //设备被USB占用, 则断开设备
                                if (!isAutoConnect || !RBackground.isBackground()) {
                                    //toastQQ(_string(R.string.device_busy_tip))
                                    laserPeckerModel.deviceBusyOnceData.postValue(true)
                                }
                                doBack {
                                    vmApp<FscBleApiModel>().disconnectAll()
                                }
                                chain(InterruptedException())//被中断
                            } else {
                                queryStateParser = it//保存数据, 但是不通知观察者
                                chain(error)
                            }
                        }.elseNull {
                            chain(error)
                        }
                    } else {
                        chain(error)
                    }
                }
            } else {
                chain(null)
            }
        }.flow { chain ->
            //读取设备版本, 解析产品信息
            if (initCommandFlowList.contains(QueryCmd.version.state)) {
                sendCommand(address, QueryCmd.version) { bean, error ->
                    bean?.let {
                        it.parse<QueryVersionParser>()?.let {
                            laserPeckerModel.updateDeviceVersion(it)
                        }
                    }
                    chain(error)
                }
            } else {
                chain(null)
            }
        }.flow { chain ->
            //读取设备设置状态
            if (initCommandFlowList.contains(QueryCmd.settingState.state)) {
                sendCommand(address, QueryCmd.settingState) { bean, error ->
                    bean?.let {
                        it.parse<QuerySettingParser>()?.let {
                            laserPeckerModel.updateDeviceSettingState(it)
                        }
                    }
                    chain(error)
                }
            } else {
                chain(null)
            }
        }.flow { chain ->
            //通知设备工作状态
            queryStateParser?.let {
                deviceStateModel.updateDeviceState(it)
            }
            chain(null)
        }.start {
            laserPeckerModel.initializeData.postValue(it == null)
            laserPeckerModel.initializeOnceData.postValue(it == null)
            if (it == null || it is InterruptedException) {
                //初始化完成
                end(it)
            } else if (count < INIT_RETRY_COUNT) {
                toastQQ(_string(R.string.device_init_fail_tip))
                //再来一次
                doBack {
                    sendInitCommand(name, address, isAutoConnect, count + 1, end)
                }
            } else {
                end(it)
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

    /**支持的固件版本范围*/
    fun supportFirmwareRange(): String? {
        return LibLpHawkKeys.lpSupportFirmware ?: if (isDebug()) {
            LaserPeckerConfigHelper.readDeviceSettingConfig()?.lpSupportFirmwareDebug
        } else {
            LaserPeckerConfigHelper.readDeviceSettingConfig()?.lpSupportFirmware
        }
    }

    /**当前的固件版本, 是否支持使用app*/
    fun isSupportFirmware(): Boolean {
        val productInfo = vmApp<LaserPeckerModel>().productInfoData.value ?: return true
        val version = productInfo.softwareVersion //固件版本
        val lpSupportFirmware = supportFirmwareRange()
        return VersionMatcher.matches(version, lpSupportFirmware)
    }

    /**设备地址*/
    fun lastDeviceAddress(): String? = initDeviceAddress

    //</editor-fold desc="packet">
}

/**当前矩形, 是否超出了设备物理雕刻范围*/
fun RectF?.isOverflowProductBounds() = EngravePreviewCmd.adjustRectRange(this).isOverflowBounds

/**将日志写入到[ble.log]
 * [log] 是否还需要输出到控制台
 * [String.writeErrorLog]
 * */
fun CharSequence.writeBleLog(logLevel: Int = L.DEBUG) = writeToLog(LogFile.ble, logLevel)

/**写入雕刻日志, 记录数据传输的索引及信息和雕刻的索引及信息*/
fun CharSequence.writeEngraveLog(logLevel: Int = L.DEBUG) = writeToLog(LogFile.engrave, logLevel)