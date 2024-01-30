package com.angcyo.canvas2.laser.pecker.util

import com.angcyo.library.component.hawk.HawkPropertyValue
import com.angcyo.library.unit.IRenderUnit
import com.angcyo.library.unit.InchRenderUnit
import com.angcyo.library.unit.MmRenderUnit
import com.angcyo.library.unit.PxRenderUnit
import com.angcyo.library.unit.toPixel

/**
 * 常量
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023-3-6
 */
object LPConstant {

    //region ---Canvas---

    /**像素单位*/
    const val CANVAS_VALUE_UNIT_PIXEL = 1

    /**厘米单位*/
    const val CANVAS_VALUE_UNIT_MM = 2

    /**英寸单位*/
    const val CANVAS_VALUE_UNIT_INCH = 3

    //endregion ---Canvas---

    //region ---Canvas设置项---

    /**单温状态, 持久化*/
    var CANVAS_VALUE_UNIT: Int by HawkPropertyValue<Any, Int>(2)

    /**是否开启智能指南, 持久化*/
    var CANVAS_SMART_ASSISTANT: Boolean by HawkPropertyValue<Any, Boolean>(true)

    /**是否开启网格绘制, 持久化*/
    var CANVAS_DRAW_GRID: Boolean by HawkPropertyValue<Any, Boolean>(true)

    /**渲染的单位*/
    val renderUnit: IRenderUnit
        get() = when (CANVAS_VALUE_UNIT) {
            CANVAS_VALUE_UNIT_PIXEL -> PxRenderUnit()
            CANVAS_VALUE_UNIT_INCH -> InchRenderUnit()
            else -> MmRenderUnit()
        }

    //endregion ---Canvas设置项---
}

/**将mm单位的值, 转换成对应的渲染单位对应的值*/
fun Float.mmToRenderUnitValue(): Float {
    if (LPConstant.renderUnit is MmRenderUnit) {
        return this
    }
    val pixel = toPixel()
    return LPConstant.renderUnit.convertPixelToValue(pixel)
}

/**[mmToRenderUnitValue]*/
fun Float.mmToRenderValueUnit(): String {
    return LPConstant.renderUnit.formatValue(mmToRenderUnitValue(), false, true)
}