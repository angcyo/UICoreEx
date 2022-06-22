package com.angcyo.bluetooth.fsc.laserpacker.data

import android.graphics.Path
import android.graphics.RectF
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerProduct

/**
 * 物理产品的一些配置信息
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/30
 */
data class ProductInfo(

    /**
     * 固件软件版本号
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel.updateDeviceVersion]
     * */
    val version: Int,
    /**
     * 产品名称
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerProduct.parseProductName]
     * */
    val name: String,
    /**机器物理的范围, 像素*/
    val bounds: RectF,
    /**机器在移动范围内的可打印范围, 像素*/
    val limitPath: Path,
    /**机器的中心点, 是否在中心, 否则就是在左上角
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerProduct.parseProductInfo]*/
    val isOriginCenter: Boolean,
) {

    /**
     * 是否是LI的设备
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerProduct.LI]
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerProduct.LII]
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerProduct.LIII]*/
    fun isLI(): Boolean {
        val version = "$version"
        return version.startsWith("1") || version.startsWith("25") || version.startsWith("41")
    }

    /**[com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerProduct.LIII_MAX]*/
    fun isLIIIMax(): Boolean = name == LaserPeckerProduct.LIII_MAX
}
