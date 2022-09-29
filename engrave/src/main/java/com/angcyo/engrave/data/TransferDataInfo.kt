package com.angcyo.engrave.data

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd

/**
 * 待传输的数据信息
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/29
 */
data class TransferDataInfo(

    /**下位机雕刻的数据类型
     * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_BITMAP_DITHERING]
     * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_GCODE]
     * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_BITMAP_PATH]
     * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_BITMAP]
     * */
    var engraveDataType: Int = DataCmd.ENGRAVE_TYPE_BITMAP_DITHERING,

    /**数据, 纯数据. 不包含文件头. 此数据不入库, 通过文件路径的方式入库*/
    var data: ByteArray? = null,

    //--图片/GCode数据相关属性, px修正过后的

    /**
     * 图片的宽高, 需要使用px分辨率进行调整修正
     * GCode的宽高, 是mm*10后的值
     * [com.angcyo.engrave.data.EngraveOptionInfo.diameterPixel]
     * [com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd.diameter]
     * */
    var width: Int = 0,
    var height: Int = 0,

    /**
     * 图片的xy, 需要使用px分辨率进行调整修正
     * GCode的xy, 是mm*10后的值
     * [com.angcyo.engrave.data.EngraveOptionInfo.diameterPixel]
     * [com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd.diameter]
     * */
    var x: Int = 0,
    var y: Int = 0,

    var px: Byte = LaserPeckerHelper.DEFAULT_PX,

    //---

    /**雕刻数据的索引, 32位, 4个字节
     * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd]*/
    var index: Int = 0, //(System.currentTimeMillis() / 1000).toInt()
    /**雕刻显示的文件名, 36个字节*/
    var name: String? = null,

    //---

    /**
     * GCode数据的总行数, 下位机用来计算进度使用
     * 路径数据的总线段数
     * */
    var lines: Int = -1,
)
