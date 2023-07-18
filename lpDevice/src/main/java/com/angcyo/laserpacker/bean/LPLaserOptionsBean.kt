package com.angcyo.laserpacker.bean

import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.library.annotation.MM
import com.angcyo.library.unit.toMm
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity

/**
 * 图层参数
 *
 * [com.angcyo.objectbox.laser.pecker.entity.MaterialEntity]
 *
 * https://www.showdoc.com.cn/2057569273029235/10372848926666581
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/07/18
 */
data class LPLaserOptionsBean(

    /**版本号*/
    var version: Int = 1,

    /**旋转轴直径*/
    @MM
    var diameter: Float = HawkEngraveKeys.lastDiameterPixel.toMm(),

    /**雕刻次数*/
    var printCount: Int = 1,

    //---

    /**[com.angcyo.objectbox.laser.pecker.entity.MaterialEntity.layerId]*/
    var layerId: String? = null,

    /**[com.angcyo.objectbox.laser.pecker.entity.MaterialEntity.code]*/
    var materialId: String? = null,

    /**[com.angcyo.objectbox.laser.pecker.entity.MaterialEntity.resIdStr]*/
    var materialKey: String? = null,

    /**[com.angcyo.objectbox.laser.pecker.entity.MaterialEntity.name]*/
    var materialName: String? = null,

    /**[com.angcyo.objectbox.laser.pecker.entity.MaterialEntity.type]*/
    var lightSource: Int = 0,

    /**[com.angcyo.objectbox.laser.pecker.entity.MaterialEntity.power]*/
    var printPower: Int = 0,

    /**[com.angcyo.objectbox.laser.pecker.entity.MaterialEntity.depth]*/
    var printDepth: Int = 0,

    /**[com.angcyo.objectbox.laser.pecker.entity.MaterialEntity.precision]*/
    var precision: Int = 0,

    /**[com.angcyo.objectbox.laser.pecker.entity.MaterialEntity.dpi]*/
    var dpi: Float = 0f,
)

fun EngraveConfigEntity.toLaserOptionsBean(): LPLaserOptionsBean {
    val bean = LPLaserOptionsBean()

    bean.layerId = layerId
    bean.materialId = materialCode
    bean.materialKey = materialKey

    bean.printCount = time
    bean.lightSource = type.toInt()
    bean.diameter = diameterPixel.toMm()
    bean.precision = precision
    bean.printPower = power
    bean.printDepth = depth

    return bean
}