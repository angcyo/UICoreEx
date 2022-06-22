package com.angcyo.engrave

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.angcyo.core.component.file.writeTo
import com.angcyo.library.ex.toElapsedTime
import com.angcyo.library.ex.toHexInt
import com.angcyo.library.utils.Constant
import com.angcyo.library.utils.FileTextData

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/20
 */

/**机器雕刻的色彩数据可视化*/
fun ByteArray.toEngraveBitmap(width: Int, height: Int): Bitmap {
    val channelBitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(channelBitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.style = Paint.Style.FILL
        strokeWidth = 1f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    val bytes = this
    for (y in 0 until height) {
        for (x in 0 until width) {
            val value: Int = bytes[y * width + x].toHexInt()
            paint.color = Color.argb(255, value, value, value)
            canvas.drawCircle(x.toFloat(), y.toFloat(), 1f, paint)//绘制圆点
        }
    }
    return channelBitmap
}

/**分:秒 的时间格式*/
fun Long?.toEngraveTime() = this?.toElapsedTime(
    pattern = intArrayOf(-1, 1, 1),
    units = arrayOf("", "", ":", ":", ":")
)