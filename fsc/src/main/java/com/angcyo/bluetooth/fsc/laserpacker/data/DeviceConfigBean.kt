package com.angcyo.bluetooth.fsc.laserpacker.data

import androidx.annotation.Keep
import com.angcyo.library.annotation.MM

/**
 * 设备配置信息结构
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/11
 */

@Keep
data class DeviceConfigBean(

    var softwareVersionRange: String? = null,
    var hardwareVersionRange: String? = null,

    var dpiList: List<PxInfo>? = null,
    var laserTypeList: List<LaserTypeInfo>? = null,
    var focalDistance: Int? = 0,
    var name: String? = null,
    var des: String? = null,

    var supportDitheringRange: String? = null,
    /**设备尺寸*/
    @MM
    var widthPhys: Int = 0,
    var heightPhys: Int = 0,
    /**最佳物理尺寸*/
    @MM
    var bestWidthPhys: Int = 0,
    var bestHeightPhys: Int = 0,
    /**有效范围宽高比例, 在[bestWidthPhys] [bestHeightPhys]中*/
    var validWidthRatio: Float = 0f,
    var validHeightRatio: Float = 0f,
    @MM
    var zMaxHeight: Int = 0,
    var rMaxHeight: Int = 0,
    var sMaxHeight: Int = 0,
    var carMaxWidth: Int = 0,
    var carMaxHeight: Int = 0,
    var penMaxHeight: Int = 0,
)