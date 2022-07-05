package com.angcyo.engrave.data

import android.graphics.Bitmap
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.data.PxInfo
import com.angcyo.objectbox.laser.pecker.entity.EngraveHistoryEntity

/**
 * 需要雕刻数据, 如果是历史文档, 部分数据可能会没有值
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/02
 */
data class EngraveDataInfo(

    //---雕刻相关属性---

    /**数据类型*/
    var dataType: Int = TYPE_BITMAP,
    //数据, 纯数据. 不包含文件头
    var data: ByteArray? = null,
    //图片数据相关属性, px修正过后的
    var width: Int = 0,
    var height: Int = 0,
    var x: Int = 0,
    var y: Int = 0,
    var px: Byte = LaserPeckerHelper.DEFAULT_PX,
    /**雕刻数据的索引, 32位, 4个字节
     * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCommand]*/
    var index: Int? = null, //(System.currentTimeMillis() / 1000).toInt()
    /**雕刻显示的文件名, 36个字节*/
    var name: String? = null,
    var lines: Int = -1, //GCode数据行数, 下位机用来计算进度使用

    //---预览相关属性---

    /**切换不同分辨率, 模式后的预览图片*/
    var optionBitmap: Bitmap? = null,
    /**操作数据时的原始x,y*/
    var optionX: Int = 0,
    var optionY: Int = 0,

    /**数据处理的模式, 比如是GCode数据, 黑白数据, 灰度数据等
     * [com.angcyo.engrave.canvas.CanvasBitmapHandler.BITMAP_MODE_GREY]
     * [com.angcyo.engrave.canvas.CanvasBitmapHandler.BITMAP_MODE_BLACK_WHITE]
     * [com.angcyo.engrave.canvas.CanvasBitmapHandler.BITMAP_MODE_GCODE]
     * */
    var optionMode: Int? = null,

    /**当前数据格式[dataType]下支持的数据其他处理模式[optionMode]*/
    var optionSupportModeList: List<Int>? = null,

    /**当前数据支持像素调整列表*/
    var optionSupportPxList: List<PxInfo>? = null,

    //---记录相关属性---

    /**是否来自历史的数据, 历史数据部分属性没有值*/
    var isFromHistory: Boolean = false,

    /**[data]数据存储的路径, 方便在历史文档中恢复数据*/
    var dataPath: String? = null,
    /**历史文档预览的图片路径*/
    var previewDataPath: String? = null,

    /**Canvas中对应的[com.angcyo.canvas.items.BaseItem]的uuid*/
    var rendererItemUuid: String? = null,
    /**开始雕刻的时间, 毫秒*/
    var startEngraveTime: Long = -1,
    /**结束雕刻的时间, 毫秒*/
    var stopEngraveTime: Long = -1,
    /**当前数据已经雕刻的次数*/
    var printTimes: Int = 0,
) {
    companion object {

        /**图片数据类型.
         *
         * 图片白色像素不打印打印, 色值:255  byte:-1
         * 图片黑色像素打印,      色值:0    byte:0
         * */
        const val TYPE_BITMAP = 0x10

        /**GCode数据类型*/
        const val TYPE_GCODE = 0x20

        /**路径数据*/
        const val TYPE_PATH = 0x30

        /**图片转路径数据格式*/
        const val TYPE_BITMAP_PATH = 0x40

        /**图片裁剪数据类型*/
        const val TYPE_BITMAP_CROP = 0x50

        /**图片抖动数据类型*/
        const val TYPE_BITMAP_DITHERING = 0x60
    }

    /**更新数据到[EngraveHistoryEntity]*/
    fun updateToEntity(entity: EngraveHistoryEntity) {
        entity.dataType = dataType
        entity.width = width
        entity.height = height
        entity.x = x
        entity.y = y
        entity.px = px

        entity.name = name
        entity.index = index
        entity.lines = lines

        entity.optionMode = optionMode
        entity.dataPath = dataPath
        entity.previewDataPath = previewDataPath

        entity.startEngraveTime = startEngraveTime
        entity.stopEngraveTime = stopEngraveTime
        entity.printTimes = printTimes
    }
}