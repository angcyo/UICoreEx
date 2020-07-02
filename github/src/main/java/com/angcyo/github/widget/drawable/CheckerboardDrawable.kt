package com.angcyo.github.widget.drawable

import android.graphics.*
import android.graphics.drawable.Drawable

/**
 * https://github.com/duanhong169/CheckerboardDrawable
 *
 * https://github.com/angcyo/RRes
 *
 * 透明背景显示的Drawable, 棋盘背景
 * 2020-7-2
 * */
class CheckerboardDrawable(val builder: Builder) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val size: Int
    private val colorOdd: Int
    private val colorEven: Int

    companion object {
        fun create(): CheckerboardDrawable {
            return CheckerboardDrawable(Builder())
        }
    }

    init {
        size = builder.size
        colorOdd = builder.colorOdd
        colorEven = builder.colorEven
        configurePaint()
    }

    private fun configurePaint() {
        val bitmap = Bitmap.createBitmap(size * 2, size * 2, Bitmap.Config.ARGB_8888)
        val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        bitmapPaint.style = Paint.Style.FILL
        val canvas = Canvas(bitmap)
        val rect = Rect(0, 0, size, size)
        bitmapPaint.color = colorOdd
        canvas.drawRect(rect, bitmapPaint)
        rect.offset(size, size)
        canvas.drawRect(rect, bitmapPaint)
        bitmapPaint.color = colorEven
        rect.offset(-size, 0)
        canvas.drawRect(rect, bitmapPaint)
        rect.offset(size, -size)
        canvas.drawRect(rect, bitmapPaint)
        paint.shader = BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
    }

    override fun draw(canvas: Canvas) {
        canvas.drawPaint(paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int {
        return PixelFormat.OPAQUE
    }

    class Builder {

        var size = 40
        var colorOdd = -0x3d3d3e
        var colorEven = -0xc0c0d

        fun build(): CheckerboardDrawable {
            return CheckerboardDrawable(this)
        }
    }
}