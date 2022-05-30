package com.angcyo.bluetooth.fsc.laserpacker.data

import android.graphics.Path
import android.graphics.RectF

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/30
 */
data class ProductInfo(
    val name: String, //产品名称
    val bounds: RectF, //机器能移动的范围
    val limitPath: Path, //机器在移动范围内的可打印范围
    val isOriginCenter: Boolean, //机器的中心点, 是否在中心, 否则就是在左上角
)
