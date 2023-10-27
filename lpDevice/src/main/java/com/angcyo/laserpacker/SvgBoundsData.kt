package com.angcyo.laserpacker

import android.graphics.Matrix
import android.graphics.RectF
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.library.unit.PointValueUnit
import com.angcyo.library.unit.toPixel
import com.angcyo.library.utils.getFloatNum

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/10/27
 */
data class SvgBoundsData(
    var svgRect: RectF? = null,
    var viewBoxStr: String? = null,
    var widthStr: String? = null,
    var heightStr: String? = null,
) {

    /**默认情况下的svg应该缩放的矩阵*/
    fun defaultSvgScaleMatrix(): Matrix? {
        if (HawkEngraveKeys.enableImportSvgScale) {
            return Matrix().apply {
                val scale = svgScale
                setScale(scale, scale)
            }
        }
        return null
    }

    fun getBoundsScaleMatrix(): Matrix? {
        if (widthStr.isNullOrBlank() || heightStr.isNullOrBlank()) {
            //未指定
            return defaultSvgScaleMatrix()
        } else {
            val svgBounds = svgRect ?: return null
            var targetWidth: Float? = null
            var targetHeight: Float? = null
            if (!widthStr.isNullOrBlank()) {
                val widthValue = widthStr.getFloatNum() ?: 0f
                val str = widthStr!!.lowercase()
                if (str.endsWith("%")) {
                    targetWidth = svgBounds.width() * widthValue / 100
                } else if (str.endsWith("pt")) {
                    targetWidth = widthValue.toPixel(PointValueUnit())
                } else if (str.endsWith("mm")) {
                    targetWidth = widthValue.toPixel()
                } else {
                    targetWidth = widthValue
                }
            }
            if (!heightStr.isNullOrBlank()) {
                val heightValue = heightStr.getFloatNum() ?: 0f
                val str = heightStr!!.lowercase()
                if (str.endsWith("%")) {
                    targetHeight = svgBounds.height() * heightValue / 100
                } else if (str.endsWith("pt")) {
                    targetHeight = heightValue.toPixel(PointValueUnit())
                } else if (str.endsWith("mm")) {
                    targetHeight = heightValue.toPixel()
                } else {
                    targetHeight = heightValue
                }
            }
            if (targetWidth == null || targetHeight == null) {
                return null
            }
            val width = svgBounds.width()
            val height = svgBounds.height()
            if (width > 0 && height > 0) {
                val sx = targetWidth / width
                val sy = targetHeight / height
                val matrix = Matrix()
                matrix.setScale(sx, sy)
                return matrix
            }
        }
        return null
    }

}
