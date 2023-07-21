package com.angcyo.bluetooth.fsc.laserpacker.data

import android.graphics.Path
import android.graphics.RectF
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.bean.DeviceConfigBean
import com.angcyo.bluetooth.fsc.laserpacker.bean.LayerConfigBean
import com.angcyo.bluetooth.fsc.laserpacker.bean.filterModuleDpiList
import com.angcyo.library.annotation.MM
import com.angcyo.library.annotation.Pixel

/**
 * 物理产品的一些配置信息
 *
 * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.parseProductInfo]
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/30
 */
data class LaserPeckerProductInfo(

    /**
     * 固件软件版本号
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel.updateDeviceVersion]
     * */
    var version: Int,
    /**
     * 产品名称
     * 解析方法: [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.parseProductName]
     *
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.LI]
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.LII]
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.LIII]
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.LIV]
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.CI]
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.CII]
     * */
    var name: String?,

    /**设备支持的激光类型
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.LASER_TYPE_WHITE]
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.LASER_TYPE_BLUE]
     * */
    var laserTypeList: List<LaserTypeInfo> = listOf(),
    /**设备支持的分辨率
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.parseProductInfo]*/
    var pxList: List<PxInfo> = emptyList(),

    /**物理尺寸宽高, mm单位*/
    @MM
    var widthPhys: Int = 0,
    @MM
    var heightPhys: Int = 0,
    /**机器物理的范围, 像素, 在手机上的像素 */
    @Pixel
    var bounds: RectF,
    /**机器的最佳预览范围, 像素, 在手机上的像素*/
    @Pixel
    var previewBounds: RectF,
    /**机器在移动范围内的可打印范围, 像素, 在手机上的像素
     * [com.angcyo.engrave.EngraveProductLayoutHelper._showZRSLimit]
     * [com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd.Companion.getLimitPath]
     * */
    @Pixel
    var limitPath: Path,
    /**Z轴限制提示框, 像素, 在手机上的像素*/
    @Pixel
    var zLimitPath: Path? = null,
    /**旋转轴,最大提示框*/
    @Pixel
    var rLimitPath: Path? = null,
    /**滑台,最大提示框*/
    @Pixel
    var sLimitPath: Path? = null,
    /**C1平台移动/小车模式,最大提示框*/
    @Pixel
    var carLimitPath: Path? = null,
    /**C1平台移动/小车模式,最大预览范围*/
    @Pixel
    var carPreviewBounds: RectF? = null,

    /** C1画笔模块, 最大高度390mm
     * limit path 同样是这个范围
     * */
    @Pixel
    var penBounds: RectF? = null,

    //---

    /**机器的中心点, 是否在中心, 否则就是在左上角
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.parseProductInfo]*/
    var isOriginCenter: Boolean = false,

    /**物理设备的焦距, 毫米*/
    @MM
    var focalDistance: Int? = null,

    //---
    /**LaserPecker-CI001F60*/
    var deviceName: String? = null, //蓝牙设备名称
    /**DC:0D:30:00:1F:60*/
    var deviceAddress: String? = null, //蓝牙设备地址
    /**固件版本号, 等同于[version]*/
    var softwareVersion: Int = -1,
    var hardwareVersion: Int = -1,

    //---

    /**当前设备是否图片的压缩抖动数据雕刻,
     * 如果支持:则抖动算法的图片发送0x60的数据, 否则发送0x10的数据*/
    var supportDithering: Boolean = true,

    /**支持的外设*/
    var ex: String? = null,

    /**第三轴的模式列表*/
    var zModeList: List<ZModel>? = null,

    /**产品对应的配置表*/
    var deviceConfigBean: DeviceConfigBean? = null
) {

    /**
     * 是否是LI的设备
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.parseProductName]
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.LI]
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.LII]
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.LIII]*/
    fun isLI(): Boolean = name == LaserPeckerHelper.LI_Z ||
            name == LaserPeckerHelper.LI ||
            name == LaserPeckerHelper.LI_PRO ||
            isLI_Z()

    /**是否是LI-Z的设备
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.parseProductName]
     * */
    fun isLI_Z(): Boolean =
        softwareVersion in 250..299 || softwareVersion in 2500..2599 /*name == LaserPeckerHelper.LI_Z_PRO*/

    /**L2 只有蓝光*/
    fun isLII(): Boolean = name == LaserPeckerHelper.LII

    fun isLIII(): Boolean = name == LaserPeckerHelper.LIII /*|| isLIIIMax()*/

    /**
     * L3-MAX 蓝光 白光 都有
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.LIII_MAX]
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.parseProductName]
     * */
    fun isLIIIMax(): Boolean = name == LaserPeckerHelper.LIII_MAX

    /**LIV
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.LIV]
     * */
    fun isLIV(): Boolean = isLIIIMax()

    /**CI
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.CI]
     * */
    fun isCI(): Boolean = name == LaserPeckerHelper.CI

    //---

    fun isL1() = isLI()

    fun isL2() = isLII()

    fun isL3() = isLIII()

    fun isL4() = isLIV()

    fun isC1() = isCI()

    /**是否是C系列*/
    fun isCSeries() = isC1()

    /**是否是lp系列*/
    fun isLPSeries() = isL1() || isL2() || isL3() || isL4()

    fun isSupportZ() = ex?.split(",")?.contains("z") == true
    fun isSupportS() = ex?.split(",")?.contains("s") == true
    fun isSupportR() = ex?.split(",")?.contains("r") == true

    /**获取指定图层的配置信息
     * [layerId] 图层id*/
    fun findLayerConfig(layerId: String): LayerConfigBean {
        deviceConfigBean?.layer?.get(layerId)?.let {
            return it.filterDpiList()
        }
        return LayerConfigBean(pxList.filterModuleDpiList())
    }
}


