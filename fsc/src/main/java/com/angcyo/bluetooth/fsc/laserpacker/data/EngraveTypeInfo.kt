package com.angcyo.bluetooth.fsc.laserpacker.data

import com.angcyo.library.extend.IToText
import com.angcyo.library.extend.IToValue

/**
 * 雕刻的数据模式
 *
 * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_BITMAP]
 * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_GCODE]
 * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_BITMAP_DITHERING]
 * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_BITMAP_PATH]
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/08/29
 */
data class EngraveTypeInfo(
    /**数据类型*/
    val type: Int,
    /**显示的标签*/
    val label: String
) : IToText, IToValue {

    override fun toText(): CharSequence? = label

    override fun toValue(): Any? = type

}
