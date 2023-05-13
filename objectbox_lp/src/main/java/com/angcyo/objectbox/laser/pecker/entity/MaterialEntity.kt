package com.angcyo.objectbox.laser.pecker.entity

import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.annotation.Keep
import com.angcyo.library.ex.*
import com.angcyo.library.extend.IToDrawable
import com.angcyo.library.extend.IToRightDrawable
import com.angcyo.library.extend.IToText
import com.angcyo.library.getAppString
import com.angcyo.objectbox.laser.pecker.R
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

    /**是否被删除*/
    var isDelete: Boolean = false,

    /**雕刻任务所使用的材质数据*/
    var taskId: String? = null,

    /**材质的代码, 唯一标识代码*/
    var code: String = uuid(),

    /**材质资源的id, 用来界面显示*/
    var resId: Int = 0,

    /**[resId] 资源id在Android上存放的名称, 通过名称可以动态获取[resId]
     * 然后通过资源id, 获取国际化的本地资源*/
    var resIdStr: String? = null,

    /**等同于[resIdStr], 只不过没有国际化, 用户自定义的名称
     * 用来标识同一组材质的关键字段*/
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
     * [com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity.layerId]
     *
     * [com.angcyo.laserpacker.device.data.EngraveLayerInfo]
     * [com.angcyo.laserpacker.device.LayerHelper.getEngraveLayerList]
     * */
    var layerId: String? = null,

    /**dpi 缩放的比例,
     * 1K 1.3K 2K 4K 对应 缩放比例 1 1.3 2 4*/
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

    ) : IToText, IToDrawable, IToRightDrawable {

    companion object {

        /**雕刻速度
         * [com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd.Companion.speedToDepth]
         * */
        @Keep
        const val SPEED = "speed"

        /**根据激光类型, 创建对应的[Drawable]
         * [whiteColor] 白光的颜色*/
        fun createLaserTypeDrawable(
            type: Int?,
            whiteColor: Int = "#efefef".toColorInt()
        ): Drawable? = when (type) {
            //蓝光
            0x00 -> _drawable(R.drawable.material_laser_type_ico).tintDrawable(Color.BLUE)
            //白光
            0x01 -> _drawable(R.drawable.material_laser_type_ico).tintDrawable(whiteColor)
            else -> null
        }

        /**获取材质状态同步资源*/
        var getMaterialItemSyncStateRes: (item: MaterialEntity) -> Int? = { null }
    }

    /**是否是自定义的材质*/
    val isCustomMaterial: Boolean
        get() = !productName.isNullOrBlank()

    override fun toText(): CharSequence? {
        val idStr = resIdStr
        return name ?: if (idStr.isNullOrBlank()) _string(resId) else getAppString(idStr)
    }

    override fun toDrawable(): Drawable? {
        return if (isCustomMaterial) {
            _drawable(R.drawable.material_edit_ico)
        } else createLaserTypeDrawable(type)
    }

    override fun toRightDrawable(): Drawable? {
        val res = getMaterialItemSyncStateRes(this)
        return if (res == null || !isCustomMaterial) {
            null
        } else {
            _drawable(res)
        }
    }
}
