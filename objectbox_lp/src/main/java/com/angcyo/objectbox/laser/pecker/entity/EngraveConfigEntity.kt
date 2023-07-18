package com.angcyo.objectbox.laser.pecker.entity

import androidx.annotation.Keep
import androidx.annotation.Px
import com.angcyo.library.ex.nowTime
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

/**
 * 雕刻参数配置信息
 *
 * 比如:  线条图层/填充图层/图片图层
 *
 * 材质 功率/深度/次数
 *
 * 激光类型等
 *
 * [com.angcyo.engrave.model.EngraveModel.engraveNext]
 * [com.angcyo.engrave.model.EngraveModel._startEngraveCmd]
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

    /**当前的雕刻参数配置, 属于那个图层.
     * [com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity.layerMode]
     *
     * [com.angcyo.laserpacker.device.data.EngraveLayerInfo]
     * [com.angcyo.laserpacker.device.LayerHelper.getEngraveLayerList]
     * */
    var layerId: String? = null,

    /**材质标识
     * [com.angcyo.objectbox.laser.pecker.entity.MaterialEntity.code]*/
    var materialCode: String? = null,

    /**材质key, 用来国际化*/
    var materialKey: String? = null,

    /**产品名, 比如L4 C1等*/
    var productName: String? = null,

    /**蓝牙设备地址
     * DC:0D:30:00:1F:60*/
    var deviceAddress: String? = null,

    /**外部设备名称
     * [com.angcyo.bluetooth.fsc.laserpacker.parse.QuerySettingParser.EX_Z]
     * [com.angcyo.bluetooth.fsc.laserpacker.parse.QuerySettingParser.EX_R]
     * [com.angcyo.bluetooth.fsc.laserpacker.parse.QuerySettingParser.EX_S]
     * [com.angcyo.bluetooth.fsc.laserpacker.parse.QuerySettingParser.EX_CAR]
     * */
    var exDevice: String? = null,

    /**固件版本号, 用来识别雕刻时所用的机器*/
    var softwareVersion: Int = -1,
    var hardwareVersion: Int = -1,

    //---L4专属---

    /**雕刻激光类型选择，0为1064nm激光 (白光-雕)，1为450nm激光 (蓝光-烧)。(L3max新增)
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.LASER_TYPE_WHITE]
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.LASER_TYPE_BLUE]
     * */
    var type: Byte = -1,

    /**雕刻物体直径, 这里用像素作为单位
     * 在[com.angcyo.engrave.EngraveFlowDataHelper.generateEngraveConfig]时,会从
     * [com.angcyo.objectbox.laser.pecker.entity.PreviewConfigEntity.diameterPixel]中获取并赋值.
     *
     * [com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd.diameter]
     * [com.angcyo.engrave.data.HawkEngraveKeys.lastDiameterPixel]
     * [com.angcyo.engrave.data.HawkEngraveKeys.lastMinDiameterPixel]
     * */
    @Px
    var diameterPixel: Float = -1f,

    //---C1专属---

    /**
     * 雕刻模块识别位（C1专用位）
     * [com.angcyo.bluetooth.fsc.laserpacker.parse.QueryStateParser.moduleState]*/
    var moduleState: Int = -1,

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

    /**创建时间*/
    var createTime: Long = nowTime()
) {

    /**加速级别/雕刻精度转换成雕刻速度*/
    fun toEngravingSpeed(): Int {
        val max = 5
        val current = precision
        return ((max - current + 1) * 1f / max * 100).toInt()
    }
}
