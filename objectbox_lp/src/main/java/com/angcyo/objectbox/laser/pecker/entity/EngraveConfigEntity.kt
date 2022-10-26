package com.angcyo.objectbox.laser.pecker.entity

import androidx.annotation.Keep
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

/**
 * 雕刻参数配置信息
 *
 * 比如:  线条图层/填充图层/图片图层
 *
 * 材质 功率/深度/次数
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/09
 */
@Keep
@Entity
data class EngraveConfigEntity(
    @Id var entityId: Long = 0L,

    /**当前雕刻任务的id*/
    var taskId: String? = null,

    /**当前的配置, 属于那个图层.
     * [com.angcyo.engrave.data.EngraveLayerInfo]
     *
     * [com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity.layerMode]
     * */
    var layerMode: Int = -1,

    /**材质标识
     * [com.angcyo.objectbox.laser.pecker.entity.MaterialEntity.code]*/
    var materialCode: String? = null,

    //---L4专属---

    /**雕刻激光类型选择，0为1064nm激光 (白光-雕)，1为450nm激光 (蓝光-烧)。(L3max新增)
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.LASER_TYPE_WHITE]
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.LASER_TYPE_BLUE]
     * */
    var type: Byte = -1,

    //---C1专属---

    /**加速级别/雕刻精度[1~5]
     * 1: 速度快/精度低
     * 5: 速度慢/精度高
     * */
    var precision: Int = -1,

    //---公共参数---

    /**功率 100% [0~100]*/
    var power: Int = -1,

    /**
     * 打印深度 10% [0~100]
     * [com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd.toHexCommandString]
     * */
    var depth: Int = -1,

    /**打印次数, 最大255*/
    var time: Int = 1,
) {
    fun toEngravingSpeed(): Int {
        val max = 5
        val current = precision
        return ((max - current + 1) * 1f / max * 100).toInt()
    }
}
