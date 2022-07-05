package com.angcyo.engrave

import com.angcyo.engrave.canvas.CanvasBitmapHandler
import com.angcyo.library.ex._string
import com.angcyo.library.ex.toElapsedTime

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/20
 */

/**分:秒 的时间格式*/
fun Long?.toEngraveTime() = this?.toElapsedTime(
    pattern = intArrayOf(-1, 1, 1),
    units = arrayOf("", "", ":", ":", ":")
)

/**模式字符串*/
fun Int?.getModeString() = when (this) {
    CanvasBitmapHandler.BITMAP_MODE_PRINT -> _string(R.string.canvas_prints)
    CanvasBitmapHandler.BITMAP_MODE_GCODE -> _string(R.string.canvas_gcode)
    CanvasBitmapHandler.BITMAP_MODE_BLACK_WHITE -> _string(R.string.canvas_black_white)
    CanvasBitmapHandler.BITMAP_MODE_DITHERING -> _string(R.string.canvas_dithering)
    CanvasBitmapHandler.BITMAP_MODE_GREY -> _string(R.string.canvas_grey)
    CanvasBitmapHandler.BITMAP_MODE_SEAL -> _string(R.string.canvas_seal)
    else -> null
}