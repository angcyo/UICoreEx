package com.angcyo.engrave.data

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.objectbox.laser.pecker.entity.EngraveHistoryEntity

/**
 * 需要雕刻数据, 如果是历史文档, 部分数据可能会没有值
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/02
 */
data class EngraveDataInfo(

    //---雕刻相关属性---

    /**数据的类型
     * [com.angcyo.canvas.utils.CanvasConstant.DATA_TYPE_BITMAP]
     * [com.angcyo.canvas.utils.CanvasConstant.DATA_TYPE_SVG]
     * [com.angcyo.canvas.utils.CanvasConstant.DATA_TYPE_GCODE]
     * [com.angcyo.canvas.utils.CanvasConstant.DATA_TYPE_PATH]
     * */
    var dataType: Int = -1,

    /**下位机雕刻的数据类型
     * [com.angcyo.engrave.data.EngraveDataInfo.ENGRAVE_TYPE_BITMAP]
     * [com.angcyo.engrave.data.EngraveDataInfo.ENGRAVE_TYPE_GCODE]
     * [com.angcyo.engrave.data.EngraveDataInfo.ENGRAVE_TYPE_BITMAP_DITHERING]
     * */
    var engraveDataType: Int = ENGRAVE_TYPE_BITMAP,

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
    var index: Int? = null, //(System.currentTimeMillis() / 1000).toInt()
    /**雕刻显示的文件名, 36个字节*/
    var name: String? = null,

    //---

    /**
     * GCode数据的总行数, 下位机用来计算进度使用
     * 路径数据的总线段数
     * */
    var lines: Int = -1,
) {
    companion object {

        /**雕刻图片数据类型.
         *
         * 图片白色像素不打印打印, 色值:255  byte:-1
         * 图片黑色像素打印,      色值:0    byte:0
         * */
        const val ENGRAVE_TYPE_BITMAP = 0x10

        /**GCode数据类型*/
        const val ENGRAVE_TYPE_GCODE = 0x20

        /**路径数据*/
        const val ENGRAVE_TYPE_PATH = 0x30

        /**图片转路径数据格式*/
        const val ENGRAVE_TYPE_BITMAP_PATH = 0x40

        /**图片裁剪数据类型*/
        const val ENGRAVE_TYPE_BITMAP_CROP = 0x50

        /**图片抖动数据类型*/
        const val ENGRAVE_TYPE_BITMAP_DITHERING = 0x60
    }

    /**更新数据到[EngraveHistoryEntity]*/
    fun updateToEntity(entity: EngraveHistoryEntity) {
        entity.dataType = dataType
        entity.engraveDataType = engraveDataType

        entity.width = width
        entity.height = height
        entity.x = x
        entity.y = y

        entity.px = px

        entity.name = name
        entity.index = index
        entity.lines = lines
    }

    /**更新数据从[EngraveHistoryEntity]*/
    fun updateFromEntity(entity: EngraveHistoryEntity): EngraveDataInfo {
        engraveDataType = entity.engraveDataType
        dataType = entity.dataType

        width = entity.width
        height = entity.height
        x = entity.x
        y = entity.y

        px = entity.px

        name = entity.name
        index = entity.index
        lines = entity.lines
        return this
    }
}