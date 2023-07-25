package com.angcyo.laserpacker.bean

import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.library.annotation.MM
import com.angcyo.library.ex.toStr
import com.angcyo.library.unit.toMm
import com.angcyo.objectbox.findLast
import com.angcyo.objectbox.laser.pecker.LPBox
import com.angcyo.objectbox.laser.pecker.bean.getLayerConfig
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.MaterialEntity
import com.angcyo.objectbox.laser.pecker.entity.MaterialEntity_
import com.angcyo.objectbox.laser.pecker.entity.TransferConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.TransferConfigEntity_

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

    //---

    /**[com.angcyo.objectbox.laser.pecker.entity.MaterialEntity.layerId]*/
    var layerId: String? = null,

    /**PC端*/
    var materialId: String? = null,

    /**[com.angcyo.objectbox.laser.pecker.entity.MaterialEntity.code]*/
    var materialCode: String? = null,

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

    /**雕刻次数
     * [com.angcyo.objectbox.laser.pecker.entity.MaterialEntity.count]*/
    var printCount: Int = 1,

    /**[com.angcyo.objectbox.laser.pecker.entity.MaterialEntity.precision]*/
    var precision: Int = 0,

    /**[com.angcyo.objectbox.laser.pecker.entity.MaterialEntity.dpi]*/
    var dpi: Float = 0f,
)

fun EngraveConfigEntity.toLaserOptionsBean(): LPLaserOptionsBean {
    val bean = LPLaserOptionsBean()

    bean.layerId = layerId
    bean.materialCode = materialCode
    bean.materialKey = materialKey
    MaterialEntity::class.findLast(LPBox.PACKAGE_NAME) {
        apply(MaterialEntity_.code.equal("$materialCode"))
    }?.let {
        bean.materialName = it.toText()?.toStr()
    }

    bean.lightSource = type.toInt()
    bean.printCount = time
    bean.diameter = diameterPixel.toMm()
    bean.precision = precision
    bean.printPower = power
    bean.printDepth = depth

    TransferConfigEntity::class.findLast(LPBox.PACKAGE_NAME) {
        apply(TransferConfigEntity_.taskId.equal("$taskId"))
    }?.apply {
        bean.dpi = layerJson?.getLayerConfig(layerId)?.dpi ?: dpi
    }

    return bean
}