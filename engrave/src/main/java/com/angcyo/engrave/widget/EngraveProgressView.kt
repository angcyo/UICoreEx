package com.angcyo.engrave.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.withClip
import com.angcyo.drawable.base.DslGradientDrawable
import com.angcyo.engrave.R
import com.angcyo.library.ex._color
import com.angcyo.library.ex.dp

/**
 * 雕刻进度
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/27
 */
class EngraveProgressView(context: Context, attributeSet: AttributeSet?) :
    View(context, attributeSet) {

    /**进度条*/
    var progressDrawable: Drawable? = null

    var progressRadius = 24 * dp

    var progressValue: Int = 0

    val _progressBound = Rect()
        get() {
            val right: Int = measuredWidth - paddingRight
            field.set(paddingLeft, paddingTop, right, measuredHeight - paddingBottom)
            return field
        }

    val _progressFraction: Float
        get() = progressValue * 1f / 100

    init {
        setProgressGradientColors()
        if (isInEditMode) {
            progressValue = 50
        }
    }

    //流光进度
    var _progressFlowValue: Int = 0

    /**进度圆角的clip路径*/
    val progressClipPath: Path = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        progressClipPath.rewind()
        progressClipPath.addRoundRect(
            _progressBound.left.toFloat(),
            _progressBound.top.toFloat(),
            _progressBound.right.toFloat(),
            _progressBound.bottom.toFloat(),
            progressRadius,
            progressRadius,
            Path.Direction.CW
        )
        canvas.withClip(progressClipPath) {
            drawColor(_color(R.color.bg_primary_color))
            drawProgress(canvas)
        }
    }

    fun drawProgress(canvas: Canvas) {
        val pBound = _progressBound
        progressDrawable?.apply {

            var progressRight = (pBound.left + pBound.width() * _progressFraction).toInt()

            val right = progressRight

            setBounds(
                pBound.left,
                pBound.top,
                right,
                pBound.bottom
            )

            draw(canvas)

            //流光效果
            val progressFlowRatio = _progressFlowValue / 100f
            setBounds(
                pBound.left,
                pBound.top,
                (right * progressFlowRatio).toInt(),
                pBound.bottom
            )
            draw(canvas)
            if (_progressFlowValue >= 100) {
                _progressFlowValue = 0
            } else {
                _progressFlowValue++
            }
            invalidate()
        }
    }

    /**设置进度条渐变的颜色*/
    fun setProgressGradientColors() {
        DslGradientDrawable().apply {
            gradientColors =
                _fillColor("${_color(R.color.colorPrimary)},${_color(R.color.colorPrimaryDark)}")
            _fillRadii(gradientRadii, progressRadius)
            updateOriginDrawable()
            progressDrawable = originDrawable
        }
    }

}