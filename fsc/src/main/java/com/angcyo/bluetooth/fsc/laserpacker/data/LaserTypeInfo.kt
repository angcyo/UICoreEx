package com.angcyo.bluetooth.fsc.laserpacker.data

import androidx.annotation.Keep
import com.angcyo.library.ex.appendSpaceIfNotEmpty
import com.angcyo.library.ex.ensureInt
import com.angcyo.library.extend.IToText
import com.angcyo.library.getAppString

/**
 * 激光类型/激光光源
 *
 * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.LASER_TYPE_BLUE]
 * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.LASER_TYPE_WHITE]
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/27
 */

@Keep
data class LaserTypeInfo(
    /**激光类型, 蓝光/白光.
     * 发给机器的参数
     * 0为450nm激光
     * 1为1064nm激光
     * */
    val type: Byte = -1,
    /**激光波长, nm单位*/
    var wave: Int = -1,
    /**功率 0.5W 2W 10W*/
    var power: Float = -1f,
    /**描述字符*/
    val label: String = "",
    /**当前对应的额模块
     * [com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel.getDeviceModuleLabel]
     * */
    val moduleState: Int = -1,
    /**[label]
     * [getAppString]
     * ```
     * laser_type_blue
     * laser_type_white
     * ```
     * */
    val labelIdStr: String? = null,
    /**是否要显示气泵参数
     * 显示风速设置的开关*/
    val showPump: Boolean = false,
) : IToText {

    //override fun toText(): CharSequence = "${wave}nm (${label})"  //label

    override fun toText(): CharSequence = "${wave}nm"  //label

    /**配置文件的名称
     * [productName] 产品名称 `LP4/LX1`*/
    fun getConfigFileName(productName: String): String {
        if (wave > 0 && power > 0) {
            return "${productName}_${wave}_${power.ensureInt()}.json"
        }
        if (moduleState >= 0) {
            return "${productName}_module_${moduleState}.json"
        }
        if (type >= 0) {
            return "${productName}_type_${type}.json"
        }
        return "${productName}.json"
    }

    fun toLabel(): CharSequence = buildString {
        if (labelIdStr.isNullOrBlank()) {
            if (wave > 0) {
                appendSpaceIfNotEmpty()
                //append("${wave}nm")
                append("$wave")
            }

            if (power > 0) {
                appendSpaceIfNotEmpty()
                append("${power.ensureInt()}w")
            }
        } else {
            append(getAppString(labelIdStr))
        }
    }

    fun toDes(): CharSequence = buildString {
        if (labelIdStr.isNullOrBlank()) {
            append(label)
        } else {
            append(getAppString(labelIdStr))
        }
        if (power > 0) {
            appendSpaceIfNotEmpty()
            append("${power.ensureInt()}w")
        }
        if (wave > 0) {
            appendSpaceIfNotEmpty()
            append("${wave}nm")
        }
    }

    /**非光源模块, 比如单色笔等*/
    fun isNotLaserModule(): Boolean = wave <= 0
}