package com.angcyo.engrave

import com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.library.ex._string

/**
 * 扩展
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/20
 */

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

/**将数据模式转换成雕刻类型
 * 数据模式:
 * [com.angcyo.canvas.utils.CanvasConstant.DATA_MODE_BLACK_WHITE]
 * [com.angcyo.canvas.utils.CanvasConstant.DATA_MODE_GCODE]
 * [com.angcyo.canvas.utils.CanvasConstant.DATA_MODE_DITHERING]
 * [com.angcyo.canvas.utils.CanvasConstant.DATA_MODE_GREY]
 *
 * 雕刻类型:
 * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_BITMAP_DITHERING]
 * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_GCODE]
 * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_BITMAP_PATH]
 * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_BITMAP]
 * */
fun Int.toEngraveTypeOfDataMode() = when (this) {
    CanvasConstant.DATA_MODE_BLACK_WHITE -> DataCmd.ENGRAVE_TYPE_BITMAP_PATH
    CanvasConstant.DATA_MODE_GCODE -> DataCmd.ENGRAVE_TYPE_GCODE
    CanvasConstant.DATA_MODE_GREY -> DataCmd.ENGRAVE_TYPE_BITMAP
    else -> DataCmd.ENGRAVE_TYPE_BITMAP_DITHERING
}

/**将雕刻类型字符串化*/
fun Int.toEngraveDataTypeStr() = when (this) {
    DataCmd.ENGRAVE_TYPE_BITMAP_DITHERING -> "抖动"
    DataCmd.ENGRAVE_TYPE_GCODE -> "GCode"
    DataCmd.ENGRAVE_TYPE_BITMAP_PATH -> "图片线段"
    DataCmd.ENGRAVE_TYPE_BITMAP -> "图片"
    else -> "未知"
}