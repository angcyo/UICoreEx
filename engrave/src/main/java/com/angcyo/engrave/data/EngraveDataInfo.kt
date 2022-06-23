package com.angcyo.engrave.data

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper

/**
 * 需要雕刻数据
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/02
 */
data class EngraveDataInfo(
    //数据类型
    var dataType: Int = TYPE_BITMAP,
    //数据
    var data: ByteArray = byteArrayOf(),
    //图片数据相关属性, px修正过后的
    var width: Int = 0,
    var height: Int = 0,
    var x: Int = 0,
    var y: Int = 0,
    var px: Byte = LaserPeckerHelper.DEFAULT_PX,
    /**雕刻数据的文件名, 32位
     * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCommand]*/
    var name: Int = -1, //(System.currentTimeMillis() / 1000).toInt()
    var lines: Int = -1, //GCode数据行数, 下位机用来计算进度使用

    /**开始雕刻的时间, 毫秒*/
    var startEngraveTime: Long = -1,

    /**结束雕刻的时间, 毫秒*/
    var stopEngraveTime: Long = -1,
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
}