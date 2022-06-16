package com.angcyo.bluetooth.fsc.laserpacker.data

import android.graphics.Path
import android.graphics.RectF

/**
 * 物理产品的一些配置信息
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/30
 */
data class ProductInfo(
    val version: Int, //固件软件版本号
    val name: String, //产品名称
    val bounds: RectF, //机器物理的范围
    val limitPath: Path, //机器在移动范围内的可打印范围
    val isOriginCenter: Boolean, //机器的中心点, 是否在中心, 否则就是在左上角
) {

    /**
     * 是否是LI的设备
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerProduct.LI]
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerProduct.LII]
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerProduct.LIII]*/
    fun isLI(): Boolean {
        val str = "$version"
        return str.startsWith("1") || str.startsWith("25") || str.startsWith("41")
    }
}
