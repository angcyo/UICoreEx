package com.angcyo.bluetooth.fsc.laserpacker.data

import androidx.annotation.Keep
import com.angcyo.library.extend.IToText

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
    val type: Byte,
    /**激光波长, nm单位*/
    var wave: Int,
    /**功率 0.5W 2W 10W*/
    var power: Float,
    /**描述字符*/
    val label: String,
    /**当前对应的额模块
     * [com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel.getDeviceModuleLabel]
     * */
    val moduleState: Int = 0,
) : IToText {

    //override fun toText(): CharSequence = "${wave}nm (${label})"  //label

    override fun toText(): CharSequence = "${wave}nm"  //label
}