package com.angcyo.objectbox.laser.pecker.entity

import androidx.annotation.Keep
import com.angcyo.library.ex._string
import com.angcyo.library.ex.uuid
import com.angcyo.library.extend.IToText
import com.angcyo.library.getAppString
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

/**
 * 材质参数配置.
 *
 * 通过[product] [dataMode] [px] 查找出对应的 [power] 和 [depth]
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/11
 */

@Keep
@Entity
data class MaterialEntity(
    @Id var entityId: Long = 0L,

    /**雕刻任务所使用的材质数据*/
    var taskId: String? = null,

    /**材质的代码, 唯一标识代码*/
    var code: String = uuid(),

    /**材质资源的id, 用来界面显示*/
    var resId: Int = 0,

    /**[resId] 资源id在Android上存放的名称, 通过名称可以动态获取[resId]
     * 然后通过资源id, 获取国际化的本地资源*/
    var resIdStr: String? = null,

    /**等同于[resIdStr]*/
    var key: String? = null,

    /**强制显示的材质名称, 不指定则使用[resId]*/
    var name: String? = null,

    //---过滤参数----

    /**产品名, 如果此值有值, 通常是用户自定义的材质
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.LIV]
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.CI]
     * */
    var productName: String? = null,

    /**当前的配置, 属于那个图层.
     * 图片一个推荐参数, 黑白和GCode参数相同
     * [com.angcyo.engrave.data.EngraveLayerInfo]
     *
     * [com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity.layerMode]
     * */
    var layerMode: Int = -1,

    /**[layerMode] 用字符串包含的方式匹配*/
    var layerModeStr: String? = null,

    /**dpi 缩放的比例,
     * 1K 2K 4K 对应 缩放比例 1 2 4*/
    var dpiScale: Float = 0f,

    //---核心配置参数---

    /**对应的激光类型, 蓝光/白光
     * [LaserPeckerHelper.LASER_TYPE_WHITE] 0x01
     * [LaserPeckerHelper.LASER_TYPE_BLUE] 0x00
     * */
    var type: Int = 0,

    /**加速级别/雕刻精度[1~5]
     * 1: 速度快/精度低
     * 5: 速度慢/精度高
     * */
    var precision: Int = -1,

    /**功率 100% [0~100]
     * [com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity.power]
     * */
    var power: Int = 100,

    /**打印深度 10% [0~100]
     * [com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity.depth]
     * [com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd.depth]
     * */
    var depth: Int = 10,

    ) : IToText {

    companion object {

        /**雕刻速度
         * [com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd.Companion.speedToDepth]
         * */
        @Keep
        const val SPEED = "speed"
    }

    override fun toText(): CharSequence? {
        val idStr = resIdStr
        return name ?: if (idStr.isNullOrBlank()) _string(resId) else getAppString(idStr)
    }

}
