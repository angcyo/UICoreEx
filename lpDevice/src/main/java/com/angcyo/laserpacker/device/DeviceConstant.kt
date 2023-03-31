package com.angcyo.laserpacker.device

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/30
 */
object DeviceConstant {

    //region ---中间数据扩展名---

    /**存放元素预览图的扩展名*/
    const val EXT_PREVIEW = ".png"

    /**存放元素转成数据后, 数据再次预览图的扩展名*/
    const val EXT_DATA_PREVIEW = ".p.png"

    /**图片路径数据*/
    const val EXT_BP = ".bp"

    /**抖动数据*/
    const val EXT_DT = ".dt"

    /**gcode数据*/
    const val EXT_GCODE = ".gcode"

    /**svg数据*/
    const val EXT_SVG = ".svg"

    //endregion ---中间数据扩展名---

    //region ---目录分配---

    /**雕刻缓存文件的文件夹*/
    const val ENGRAVE_FILE_FOLDER = "engrave"

    /**GCode/Svg矢量缓存目录*/
    const val VECTOR_FILE_FOLDER = "vector"

    /**雕刻传输数据缓存文件的文件夹*/
    const val ENGRAVE_TRANSFER_FILE_FOLDER = "transfer"

    //endregion ---目录分配---

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