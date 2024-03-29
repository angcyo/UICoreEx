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
import com.angcyo.http.tcp.TcpDevice
import com.angcyo.library.L
import com.angcyo.library.annotation.MM
import com.angcyo.library.annotation.Pixel
import com.angcyo.library.component.RBackground
import com.angcyo.library.component.VersionMatcher
import com.angcyo.library.component.flow
import com.angcyo.library.component.hawk.LibLpHawkKeys
import com.angcyo.library.component.pool.acquireTempMatrix
import com.angcyo.library.component.pool.release
import com.angcyo.library.ex.*
import com.angcyo.library.toastQQ
import com.angcyo.library.unit.IValueUnit.Companion.MM_UNIT
import com.angcyo.library.utils.LogFile
import com.angcyo.objectbox.findFirst
import com.angcyo.objectbox.findLast
import com.angcyo.objectbox.laser.pecker.LPBox
import com.angcyo.objectbox.laser.pecker.entity.CommandEntity
import com.angcyo.objectbox.laser.pecker.entity.CommandEntity_
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity_
import com.angcyo.objectbox.laser.pecker.lpSaveEntity
import com.angcyo.viewmodel.updateValue
import com.pixplicity.sharp.Sharp
import kotlin.math.max

/**
 * https://docs.qq.com/doc/DWE1MVnVOQ3RJSXZ1
 * 产品型号/参数表
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/24
 */
object LaserPeckerHelper {

    /**设备类型:无*/
    const val DEVICE_TYPE_NONE = 0

    /**设备类型:蓝牙*/
    const val DEVICE_TYPE_BLE = 1

    /**设备类型:WIFI*/
    const val DEVICE_TYPE_WIFI = 2

    /**设备类型:Http*/
    const val DEVICE_TYPE_HTTP = 3

    //---

    /**初始化指令, 失败后重试的次数*/
    var INIT_RETRY_COUNT = 3

    /**填充图层id*/
    const val LAYER_FILL = "layerFill"

    /**图片图层id*/
    const val LAYER_PICTURE = "layerPicture"

    /**线条图层id*/
    const val LAYER_LINE = "layerLine"

    /**切割图层id*/
    const val LAYER_CUT = "layerCut"

    //region ---产品名称---

    //1为450nm激光, 波长

    //罗马数字
    //https://w3.iams.sinica.edu.tw/lab/wbtzeng/labtech/roman_number.htm

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
    const val LV = "LP5" //2023-7-31 120*160mm 椭圆

    //---焦距 40mm
    //2023-2-17  C系列改名为LX  C1重命名为LX1
    const val CI_OLD = "C1"            //"CI"               //spp 300*400mm
    const val CI = "LX1"               //"LX1"              //spp 300*400mm
    const val LX2 = "LX2"               //"CII"
    const val UNKNOWN = "Unknown"

    //endregion ---产品名称---

    /**蓝牙名称前缀, 只显示指定前缀的蓝牙设备*/
    const val PRODUCT_PREFIX = "LaserPecker"

    /**固定数据头, 数据长度用1个字节表示*/
    const val PACKET_HEAD = "AABB"

    /**大数据的数据头, 数据长度用4个字节表示*/
    const val PACKET_HEAD_BIG = "AACC"

    //固定文件头的字节数量
    const val PACKET_FILE_HEAD_SIZE = 64

    //固定头的字节数
    val packetHeadSize: Int
        get() = PACKET_HEAD.toHexByteArray().size

    //校验位的占用字节数量
    const val CHECK_SIZE = 2

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
    /**[com.angcyo.bluetooth.fsc.laserpacker.data.LaserTypeInfo]*/
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

        /* 逐Byte添加位数和  */
        for (byteMsg in bytes) {
            val num = if (byteMsg.toLong() >= 0) byteMsg.toLong() else byteMsg.toLong() + 256
            sum += num
        }
        /* end of for (byte byteMsg : msg)  */

        /* 位数和转化为Byte数组  */
        for (count in 0 until length) {
            result[length - count - 1] = (sum shr count * 8 and 0xff).toByte()
        }
        /* end of for (int liv_Count = 0; liv_Count < length; liv_Count++)  */
        return result
    }

    /**查找[PxInfo]*/
    fun findPxInfo(layerId: String, dpi: Float?, debug: Boolean = false): PxInfo {
        //0: 每个图层对应的dpi信息不一样, 所以
        val list = findProductLayerSupportPxList(layerId)
        val find = list.find { it.dpi == dpi }
        if (find != null) {
            return find
        }

        //1.
        vmApp<LaserPeckerModel>().productInfoData.value?.pxList?.forEach {
            if (it.dpi == dpi) {
                return it
            }
        }


        //2.
        val configList = LaserPeckerConfigHelper.readDeviceConfig()
        if (!configList.isNullOrEmpty()) {
            for (config in configList) {
                val pxInfo = config.getLayerConfig(layerId).dpiList?.find { it.dpi == dpi }
                if (pxInfo != null) {
                    return pxInfo
                }
            }
        }

        //3.
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

    fun isBleDevice(deviceName: String?): Boolean {
        return !isWifiDevice(deviceName) && !isHttpDevice(deviceName);
    }

    /**指定的蓝牙设备名, 是否是wifi设备, wifi设备使用socket传输
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel.isWifiProduct]
     * */
    fun isWifiDevice(deviceName: String?): Boolean {
        //LP5 才有WIFI模块
        return deviceName?.startsWith(LV) == true
    }

    /**
     * LX2之后, 都是http请求的设备
     * */
    fun isHttpDevice(deviceName: String?): Boolean {
        //LP5 LX2 才有WIFI模块
        return deviceName?.startsWith(LX2) == true
    }

    /**指定的蓝牙设备名, 是否具有USB存储功能
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel.isHaveUsbProduct]
     * */
    fun isHaveUsbDevice(deviceName: String?): Boolean {
        //LP5 才有USB模块
        return deviceName?.startsWith(LV) == true
    }

    //PxInfo(dpi ?: DPI_254, vmApp<LaserPeckerModel>().productInfoData.value ?.widthPhys ?: 100)

    /**缩放图片到[px]指定的宽高*/
    fun bitmapScale(
        bitmap: Bitmap,
        layerId: String,
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

        val pxInfo = findPxInfo(layerId, dpi)
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

        //最小一个像素, 否则会崩溃
        newWidth = max(1, newWidth)
        newHeight = max(1, newHeight)

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

        fun maxValueOf(value: Int, def: Int): Float {
            val v = if (value <= 0) {
                def
            } else {
                value
            }
            return mmValueUnit.convertValueToPixel(v.toFloat()).ceil()
        }

        //物理尺寸宽高mm单位
        @MM
        var wPhys = configBean.widthPhys
        var hPhys = configBean.heightPhys

        @Pixel
        val zMaxWidth = maxValueOf(configBean.zMaxWidth, wPhys)
        val rMaxWidth = maxValueOf(configBean.rMaxWidth, wPhys)
        val sMaxWidth = maxValueOf(configBean.sMaxWidth, wPhys)
        val carMaxWidth = maxValueOf(configBean.carMaxWidth, wPhys)
        val zMaxHeight = maxValueOf(configBean.zMaxHeight, hPhys)
        val rMaxHeight = maxValueOf(configBean.rMaxHeight, hPhys)
        val sMaxHeight = maxValueOf(configBean.sMaxHeight, hPhys)
        val carMaxHeight = maxValueOf(configBean.carMaxHeight, hPhys)

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
        zLimitPath.addRect(left, top, zMaxWidth, zMaxHeight, Path.Direction.CW)
        rLimitPath.addRect(left, top, rMaxWidth, rMaxHeight, Path.Direction.CW)
        sLimitPath.addRect(left, top, sMaxWidth, sMaxHeight, Path.Direction.CW)
        //C1移动平台模式限制宽度大小
        carLimitPath.addRect(left, top, carMaxWidth, carMaxHeight, Path.Direction.CW)
        carPreviewBounds.set(left, top, carMaxWidth, bottom)
        //C1画笔模式限制高度大小
        val penHPhys = configBean.penMaxHeight
        val penBottom = mmValueUnit.convertValueToPixel(penHPhys.toFloat())
        penBounds.set(left, top, right, penBottom)

        //最佳预览范围设置
        if (!configBean.bestPhysPath.isNullOrBlank()) {
            //path
            loadStringPath(configBean.bestPhysPath, previewBounds)?.let { limitPath.set(it) }
        } else if (configBean.bestWidthPhys > 0 && configBean.bestHeightPhys > 0) {
            val lOffset = (wPhys - configBean.bestWidthPhys) / 2 //mm
            val tOffset = (hPhys - configBean.bestHeightPhys) / 2 //mm
            val l = mmValueUnit.convertValueToPixel(lOffset.toFloat()).floor()
            val t = mmValueUnit.convertValueToPixel(tOffset.toFloat()).floor()
            val r = mmValueUnit.convertValueToPixel((configBean.bestWidthPhys + lOffset).toFloat())
                .ceil()
            val b = mmValueUnit.convertValueToPixel((configBean.bestHeightPhys + tOffset).toFloat())
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
            configBean.dpiList?.filterPxList() ?: emptyList(),
            wPhys,
            hPhys,
            bounds,
            previewBounds,
            limitPath
        ).apply {
            this.isOriginCenter = false
            this.zTipPath = loadStringPath(configBean.zTipPath)
            this.rTipPath = loadStringPath(configBean.rTipPath)
            this.sTipPath = loadStringPath(configBean.sTipPath)
            this.sRepTipPath = loadStringPath(configBean.sRepTipPath)
            this.tipPath = loadStringPath(configBean.tipPath)
            this.carTipPath = loadStringPath(configBean.carTipPath)
            this.penTipPath = loadStringPath(configBean.penTipPath)
            this.zLimitPath = zLimitPath
            this.rLimitPath = rLimitPath
            this.sLimitPath = sLimitPath
            this.sRepLimitPath = sLimitPath
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
            this.deviceConfigBean = configBean
        }
    }

    /**加载mm 对应的path, 返回像素单位的数据*/
    @Pixel
    private fun loadStringPath(pathStr: String?, bounds: RectF? = null): Path? {
        if (pathStr.isNullOrEmpty()) {
            return null
        }
        val mmValueUnit = MM_UNIT
        val path = Sharp.loadPath(pathStr) //里面是mm单位
        val scaleMatrix = acquireTempMatrix()
        val scale = mmValueUnit.convertValueToPixel(1f)
        scaleMatrix.setScale(scale, scale)//缩放到像素单位
        path.transform(scaleMatrix)
        if (bounds != null) {
            path.computePathBounds(bounds)
        }
        scaleMatrix.release()
        return path
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
    private fun findProductSupportPxList(): List<PxInfo> {
        val result = mutableListOf<PxInfo>()
        vmApp<LaserPeckerModel>().productInfoData.value?.pxList?.filterPxList(result)
        return result
    }

    /**[findProductLayerSupportPxList]*/
    fun findProductLayerSupportPxList(layerId: String = HawkEngraveKeys.lastLayerId): List<PxInfo> {
        return findProductLayerSupportPxList(listOf(layerId))
    }

    /**优先使用图层对应的分辨率列表
     * [layerIdList] 图层集合, 找到后即返回*/
    fun findProductLayerSupportPxList(layerIdList: List<String>): List<PxInfo> {
        if (layerIdList.isEmpty()) {
            //no op
        } else {
            val productInfo = vmApp<LaserPeckerModel>().productInfoData.value

            for (layerId in layerIdList) {
                val dpiList = productInfo?.findLayerConfig(layerId)?.dpiList
                if (!dpiList.isNullOrEmpty()) {
                    return dpiList.filterPxList()
                }
            }
        }

        return findProductSupportPxList()
    }

    /**返回设备支持的光源列表*/
    fun findProductSupportLaserTypeList(): List<LaserTypeInfo> {
        val result = mutableListOf<LaserTypeInfo>()
        vmApp<LaserPeckerModel>().productInfoData.value?.laserTypeList?.filter {
            it.type >= 0
        }?.let {
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
        "发送指令[$address]:${uuid}:${command.getReceiveTimeout()}ms->$commandLogString".apply {
            writeBleLog()
            dataShareModel.shareTextOnceData.postValue(this) //指令日志
        }

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
            buildString {
                append("指令返回:${uuid}->")
                if (error == null) {
                    append("$result\n")
                    append(resultDes)
                } else {
                    append("\n$error")
                }
            }.apply {
                writeBleLog()
                dataShareModel.shareTextOnceData.postValue(this) //指令日志
            }
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
     * [com.angcyo.bluetooth.fsc.laserpacker.command.sendCommand]
     * */
    fun sendCommand(
        command: ICommand,
        address: String? = null,
        progress: ISendProgressAction? = null,
        action: IReceiveBeanAction? = null
    ): WaitReceivePacket? {
        val apiModel = vmApp<FscBleApiModel>()
        var deviceAddress = address
        if (deviceAddress.isNullOrEmpty() && !WifiApiModel.useWifi()) {
            deviceAddress = apiModel.lastDeviceAddress()
            if (deviceAddress.isNullOrBlank()) {
                action?.invoke(null, NoDeviceException())
                return null
            }
        }
        return sendCommand(
            deviceAddress ?: HawkEngraveKeys.lastWifiIp ?: LibLpHawkKeys.wifiAddress ?: "localhost",
            command,
            apiModel,
            progress,
            action
        )
    }

    /**初始化的蓝牙设备名称*/
    var initDeviceName: String? = null

    /**初始化的蓝牙设备地址, 也有可能是wifi地址
     * [lastDeviceAddress]
     * [com.angcyo.library.component.hawk.LibLpHawkKeys.wifiAddress]
     * */
    var initDeviceAddress: String? = null

    /**初始化的时候, 设备是否正忙*/
    var isInitDeviceBusy = false

    /**发送初始化的指令, 读取设备的基础信息
     * [name] 蓝牙设备的名称
     * [address] 蓝牙设备的地址
     * [isAutoConnect] 是否是自动连接触发的初始化
     * [count] 重试的次数
     *
     * [com.angcyo.laserpacker.device.MaterialHelper.getProductMaterialList]
     * */
    fun sendInitCommand(
        name: String,
        address: String,
        isAutoConnect: Boolean,
        count: Int = 0,
        end: (Throwable?) -> Unit = {}
    ) {
        val isHttpDevice = name.deviceType == DEVICE_TYPE_HTTP
        isInitDeviceBusy = false

        val laserPeckerModel = vmApp<LaserPeckerModel>()
        val deviceStateModel = vmApp<DeviceStateModel>()

        initDeviceName = name
        initDeviceAddress = address

        var queryStateParser: QueryStateParser? = null
        flow { chain ->
            //读取设备工作状态, 优先判断设备是否正忙, 异常
            if (initCommandFlowList.contains(QueryCmd.workState.state)) {
                if (isHttpDevice) {
                    vmApp<HttpApiModel>().fetchServerInfo(address.toLocal) { bean, error ->
                        chain(error)
                    }
                } else {
                    sendCommand(address, QueryCmd.workState) { bean, error ->
                        if (bean != null) {
                            bean.parse<QueryStateParser>()?.let {
                                if (it.usbConnect == QueryStateParser.CONNECT_TYPE_USB) {
                                    //设备被USB占用, 则断开设备
                                    if (!isAutoConnect || !RBackground.isBackground()) {
                                        isInitDeviceBusy = true
                                        if (!laserPeckerModel.deviceBusyOnceData.hasObservers()) {
                                            toastQQ(_string(R.string.device_busy_tip))
                                        }
                                        laserPeckerModel.deviceBusyOnceData.postValue(true)
                                    }
                                    doBack {
                                        vmApp<FscBleApiModel>().disconnectAll()
                                    }
                                    chain(InterruptedException())//被中断
                                } else {
                                    queryStateParser = it//保存数据, 但是不通知观察者

                                    //2023-8-11 单模块设备需要提前知道设备模块信息
                                    queryStateParser?.let {
                                        deviceStateModel.deviceStateData.updateValue(
                                            queryStateParser
                                        )
                                    }

                                    chain(error)
                                }
                            }.elseNull {
                                chain(error)
                            }
                        } else {
                            chain(error)
                        }
                    }
                }
            } else {
                chain(null)
            }
        }.flow { chain ->
            //读取设备版本, 解析产品信息
            if (!isHttpDevice && initCommandFlowList.contains(QueryCmd.version.state)) {
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
            if (!isHttpDevice && initCommandFlowList.contains(QueryCmd.settingState.state)) {
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
                //初始化完成, 或者被中断
                end(it)
            } else if (count < INIT_RETRY_COUNT) {
                toastQQ(_string(R.string.device_init_fail_tip))
                //再来一次
                doBack {
                    sendInitCommand(name, address, isAutoConnect, count + 1, end)
                }
            } else {
                //初始化失败, 断开蓝牙
                doBack {
                    vmApp<FscBleApiModel>().disconnectAll()
                }
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
            _deviceSettingBean?.lpSupportFirmwareDebug ?: _deviceSettingBean?.lpSupportFirmware
        } else {
            _deviceSettingBean?.lpSupportFirmware
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

    /**获取最后一次的气泵参数*/
    fun getLastPump(
        layerId: String?,
        productName: String? = _productName
    ): Int {
        val last = EngraveConfigEntity::class.findLast(LPBox.PACKAGE_NAME) {
            apply(
                EngraveConfigEntity_.productName.equal("$productName")
                    .and(EngraveConfigEntity_.layerId.equal(layerId ?: ""))
            )
        }
        return maxOf(last?.pump ?: 0, 0)
    }

    /**获取推荐的气泵参数*/
    fun getRecommendPump(layerId: String?, depth: Int): Int? {
        val list = _deviceConfigBean?.pumpMap?.get(layerId) ?: return null
        //气泵参数推荐
        return if (depth <= 40) {
            list.firstOrNull()?.value
        } else {
            list.lastOrNull()?.value
        }
    }

    //</editor-fold desc="packet">
}

/**当前连接的产品名称*/
val _productName: String?
    get() = vmApp<LaserPeckerModel>().productInfoData.value?.name

/**当前矩形, 是否超出了设备物理雕刻范围*/
fun RectF?.isOverflowProductBounds() = EngravePreviewCmd.adjustRectRange(this).run {
    overflowType.isOverflowBounds() || (HawkEngraveKeys.enableDataBoundsStrict && overflowType.isOverflowLimit())
}

/**将日志写入到[ble.log]
 * [log] 是否还需要输出到控制台
 * [String.writeErrorLog]
 * */
fun CharSequence.writeBleLog(logLevel: Int = L.DEBUG) = writeToLog(LogFile.ble, logLevel)

/**写入雕刻日志, 记录数据传输的索引及信息和雕刻的索引及信息*/
fun CharSequence.writeEngraveLog(logLevel: Int = L.DEBUG) = writeToLog(LogFile.engrave, logLevel)

/**过滤分辨率列表*/
fun List<PxInfo>.filterPxList(result: MutableList<PxInfo> = mutableListOf()) =
    filterTo(result) {
        if (it.debug) {
            isDebug()
        } else {
            true
        }
    }

/**mDns服务的请求地址*/
val String.toLocal: String
    get() = if (isHttpScheme()) this else "http://${this}.local"

/**当前的字符串是否是ip*/
val String.isIp: Boolean
    get() = count { it == '.' } >= 3

/**访问主机*/
val TcpDevice.host: String
    get() = if (address.isIp || address.contains("local")) address else when (deviceType) {
        LaserPeckerHelper.DEVICE_TYPE_HTTP -> "http://${address}.local"
        LaserPeckerHelper.DEVICE_TYPE_WIFI -> "${address}.local"
        else -> address
    }

/**从设备名中获取设备类型信息*/
val TcpDevice.deviceType: Int
    get() = deviceName?.deviceType ?: LaserPeckerHelper.DEVICE_TYPE_NONE

val TcpDevice.deviceTypeName: String
    get() = when (deviceType) {
        LaserPeckerHelper.DEVICE_TYPE_HTTP -> "Http设备"
        LaserPeckerHelper.DEVICE_TYPE_WIFI -> "Wifi设备"
        else -> "不支持的设备:${deviceName}"
    }

val String.deviceType: Int
    get() = if (LaserPeckerHelper.isHttpDevice(this)) {
        LaserPeckerHelper.DEVICE_TYPE_HTTP
    } else if (LaserPeckerHelper.isWifiDevice(this)) {
        LaserPeckerHelper.DEVICE_TYPE_WIFI
    } else {
        LaserPeckerHelper.DEVICE_TYPE_NONE
    }