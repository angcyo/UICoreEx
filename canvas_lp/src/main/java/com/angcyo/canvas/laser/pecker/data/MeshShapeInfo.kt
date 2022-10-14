package com.angcyo.canvas.laser.pecker.data

import com.angcyo.canvas.laser.pecker.R
import com.angcyo.library.ex._string
import com.angcyo.library.extend.IToText
import com.angcyo.library.extend.IToValue

/**
 * "CONE" 圆锥
 * "BALL" 球体
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/14
 */
data class MeshShapeInfo(val shape: String) : IToText, IToValue {

    override fun toText(): CharSequence {
        return if (shape == "CONE") {
            _string(R.string.canvas_cone)
        } else {
            _string(R.string.canvas_ball)
        }
    }

    override fun toValue(): Any = shape
}
