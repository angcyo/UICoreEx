package com.angcyo.game.spirit

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.angcyo.game.R
import com.angcyo.game.core.DrawParams
import com.angcyo.game.core.UpdateParams
import com.angcyo.library.ex._color
import com.angcyo.library.ex.toDp
import kotlin.math.max
import kotlin.math.min

/**
 * 波纹效果精灵
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/26
 */
open class WaveSpirit : BaseSpirit() {

    /**波纹颜色*/
    var waveSoldColor: Int = _color(R.color.colorAccent)

    /**波纹描边的颜色*/
    var waveStrokeColor: Int = Color.TRANSPARENT

    /**波纹描边的宽度*/
    var waveStrokeWidth: Float = 2.toDp()

    /**波纹开始的透明度*/
    var waveAlphaStart = 0.8f
        set(value) {
            field = value
            _waveDrawAlpha = value
        }

    /**开始的半径*/
    var waveRadiusStart = 0f
        set(value) {
            field = value
            _waveDrawRadius = value
        }

    var _waveDrawRadius = 0f
    var _waveDrawAlpha = 1f

    override fun draw(canvas: Canvas, drawParams: DrawParams) {
        val rectF = _layer!!.layerRectF
        if (waveSoldColor != 0) {
            spiritPaint.color = waveSoldColor
            spiritPaint.alpha = (_waveDrawAlpha * 255).toInt()
            spiritPaint.style = Paint.Style.FILL
            canvas.drawCircle(rectF.centerX(), rectF.centerY(), _waveDrawRadius, spiritPaint)
        }

        if (waveStrokeColor != 0) {
            spiritPaint.strokeWidth = waveStrokeWidth
            spiritPaint.style = Paint.Style.STROKE
            spiritPaint.color = waveStrokeColor
            spiritPaint.alpha = (_waveDrawAlpha * 255).toInt()
            canvas.drawCircle(rectF.centerX(), rectF.centerY(), _waveDrawRadius, spiritPaint)
        }
    }

    override fun update(updateParams: UpdateParams) {

        val rectF = _layer!!.layerRectF
        val maxRadius = min(rectF.width(), rectF.height()) / 2

        if (_waveDrawRadius > maxRadius) {
            _layer?.removeSpirit(this)
        }

        //半径累加
        _waveDrawRadius = max(waveRadiusStart, _waveDrawRadius) + 1.toDp()

        val ratio = (_waveDrawRadius - waveRadiusStart) / maxRadius
        _waveDrawAlpha = waveAlphaStart * (1 - ratio)
    }
}