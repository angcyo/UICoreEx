package com.angcyo.engrave2

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd
import com.angcyo.engrave2.EngraveConstant.DATA_MODE_BLACK_WHITE
import com.angcyo.engrave2.EngraveConstant.DATA_MODE_DITHERING
import com.angcyo.engrave2.EngraveConstant.DATA_MODE_GCODE
import com.angcyo.engrave2.EngraveConstant.DATA_MODE_GREY
import com.angcyo.engrave2.EngraveConstant.DATA_MODE_PRINT
import com.angcyo.engrave2.EngraveConstant.DATA_MODE_SEAL
import com.angcyo.library.component.hawk.LibHawkKeys
import com.angcyo.library.ex._string
import com.angcyo.library.ex.toMinuteTime

/** 雕刻相关的常量
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/28
 */
object EngraveConstant {

    //region ---缺省值---

    /**默认的阈值*/
    val DEFAULT_THRESHOLD: Float = LibHawkKeys.grayThreshold.toFloat()

    //endregion ---缺省值---

    //region ---数据处理模式---

    /**数数据模式, 黑白, 发送线段数据
     * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_BITMAP_PATH]
     * */
    const val DATA_MODE_BLACK_WHITE = 1

    /**数据模式, 抖动, 发送抖动数据
     * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_BITMAP_DITHERING]
     * */
    const val DATA_MODE_DITHERING = 5

    /**数据模式, GCode, 发送GCode数据
     * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_GCODE]
     * */
    const val DATA_MODE_GCODE = 6

    //

    /**数据模式, 版画*/
    const val DATA_MODE_PRINT = 4

    /**数据模式, 灰度*/
    const val DATA_MODE_GREY = 3

    /**数据模式, 印章*/
    const val DATA_MODE_SEAL = 2

    //endregion ---数据处理模式---
}

/**数据模式int, 转成对应的字符串*/
fun Int.toDataModeStr() = when (this) {
    DATA_MODE_PRINT -> "版画"
    DATA_MODE_BLACK_WHITE -> "黑白"
    DATA_MODE_DITHERING -> "抖动"
    DATA_MODE_GREY -> "灰度"
    DATA_MODE_SEAL -> "印章"
    DATA_MODE_GCODE -> "GCode"
    else -> "DataMode-${this}"
}

/**将雕刻类型字符串化*/
fun Int.toEngraveDataTypeStr() = when (this) {
    DataCmd.ENGRAVE_TYPE_BITMAP_DITHERING -> "抖动"
    DataCmd.ENGRAVE_TYPE_GCODE -> "GCode"
    DataCmd.ENGRAVE_TYPE_BITMAP_PATH -> "图片线段"
    DataCmd.ENGRAVE_TYPE_BITMAP -> "图片"
    else -> "未知"
}