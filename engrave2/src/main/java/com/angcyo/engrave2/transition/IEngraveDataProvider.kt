package com.angcyo.engrave2.transition

import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.RectF
import com.angcyo.library.annotation.Pixel

/**
 * 雕刻原始数据提供者
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/29
 */
interface IEngraveDataProvider {

    /**获取数据对应的图片, 用来生成雕刻数据
     * 这个图片应该是相对于0,0位置绘制的, 并且包含了旋转/缩放/倾斜等参数
     * */
    fun getBitmapData(): Bitmap? = null

    /**获取数据对应的矢量数据, 用来生成雕刻数据
     * 这个矢量应该是相对于0,0位置绘制的, 并且包含了旋转/缩放/倾斜等参数
     * */
    fun getPathData(): List<Path>? = null

    /**获取转发的原始的数据, 不进行任务处理 */
    fun getRawData(): ByteArray? = null

    //---

    /**获取数据索引, 发给机器的数据索引
     * [com.angcyo.engrave2.EngraveHelper.generateEngraveIndex]*/
    fun getDataIndex(): Int

    /**获取数据在画布中的位置*/
    @Pixel
    fun getDataBounds(): RectF

}