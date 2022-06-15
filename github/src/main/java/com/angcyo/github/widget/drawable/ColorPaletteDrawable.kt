package com.angcyo.github.widget.drawable

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.angcyo.drawable.base.AbsDslDrawable
import com.angcyo.library.ex.dpi

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/14
 */
class ColorPaletteDrawable : AbsDslDrawable() {

    var colors = mutableListOf(
        Color.RED,
        Color.MAGENTA,
        Color.BLUE,
        Color.CYAN,
        Color.GREEN,
        Color.YELLOW,
        Color.WHITE,
        Color.BLACK
    )

    init {
        textPaint.style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas) {
        if (colors.isNotEmpty()) {
            val width = bounds.width() / colors.size
            colors.forEachIndexed { index, color ->
                val left = width * index
                textPaint.color = color
                canvas.drawRect(
                    left.toFloat(), bounds.top.toFloat(),
                    (left + width).toFloat(), bounds.bottom.toFloat(),
                    textPaint
                )
            }
        }
    }

    override fun getIntrinsicWidth(): Int {
        return 100 * dpi
    }

    override fun getIntrinsicHeight(): Int {
        return 100 * dpi
    }
}