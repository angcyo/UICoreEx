package com.angcyo.objectbox.laser.pecker.entity

import androidx.annotation.Keep
import com.angcyo.library.ex._string
import com.angcyo.library.ex.uuid
import com.angcyo.library.extend.IToText
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

    /**材质的代码, 唯一标识代码*/
    var code: String = uuid(),

    /**材质资源的id, 用来界面显示*/
    var resId: Int = 0,

    /**强制显示的材质名称, 不指定则使用[resId]*/
    var name: String? = null,

    /**对应的产品名称, 多个使用`,`分割*/
    var product: String = "",

    /**
     * 数据模式
     * [CanvasConstant.BITMAP_MODE_BLACK_WHITE]
     * [CanvasConstant.BITMAP_MODE_GREY]
     * */
    var dataMode: Int = 3,

    /**分辨率*/
    var dpi: Float = -1f,

    /**对应的激光类型, 蓝光/白光
     * [LaserPeckerHelper.LASER_TYPE_WHITE]
     * [LaserPeckerHelper.LASER_TYPE_BLUE]
     * */
    var type: Int = 0,

    //---

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

    override fun toText(): CharSequence {
        return name ?: _string(resId)
    }

}
