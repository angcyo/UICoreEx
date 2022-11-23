package com.angcyo.bluetooth.fsc.laserpacker.data

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
data class LaserTypeInfo(
    /**激光类型, 蓝光/白光.
     * 发给机器的参数
     * */
    val type: Byte,
    /**激光波长, nm单位*/
    val wave: Int,
    /**描述字符*/
    val label: CharSequence,
) : IToText {
    override fun toText(): CharSequence = "${wave}nm (${label})"  //label
}