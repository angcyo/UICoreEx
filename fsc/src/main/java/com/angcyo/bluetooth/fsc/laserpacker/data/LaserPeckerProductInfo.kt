package com.angcyo.bluetooth.fsc.laserpacker.data

import android.graphics.Path
import android.graphics.RectF
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper

/**
 * 物理产品的一些配置信息
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
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.parseProductName]
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
    var typeList: List<Byte> = listOf(LaserPeckerHelper.LASER_TYPE_BLUE),
    /**机器物理的范围, 像素*/
    var bounds: RectF,
    /**机器在移动范围内的可打印范围, 像素*/
    var limitPath: Path,
    /**Z轴限制提示框, 像素*/
    var zLimitPath: Path? = null,
    /**机器的中心点, 是否在中心, 否则就是在左上角
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.parseProductInfo]*/
    var isOriginCenter: Boolean = false,
    var hardwareVersion: Int = -1,
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
    fun isLI_Z(): Boolean = name == LaserPeckerHelper.LI_Z_PRO

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
}


