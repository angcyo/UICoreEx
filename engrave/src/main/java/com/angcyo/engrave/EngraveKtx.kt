package com.angcyo.engrave

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.library.ex._string
import com.angcyo.library.ex.toElapsedTime

/**
 * 扩展
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/20
 */

/**分:秒 的时间格式*/
fun Long?.toEngraveTime() = this?.toElapsedTime(
    pattern = intArrayOf(-1, 1, 1),
    units = arrayOf("", "", ":", ":", ":")
)

/**模式字符串*/
fun Int?.toModeString() = when (this) {
    CanvasConstant.DATA_MODE_PRINT -> _string(R.string.canvas_prints)
    CanvasConstant.DATA_MODE_GCODE -> _string(R.string.canvas_gcode)
    CanvasConstant.DATA_MODE_BLACK_WHITE -> _string(R.string.canvas_black_white)
    CanvasConstant.DATA_MODE_DITHERING -> _string(R.string.canvas_dithering)
    CanvasConstant.DATA_MODE_GREY -> _string(R.string.canvas_grey)
    CanvasConstant.DATA_MODE_SEAL -> _string(R.string.canvas_seal)
    else -> null
}

/**激光类型字符串*/
fun Byte?.toLaserTypeString() = when (this) {
    LaserPeckerHelper.LASER_TYPE_WHITE -> _string(R.string.laser_type_white)
    else -> _string(R.string.laser_type_blue)
}